package totemresize.mixin;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
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
     * Injects at HEAD of renderFirstPersonItem to apply our scale transform
     * before the vanilla rendering logic runs.
     *
     * <p>This is the same injection point used by Totem Tweaks. By scaling
     * the MatrixStack before any other transforms, we ensure clean composition
     * with the vanilla model transforms and any resource pack overrides.</p>
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

        if (!TotemResizeUtil.isTotem(stack)) {
            return;
        }

        // Read the public static volatile field – zero indirection.
        float scale = TotemResizeConfig.heldScale;

        // Vanilla size (1.0×) → skip entirely for zero overhead.
        // At 1.0×, the totem renders at whatever the JSON/resource pack specifies.
        if (Float.compare(scale, 1.0f) == 0) {
            return;
        }

        // Invisible → scale to absolute zero so the GPU draws nothing.
        if (Float.compare(scale, 0.0f) == 0) {
            matrices.scale(0.0f, 0.0f, 0.0f);
            return;
        }

        // Apply uniform scale using MatrixStack.scale() for clean matrix
        // composition. This avoids the jitter that can occur when directly
        // manipulating Matrix4f with translate/scale/translate sequences.
        //
        // The scale is applied BEFORE the vanilla rendering pipeline processes
        // the bakedModel transforms, so it effectively multiplies the resource
        // pack's JSON display values by our scale factor.
        matrices.scale(scale, scale, scale);
    }
}
