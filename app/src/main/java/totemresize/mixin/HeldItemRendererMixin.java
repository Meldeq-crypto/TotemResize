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
 * Scales the held Totem of Undying in first-person view using the
 * <b>Held Totem</b> scale value from config.
 *
 * <p>Uses priority <b>10001</b> so this mixin applies <em>after</em> virtually
 * all other mods and texture packs (default priority is 1000), preventing them
 * from overriding our scale transform.  The HEAD injection point is chosen for
 * maximum cross-version compatibility across 1.21.1 – 1.21.x.
 *
 * <p>The scaling is performed around the model centre (translate → scale →
 * translate back) so the totem stays centred even at extreme sizes.
 * A scale of 0.0 effectively makes the item invisible.
 */
@Mixin(value = HeldItemRenderer.class, priority = 10001)
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

        float scale = TotemResizeConfig.getHeldScale();
        if (Float.compare(scale, 1.0f) == 0) {
            return; // default size – skip entirely for zero overhead
        }

        if (Float.compare(scale, 0.0f) == 0) {
            // Not visible: scale to zero so nothing renders
            matrices.scale(0.0f, 0.0f, 0.0f);
            return;
        }

        // Centre-anchored scaling: move origin to centre of the held-item
        // model, scale, then move back.  The caller already wraps this
        // method in its own push/pop, so in-place transforms are safe.
        matrices.translate(0.5, 0.5, 0.5);
        matrices.scale(scale, scale, scale);
        matrices.translate(-0.5, -0.5, -0.5);
    }
}
