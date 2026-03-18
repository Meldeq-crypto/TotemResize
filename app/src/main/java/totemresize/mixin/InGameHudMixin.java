package totemresize.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import totemresize.config.TotemResizeConfig;

/**
 * Scales the Totem of Undying "pop" overlay animation that plays on the HUD
 * when the player is saved from death.
 *
 * <p>Targets {@link InGameHud} (Yarn) / {@code net.minecraft.client.gui.Gui} (MojMap).
 * Wraps the overlay rendering in a {@code poseStack.push()} / {@code poseStack.pop()}
 * and applies centre-anchored scaling so the animation stays perfectly centred
 * on screen even at extreme sizes.
 *
 * <p>Uses priority <b>10001</b> so this mixin applies after virtually all
 * other mods and texture packs, preventing them from overriding the scale.
 *
 * <p>Multiple injection targets are declared with {@code require = 0} for
 * cross-version compatibility across 1.21.1 – 1.21.x builds.
 */
@Mixin(value = InGameHud.class, priority = 10001)
public abstract class InGameHudMixin {

    /**
     * Tracks whether we pushed the matrix stack so we can safely pop only
     * if we actually pushed.
     */
    @Unique
    private boolean totemResize$pushed = false;

    // ── Target 1: renderMiscOverlays(DrawContext, RenderTickCounter) ──
    // Some 1.21.x Yarn builds place the totem overlay inside this method.

    @Inject(method = "renderMiscOverlays", at = @At("HEAD"), require = 0)
    private void totemResize$beforeMiscOverlays(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        applyPopScale(context);
    }

    @Inject(method = "renderMiscOverlays", at = @At("RETURN"), require = 0)
    private void totemResize$afterMiscOverlays(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        removePopScale(context);
    }

    // ── Target 2: renderMiscOverlays(DrawContext, float) ──
    // Older 1.21.x builds may use a float tickDelta instead of RenderTickCounter.

    @Inject(method = "renderMiscOverlays(Lnet/minecraft/client/gui/DrawContext;F)V", at = @At("HEAD"), require = 0)
    private void totemResize$beforeMiscOverlaysLegacy(DrawContext context, float tickDelta, CallbackInfo ci) {
        applyPopScale(context);
    }

    @Inject(method = "renderMiscOverlays(Lnet/minecraft/client/gui/DrawContext;F)V", at = @At("RETURN"), require = 0)
    private void totemResize$afterMiscOverlaysLegacy(DrawContext context, float tickDelta, CallbackInfo ci) {
        removePopScale(context);
    }

    // ── Shared logic ──

    @Unique
    private void applyPopScale(DrawContext context) {
        float scale = TotemResizeConfig.getPopScale();
        if (Float.compare(scale, 1.0f) == 0) {
            totemResize$pushed = false;
            return;
        }

        MatrixStack matrices = context.getMatrices();
        matrices.push();
        totemResize$pushed = true;

        if (Float.compare(scale, 0.0f) == 0) {
            // Not visible: scale to zero so nothing renders
            matrices.scale(0.0f, 0.0f, 0.0f);
            return;
        }

        // Centre-anchored scaling: translate to screen centre, scale,
        // translate back so the overlay stays perfectly centred.
        MinecraftClient client = MinecraftClient.getInstance();
        float centreX = client.getWindow().getScaledWidth() / 2.0f;
        float centreY = client.getWindow().getScaledHeight() / 2.0f;
        matrices.translate(centreX, centreY, 0);
        matrices.scale(scale, scale, 1.0f);
        matrices.translate(-centreX, -centreY, 0);
    }

    @Unique
    private void removePopScale(DrawContext context) {
        if (totemResize$pushed) {
            context.getMatrices().pop();
            totemResize$pushed = false;
        }
    }
}
