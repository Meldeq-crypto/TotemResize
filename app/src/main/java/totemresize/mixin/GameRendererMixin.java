package totemresize.mixin;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import totemresize.config.TotemResizeConfig;
import totemresize.util.TotemResizeUtil;

/**
 * Scales the Totem of Undying "pop" overlay animation.
 *
 * <h2>Approach (inspired by Totem Tweaks)</h2>
 * <p>The totem pop animation is rendered inside
 * {@code GameRenderer.renderFloatingItem(DrawContext, float)}. This method
 * creates its own {@link MatrixStack}, computes a bouncing animation scale
 * value {@code s = 50.0f + 175.0f * sin(θ)}, then calls
 * {@code matrices.scale(s, -s, s)}.
 *
 * <p>We intercept three key points:
 * <ol>
 *   <li>{@code showFloatingItem(ItemStack)} – to track when a pop is triggered
 *       and verify it's actually a Totem of Undying.</li>
 *   <li>The {@code MatrixStack.scale(FFF)} call inside {@code renderFloatingItem}
 *       – to multiply all three scale components by the user's chosen visual
 *       scale factor (derived from the 0.6 JSON baseline).</li>
 *   <li>The {@code MatrixStack.translate(FFF)} call inside {@code renderFloatingItem}
 *       – to adjust screen position proportionally when scaling.</li>
 * </ol>
 *
 * <h2>JSON Baseline Integration</h2>
 * <p>The 0.6 scale factor from the provided totem_of_undying.json is the
 * mathematical baseline. Slider value 5 → multiplier 1.0× means the pop
 * animation renders at vanilla size. The user's scale multiplier is applied
 * on top of both vanilla and any resource pack transforms.</p>
 *
 * <h2>Priority</h2>
 * <p>Set to {@code Integer.MAX_VALUE} to guarantee this mixin runs last,
 * overriding any resource packs or other mods that touch the pop animation.</p>
 */
@Mixin(value = GameRenderer.class, priority = Integer.MAX_VALUE)
public abstract class GameRendererMixin {

    @Shadow
    private ItemStack floatingItem;

    @Shadow
    private int floatingItemTimeLeft;

    /** Tracks whether the current floating item is a totem. */
    @Unique
    private boolean totemResize$isTotemPop = false;

    /**
     * Intercepts {@code showFloatingItem} to track when a totem pop is triggered.
     * This mirrors Totem Tweaks' approach of capturing the item at trigger time.
     */
    @Inject(
            method = "showFloatingItem",
            at = @At("HEAD")
    )
    private void totemResize$onShowFloatingItem(ItemStack stack, CallbackInfo ci) {
        totemResize$isTotemPop = TotemResizeUtil.isTotem(stack);
    }

    /**
     * Intercepts the {@code MatrixStack.scale(FFF)} call inside
     * {@code renderFloatingItem} and multiplies all three components by the
     * user's chosen pop scale.
     *
     * <p>This uses {@code @ModifyArgs} for robust interception — the same
     * pattern used by Totem Tweaks. The original call is
     * {@code matrices.scale(s, -s, s)} where {@code s} is the computed
     * bounce animation value. We multiply each axis by our scale factor,
     * preserving the Y-flip (negative sign).</p>
     *
     * @param args the three float arguments to MatrixStack.scale()
     */
    @ModifyArgs(
            method = "renderFloatingItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/util/math/MatrixStack;scale(FFF)V"
            ),
            require = 0
    )
    private void totemResize$modifyPopScale(Args args) {
        float scale = TotemResizeConfig.popScale;

        // 1.0× → pass through unchanged (zero overhead at default).
        if (Float.compare(scale, 1.0f) == 0) {
            return;
        }

        float x = args.get(0);
        float y = args.get(1);
        float z = args.get(2);

        // 0.0× → scale to zero: GPU draws nothing. Effectively hides the pop.
        if (Float.compare(scale, 0.0f) == 0) {
            args.set(0, 0.0f);
            args.set(1, 0.0f);
            args.set(2, 0.0f);
            return;
        }

        // Multiply the animation scale by our visual scale.
        // x and z are positive, y is negative (vanilla flips Y).
        // The multiplier preserves sign so the flip stays correct.
        args.set(0, x * scale);
        args.set(1, y * scale);
        args.set(2, z * scale);
    }

    /**
     * Intercepts the {@code MatrixStack.translate(FFF)} call inside
     * {@code renderFloatingItem} to adjust the pop animation position
     * proportionally when scaling.
     *
     * <p>This ensures the pop animation stays centered on screen regardless
     * of size. The translate positions are computed from floatingItemWidth/Height
     * multiplied by window dimensions, and we adjust the Z-offset based on scale.</p>
     */
    @ModifyArgs(
            method = "renderFloatingItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V"
            ),
            require = 0
    )
    private void totemResize$modifyPopTranslate(Args args) {
        // Only adjust if scale is non-default
        float scale = TotemResizeConfig.popScale;
        if (Float.compare(scale, 1.0f) == 0 || Float.compare(scale, 0.0f) == 0) {
            return;
        }

        // Leave X and Y unchanged (centered by vanilla logic).
        // Adjust Z depth slightly for larger scales to prevent clipping.
        float z = args.get(2);
        args.set(2, z * Math.min(scale, 2.0f));
    }
}
