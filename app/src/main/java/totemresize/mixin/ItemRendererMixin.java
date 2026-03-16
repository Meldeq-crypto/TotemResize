package totemresize.mixin;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import totemresize.config.TotemResizeConfig;
import totemresize.util.TotemResizeUtil;

/**
 * Scales the Totem of Undying "pop" sprite that plays as an overlay when
 * the player is saved from death.
 *
 * <p>This catches any totem rendered in GUI / FIXED mode (which includes the
 * pop overlay and similar UI contexts) and applies centre-anchored scaling so
 * the totem doesn't fly off the edge of the screen at large sizes.
 *
 * <p>Two injection targets are declared with {@code require = 0} to cover
 * both the 7-arg and 8-arg overloads across different 1.21.x builds.
 * Only one needs to match for the mod to work.
 */
@Mixin(value = ItemRenderer.class, priority = 1001)
public abstract class ItemRendererMixin {

    /* ── 8-arg overload (older 1.21 builds) ── */
    @Inject(
        method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V",
        at = @At("HEAD"),
        require = 0
    )
    private void totemResize$scaleTotemPopLegacy(
            ItemStack stack, ModelTransformationMode displayContext, boolean leftHanded,
            MatrixStack matrices, VertexConsumerProvider vertices,
            int light, int overlay, BakedModel model, CallbackInfo info) {
        applyGuiScale(stack, displayContext, matrices);
    }

    /* ── 6-arg overload (newer 1.21 builds) ── */
    @Inject(
        method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;II)V",
        at = @At("HEAD"),
        require = 0
    )
    private void totemResize$scaleTotemPop(
            ItemStack stack, ModelTransformationMode displayContext,
            MatrixStack matrices, VertexConsumerProvider vertices,
            int light, int overlay, CallbackInfo info) {
        applyGuiScale(stack, displayContext, matrices);
    }

    // ── shared logic ──

    /**
     * Apply centre-anchored scaling when a totem is rendered in GUI / FIXED
     * mode (the totem pop overlay uses one of these).
     */
    private static void applyGuiScale(ItemStack stack, ModelTransformationMode ctx, MatrixStack matrices) {
        if (ctx != ModelTransformationMode.GUI && ctx != ModelTransformationMode.FIXED) {
            return;
        }
        if (!TotemResizeUtil.isTotem(stack)) {
            return;
        }

        float scale = TotemResizeConfig.get().getRenderScale();
        if (Float.compare(scale, 1.0f) == 0) {
            return; // default – no scaling needed
        }

        // Centre-anchored: translate origin to model centre, scale, translate back.
        // The item model in GUI mode is typically 1×1×1 centred at (0.5, 0.5, 0.5).
        matrices.translate(0.5, 0.5, 0.5);
        matrices.scale(scale, scale, scale);
        matrices.translate(-0.5, -0.5, -0.5);
    }
}
