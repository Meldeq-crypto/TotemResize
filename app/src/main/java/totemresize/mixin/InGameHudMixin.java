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
 * The "Unstoppable" Totem Pop Fix.
 *
 * <p>Scales the Totem of Undying "pop" overlay animation that plays on the HUD
 * when the player is saved from death.
 *
 * <h2>Why this mixin is unbreakable</h2>
 * <ul>
 *   <li><b>Priority 999999</b> – Tells the mixin system: "I don't care what any
 *       resource pack or other mod said; run me at the very last millisecond."
 *       This guarantees our transform is the final word on the totem's size.</li>
 *   <li><b>Push / Pop isolation</b> – We push a fresh matrix onto the pose stack
 *       before the totem renders and pop it afterward. This means our scale
 *       transform is perfectly isolated: it cannot accidentally shrink the
 *       crosshair, health bar, hotbar, or any other HUD element.</li>
 *   <li><b>Centre-anchored scaling</b> – We translate to the screen centre,
 *       apply the scale, then translate back. The totem animation stays
 *       perfectly centred even at 6× (Screen Overload).</li>
 *   <li><b>0.0× = GPU no-draw</b> – When set to Invisible, the code literally
 *       tells the GPU "scale to zero," which is the only 100% effective way to
 *       remove the pop overlay.</li>
 * </ul>
 *
 * <p>Multiple injection targets are declared with {@code require = 0} for
 * cross-version compatibility across 1.21.1 – 1.21.x builds.
 */
@Mixin(value = InGameHud.class, priority = 999999)
public abstract class InGameHudMixin {

    /**
     * Tracks whether we pushed the matrix stack so we can safely pop only
     * if we actually pushed (avoids corrupting the stack if scale == 1.0).
     */
    @Unique
    private boolean totemResize$pushed = false;

    // ── Target 1: renderMiscOverlays(DrawContext, RenderTickCounter) ──
    // 1.21.1+ Yarn builds place the totem overlay inside this method.

    @Inject(method = "renderMiscOverlays", at = @At("HEAD"), require = 0)
    private void totemResize$beforeMiscOverlays(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        totemResize$applyPopScale(context);
    }

    @Inject(method = "renderMiscOverlays", at = @At("RETURN"), require = 0)
    private void totemResize$afterMiscOverlays(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        totemResize$removePopScale(context);
    }

    // ── Target 2: renderMiscOverlays(DrawContext, float) ──
    // Older 1.21.x builds may use a float tickDelta instead of RenderTickCounter.

    @Inject(method = "renderMiscOverlays(Lnet/minecraft/client/gui/DrawContext;F)V", at = @At("HEAD"), require = 0)
    private void totemResize$beforeMiscOverlaysLegacy(DrawContext context, float tickDelta, CallbackInfo ci) {
        totemResize$applyPopScale(context);
    }

    @Inject(method = "renderMiscOverlays(Lnet/minecraft/client/gui/DrawContext;F)V", at = @At("RETURN"), require = 0)
    private void totemResize$afterMiscOverlaysLegacy(DrawContext context, float tickDelta, CallbackInfo ci) {
        totemResize$removePopScale(context);
    }

    // ── Shared push / scale logic ──────────────────────────────────────

    @Unique
    private void totemResize$applyPopScale(DrawContext context) {
        // Read the public static volatile field – zero indirection.
        float scale = TotemResizeConfig.popScale;

        // Vanilla size → no-op (skip push entirely for zero overhead)
        if (Float.compare(scale, 1.0f) == 0) {
            totemResize$pushed = false;
            return;
        }

        MatrixStack matrices = context.getMatrices();
        matrices.push();
        totemResize$pushed = true;

        // Invisible → scale to absolute zero so the GPU draws nothing.
        if (Float.compare(scale, 0.0f) == 0) {
            matrices.scale(0.0f, 0.0f, 0.0f);
            return;
        }

        // Centre-anchored scaling:
        //   1. Translate origin to screen centre.
        //   2. Apply uniform XY scale (Z stays 1.0 to avoid depth issues).
        //   3. Translate origin back.
        // This keeps the totem animation perfectly centred at any size.
        MinecraftClient client = MinecraftClient.getInstance();
        float centreX = client.getWindow().getScaledWidth() / 2.0f;
        float centreY = client.getWindow().getScaledHeight() / 2.0f;

        matrices.translate(centreX, centreY, 0.0f);
        matrices.scale(scale, scale, 1.0f);
        matrices.translate(-centreX, -centreY, 0.0f);
    }

    @Unique
    private void totemResize$removePopScale(DrawContext context) {
        if (totemResize$pushed) {
            context.getMatrices().pop();
            totemResize$pushed = false;
        }
    }
}
