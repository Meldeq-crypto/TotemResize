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
 * <p>Uses priority 1001 so this mixin applies <em>after</em> most cosmetic
 * mods (default priority is 1000), preventing them from overriding the scale.
 *
 * <p>The scaling is performed around the model centre (translate → scale →
 * translate back) so the totem stays centred even at extreme sizes.
 */
@Mixin(value = HeldItemRenderer.class, priority = 1001)
public class HeldItemRendererMixin {

    @Inject(
        method = "renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
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

        float scale = TotemResizeConfig.get().getRenderScale();
        if (Float.compare(scale, 1.0f) == 0) {
            return; // default size – skip entirely for zero overhead
        }

        // Centre-anchored scaling: move origin to centre of the held-item
        // model, scale, then move back.  The caller already wraps this
        // method in its own push/pop, so in-place transforms are safe.
        matrices.translate(0.5, 0.5, 0.5);
        matrices.scale(scale, scale, scale);
        matrices.translate(-0.5, -0.5, -0.5);
    }
}
