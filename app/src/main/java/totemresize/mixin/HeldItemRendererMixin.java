package totemresize.mixin;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
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
 * <h2>Why this mixin is unbreakable</h2>
 * <ul>
 *   <li><b>Priority 999999</b> – Runs after every other mod and resource pack,
 *       guaranteeing our scale is the final transform applied.</li>
 *   <li><b>PoseStack scaling</b> – We scale the PoseStack directly at
 *       {@code HEAD} of {@code renderFirstPersonItem}, immediately before
 *       any rendering occurs.  The caller already wraps this in its own
 *       push/pop, so the transform is inherently isolated.</li>
 *   <li><b>0.0× = GPU no-draw</b> – For "Invisible", we scale to exactly
 *       zero so the GPU discards all geometry.</li>
 *   <li><b>Centre-anchored</b> – Translate to model centre → scale →
 *       translate back, so the totem stays centred at any size.</li>
 * </ul>
 */
@Mixin(value = HeldItemRenderer.class, priority = 999999)
public class HeldItemRendererMixin {

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

        // Vanilla size → skip entirely for zero overhead.
        if (Float.compare(scale, 1.0f) == 0) {
            return;
        }

        // Invisible → scale to absolute zero so the GPU draws nothing.
        if (Float.compare(scale, 0.0f) == 0) {
            matrices.scale(0.0f, 0.0f, 0.0f);
            return;
        }

        // Centre-anchored scaling: move origin to centre of the held-item
        // model space, scale, then move back.  The caller already wraps
        // this method in its own push/pop, so in-place transforms are safe.
        matrices.translate(0.5, 0.5, 0.5);
        matrices.scale(scale, scale, scale);
        matrices.translate(-0.5, -0.5, -0.5);
    }
}
