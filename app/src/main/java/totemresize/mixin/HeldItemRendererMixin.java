package totemresize.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import totemresize.config.TotemResizeConfig;
import totemresize.util.TotemResizeUtil;

/**
 * Scales the held Totem of Undying in first-person view.
 *
 * <h2>Approach (inspired by Totem Tweaks)</h2>
 * <p>Totem Tweaks injects at HEAD of {@code renderFirstPersonItem} and applies
 * {@code matrices.scale(size, size, size)} when the item is a totem. We follow
 * the same pattern for maximum compatibility.
 *
 * <h2>Priority</h2>
 * <p>Set to {@code Integer.MAX_VALUE} to guarantee this mixin runs last,
 * after every other mod and resource pack has applied their transforms.
 *
 * <h2>Resource Pack Synergy</h2>
 * <p>The scale multiplier is applied <b>after</b> the bakedModel transforms
 * are applied by the rendering pipeline. This means:
 * <ul>
 *   <li>If a resource pack has its own totem_of_undying.json with custom
 *       display values (e.g., different scale, translation, rotation), those
 *       values are applied first by Minecraft's model system.</li>
 *   <li>Our mixin then <b>multiplies</b> the result by the user's custom
 *       scale, preserving 3D models and custom offsets from packs.</li>
 *   <li>The JSON baseline (scale: [0.6, 0.6, 0.6]) corresponds to slider
 *       value 5 (1.0× multiplier), so at default the totem renders exactly
 *       as the resource pack intended.</li>
 * </ul>
 *
 * <h2>Synchronization with Pop</h2>
 * <p>Both this mixin and {@link GameRendererMixin} read from the same unified
 * {@code TotemResizeConfig.heldScale} / {@code popScale} volatile fields, which
 * are always identical. This ensures the size in hand and the pop animation
 * are perfectly synchronized.</p>
 *
 * <h2>Anti-Jitter</h2>
 * <p>We use {@code MatrixStack.scale()} (the high-level API) instead of
 * directly manipulating the {@code Matrix4f}. This avoids floating-point
 * precision issues that can cause jitter or flicker when resizing in the
 * config menu.</p>
 */
@Mixin(value = HeldItemRenderer.class, priority = Integer.MAX_VALUE)
public class HeldItemRendererMixin {

    /**
     * Whether we disabled face culling for the current frame and need to
     * restore it in the TAIL injection.
     */
    @Unique
    private boolean totemResize$didDisableCull = false;

    /**
     * Resolves which {@link Arm} is used for a given {@link Hand}, accounting
     * for the player's main-arm preference (left-handed vs right-handed).
     */
    @Unique
    private Arm totemResize$getArmForHand(AbstractClientPlayerEntity player, Hand hand) {
        Arm mainArm = player.getMainArm();
        if (hand == Hand.MAIN_HAND) {
            return mainArm;
        }
        // Offhand is the opposite arm.
        return mainArm == Arm.RIGHT ? Arm.LEFT : Arm.RIGHT;
    }

    /**
     * Injects at HEAD of renderFirstPersonItem to apply our scale transform
     * before the vanilla rendering logic runs.
     *
     * <h3>Left-hand (offhand) fix</h3>
     * <p>Vanilla mirrors the left-hand model by applying a {@code -1} multiplier
     * on the X axis for translations and rotations. When we apply a uniform
     * scale at HEAD, the mirrored translations get magnified, pushing the model
     * off-center. Additionally, the combination of our positive scale with
     * vanilla's negative-X mirror produces a transformation matrix whose
     * determinant's sign can cause back-face culling to hide the wrong faces,
     * making one side of the totem appear invisible.</p>
     *
     * <p>We fix this by:
     * <ol>
     *   <li>For <b>both hands</b>: applying the scale uniformly.</li>
     *   <li>For the <b>left hand (offhand)</b>: temporarily disabling back-face
     *       culling so the mirrored+scaled geometry renders all faces. Culling
     *       is re-enabled in the TAIL injection.</li>
     *   <li>For the <b>left hand</b>: applying a small X-axis translation
     *       correction proportional to {@code (scale - 1)} to compensate for
     *       the shifted pivot caused by the mirrored transforms being scaled.</li>
     * </ol>
     */
    @Inject(
            method = "renderFirstPersonItem",
            at = @At("HEAD")
    )
    private void totemResize$scaleHeldTotem(
            AbstractClientPlayerEntity player, float tickDelta, float pitch,
            Hand hand, float swingProgress, ItemStack stack, float equipProgress,
            MatrixStack matrices, VertexConsumerProvider vertices, int light,
            CallbackInfo info) {

        totemResize$didDisableCull = false;

        if (!TotemResizeUtil.isTotem(stack)) {
            return;
        }

        // Read the public static volatile field – zero indirection.
        float scale = TotemResizeConfig.heldScale;

        // Vanilla size (1.0×) → skip entirely for zero overhead.
        if (Float.compare(scale, 1.0f) == 0) {
            return;
        }

        // Invisible → scale to absolute zero so the GPU draws nothing.
        if (Float.compare(scale, 0.0f) == 0) {
            matrices.scale(0.0f, 0.0f, 0.0f);
            return;
        }

        Arm arm = totemResize$getArmForHand(player, hand);
        boolean isLeftArm = (arm == Arm.LEFT);

        if (isLeftArm) {
            // ── Left-hand / offhand fix ────────────────────────────────
            //
            // Vanilla mirrors the left hand with a -1 X multiplier applied
            // to translations and rotations inside renderFirstPersonItem.
            // When we pre-scale uniformly, those mirrored offsets are
            // magnified, causing the model to shift sideways. We compensate
            // by applying a counter-translation on X proportional to how
            // much the scale deviates from 1.0.
            //
            // The constant 0.28 was tuned to keep the offhand model in its
            // natural screen position across the full slider range (0×–6×).
            float drift = (scale - 1.0f) * 0.28f;
            matrices.translate(drift, 0.0f, 0.0f);

            // Disable back-face culling. The combination of our positive
            // uniform scale with vanilla's negative-X mirror produces a
            // negative determinant, which reverses the face winding order.
            // Without this, one side of the totem is invisible.
            RenderSystem.disableCull();
            totemResize$didDisableCull = true;
        }

        // Apply uniform scale. For the right hand this is all that's needed.
        // For the left hand, the translate + cull-disable above fix the
        // mirroring artefacts.
        matrices.scale(scale, scale, scale);
    }

    /**
     * Re-enables face culling after the left-hand totem has finished rendering,
     * so other items/geometry are not affected.
     */
    @Inject(
            method = "renderFirstPersonItem",
            at = @At("TAIL")
    )
    private void totemResize$restoreCull(
            AbstractClientPlayerEntity player, float tickDelta, float pitch,
            Hand hand, float swingProgress, ItemStack stack, float equipProgress,
            MatrixStack matrices, VertexConsumerProvider vertices, int light,
            CallbackInfo info) {

        if (totemResize$didDisableCull) {
            RenderSystem.enableCull();
            totemResize$didDisableCull = false;
        }
    }
}
