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
 * <h2>Features</h2>
 * <ul>
 *   <li><b>Scale:</b> Uniform scale from config slider.</li>
 *   <li><b>X/Y Offset:</b> Translates the totem position in first-person.</li>
 *   <li><b>Static Mode:</b> When enabled, counteracts vanilla bobbing by
 *       zeroing out the swing and equip progress parameters.</li>
 * </ul>
 *
 * <h2>Priority</h2>
 * <p>Set to {@code Integer.MAX_VALUE} to guarantee this mixin runs last,
 * after every other mod and resource pack has applied their transforms.
 *
 * <h2>Resource Pack Synergy</h2>
 * <p>The scale multiplier is applied <b>after</b> the bakedModel transforms
 * are applied by the rendering pipeline. This means resource packs' custom
 * display values are preserved but resized.</p>
 *
 * <h2>Anti-Jitter</h2>
 * <p>We use {@code MatrixStack.scale()} (the high-level API) instead of
 * directly manipulating the {@code Matrix4f}.</p>
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
        return mainArm == Arm.RIGHT ? Arm.LEFT : Arm.RIGHT;
    }

    /**
     * Injects at HEAD of renderFirstPersonItem to apply our scale, offset,
     * and static-mode transforms before the vanilla rendering logic runs.
     *
     * <h3>Static Mode</h3>
     * <p>When static mode is enabled, we counteract the vanilla bobbing
     * animation by applying a counter-translation that zeros out the
     * swing-progress and equip-progress effects. The vanilla method uses
     * these to create the bobbing/swinging motion, so we translate the
     * matrix stack back to a neutral position.</p>
     *
     * <h3>Left-hand (offhand) fix</h3>
     * <p>Vanilla mirrors the left-hand model by applying a {@code -1} multiplier
     * on the X axis. When we apply a uniform scale at HEAD, the mirrored
     * translations get magnified. We compensate with a counter-translation
     * and disable back-face culling.</p>
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

        // Read the public static volatile fields – zero indirection.
        float scale = TotemResizeConfig.heldScale;
        float xOff = TotemResizeConfig.xOffset;
        float yOff = TotemResizeConfig.yOffset;

        // Apply X/Y offset (always, even at default scale)
        if (xOff != 0.0f || yOff != 0.0f) {
            matrices.translate(xOff, yOff, 0.0f);
        }

        // Vanilla size (1.0×) → skip scale entirely for zero overhead.
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
            float drift = (scale - 1.0f) * 0.28f;
            matrices.translate(drift, 0.0f, 0.0f);

            RenderSystem.disableCull();
            totemResize$didDisableCull = true;
        }

        // Apply uniform scale.
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

    /**
     * Static Mode: Intercepts the bobEquip call to neutralize bobbing.
     *
     * <p>Vanilla calls {@code HeldItemRenderer.renderFirstPersonItem}
     * which internally uses {@code swingProgress} and {@code equipProgress}
     * to produce the bobbing/swinging animation. When static mode is on,
     * we cancel the bob by resetting the equip offset.
     *
     * <p>This injection targets the {@code applyEquipOffset} method which
     * vanilla uses to translate the item based on equip progress. We zero
     * out the translation when the totem should be static.</p>
     */
    @Inject(
            method = "applyEquipOffset",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void totemResize$cancelEquipBob(MatrixStack matrices, Arm arm, float equipProgress, CallbackInfo info) {
        if (TotemResizeConfig.staticTotem) {
            // Cancel the equip offset animation entirely for a static look.
            // We don't cancel it — we just apply zero offset.
            info.cancel();
        }
    }

    /**
     * Static Mode: Intercepts the swing animation to neutralize hand swing.
     *
     * <p>When static totem is enabled, this cancels the swing animation
     * that causes the hand to move when attacking or using items.</p>
     */
    @Inject(
            method = "applySwingOffset",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void totemResize$cancelSwingBob(MatrixStack matrices, Arm arm, float swingProgress, CallbackInfo info) {
        if (TotemResizeConfig.staticTotem) {
            info.cancel();
        }
    }
}
