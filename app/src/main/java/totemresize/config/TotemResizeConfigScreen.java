package totemresize.config;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Interactive Canvas Configuration Screen for Totem Resizer.
 *
 * <h2>Transparent Background</h2>
 * <p>The default Minecraft background blur / dirt texture is disabled.
 * Instead, a semi-transparent dark overlay is drawn so the player can see
 * the actual game world behind the totem they are resizing.</p>
 *
 * <h2>Hybrid UI</h2>
 * <ul>
 *   <li><b>Corner-Drag Resizing:</b> Users click and drag the corners of
 *       the totem preview to scale it up or down.</li>
 *   <li><b>Precision Sliders:</b> Scale (0.1–3.0), X-Offset, Y-Offset
 *       that sync in real-time with the drag.</li>
 *   <li><b>Pop Scale Slider:</b> Independent control for the pop animation size.</li>
 *   <li><b>Static Toggle:</b> Locks the totem, disabling bobbing animation.</li>
 *   <li><b>Scroll Fine-Tuning:</b> Mouse scroll adjusts by ±0.1 increments.</li>
 * </ul>
 *
 * <h2>Persistence</h2>
 * <p>Changes are only finalized and saved to {@code totemscale.json}
 * when the user clicks "Save &amp; Exit".</p>
 */
public final class TotemResizeConfigScreen extends Screen {

    private static final int PREVIEW_BASE_SIZE = 64;
    private static final int CORNER_HANDLE_SIZE = 8;
    private static final int MIN_PREVIEW_SIZE = 8;
    private static final int MAX_PREVIEW_SIZE = 320;

    // ── Precision slider ranges ────────────────────────────────────────
    private static final double PRECISION_SCALE_MIN = 0.1;
    private static final double PRECISION_SCALE_MAX = 3.0;
    private static final double OFFSET_MIN = -1.0;
    private static final double OFFSET_MAX = 1.0;

    private final Screen parent;

    // ── Working copies (not saved until "Save & Exit") ─────────────────
    private double workingVisualScale;
    private double workingPopVisualScale;
    private double workingXOffset;
    private double workingYOffset;
    private boolean workingStaticTotem;

    // ── Corner-drag state ──────────────────────────────────────────────
    private boolean dragging = false;
    private int dragStartX, dragStartY;
    private double dragStartSlider;

    // ── Preview geometry (computed each frame) ─────────────────────────
    private int previewCenterX, previewCenterY;
    private int previewSize;

    // ── Buttons ────────────────────────────────────────────────────────
    private ButtonWidget saveButton;
    private ButtonWidget resetButton;
    private ButtonWidget cancelButton;
    private ButtonWidget staticToggleButton;

    // ── Precision Sliders ──────────────────────────────────────────────
    private PrecisionSlider scaleSlider;
    private PrecisionSlider xOffsetSlider;
    private PrecisionSlider yOffsetSlider;
    private PrecisionSlider popScaleSlider;

    private TotemResizeConfigScreen(Screen parent) {
        super(Text.translatable("totemresize.title"));
        this.parent = parent;

        TotemResizeConfigData config = TotemResizeConfig.get();
        this.workingVisualScale = config.visualScale;
        this.workingPopVisualScale = config.popVisualScale;
        this.workingXOffset = config.xOffset;
        this.workingYOffset = config.yOffset;
        this.workingStaticTotem = config.staticTotem;
    }

    /**
     * Factory method — the only public way to create this screen.
     */
    public static Screen create(Screen parent) {
        return new TotemResizeConfigScreen(parent);
    }

    // ── Screen lifecycle ───────────────────────────────────────────────

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int bottomY = this.height - 30;

        // Compute preview center (shifted left to make room for sliders on the right)
        previewCenterX = this.width / 2 - 60;
        previewCenterY = this.height / 2 - 10;

        // ── Precision Sliders (right side panel) ───────────────────────
        int sliderX = previewCenterX + MAX_PREVIEW_SIZE / 2 + 20;
        int sliderWidth = Math.min(140, this.width - sliderX - 10);
        if (sliderWidth < 80) sliderWidth = 80;
        int sliderY = previewCenterY - 70;

        // Scale slider: maps precision range (0.1–3.0) to internal slider (1.0–10.0)
        scaleSlider = new PrecisionSlider(
                sliderX, sliderY, sliderWidth, 20,
                "Held Scale",
                PRECISION_SCALE_MIN, PRECISION_SCALE_MAX,
                precisionFromSlider(workingVisualScale)
        ) {
            @Override
            protected void updateMessage() {
                setMessage(Text.literal("Held Scale: " + String.format("%.2f", getValue()) + "\u00D7")
                        .formatted(Formatting.WHITE));
            }

            @Override
            protected void applyValueImpl() {
                setVisualScaleFromPrecision(getValue());
            }
        };
        addDrawableChild(scaleSlider);

        // X-Offset slider
        xOffsetSlider = new PrecisionSlider(
                sliderX, sliderY + 26, sliderWidth, 20,
                "X-Offset",
                OFFSET_MIN, OFFSET_MAX,
                workingXOffset
        ) {
            @Override
            protected void updateMessage() {
                setMessage(Text.literal("X-Offset: " + String.format("%+.2f", getValue()))
                        .formatted(Formatting.WHITE));
            }

            @Override
            protected void applyValueImpl() {
                workingXOffset = getValue();
            }
        };
        addDrawableChild(xOffsetSlider);

        // Y-Offset slider
        yOffsetSlider = new PrecisionSlider(
                sliderX, sliderY + 52, sliderWidth, 20,
                "Y-Offset",
                OFFSET_MIN, OFFSET_MAX,
                workingYOffset
        ) {
            @Override
            protected void updateMessage() {
                setMessage(Text.literal("Y-Offset: " + String.format("%+.2f", getValue()))
                        .formatted(Formatting.WHITE));
            }

            @Override
            protected void applyValueImpl() {
                workingYOffset = getValue();
            }
        };
        addDrawableChild(yOffsetSlider);

        // ── Separator label for pop section ────────────────────────────
        // Pop Scale slider (independent)
        popScaleSlider = new PrecisionSlider(
                sliderX, sliderY + 90, sliderWidth, 20,
                "Pop Scale",
                PRECISION_SCALE_MIN, PRECISION_SCALE_MAX,
                precisionFromSlider(workingPopVisualScale)
        ) {
            @Override
            protected void updateMessage() {
                setMessage(Text.literal("Pop Scale: " + String.format("%.2f", getValue()) + "\u00D7")
                        .formatted(Formatting.GOLD));
            }

            @Override
            protected void applyValueImpl() {
                setPopScaleFromPrecision(getValue());
            }
        };
        addDrawableChild(popScaleSlider);

        // ── Static Toggle Button ───────────────────────────────────────
        staticToggleButton = ButtonWidget.builder(
                getStaticToggleText(),
                btn -> {
                    workingStaticTotem = !workingStaticTotem;
                    btn.setMessage(getStaticToggleText());
                })
                .dimensions(sliderX, sliderY + 122, sliderWidth, 20)
                .build();
        addDrawableChild(staticToggleButton);

        // ── Bottom row: Reset | Cancel | Save & Exit ───────────────────
        int btnWidth = 100;
        int spacing = 6;
        int totalWidth = btnWidth * 3 + spacing * 2;
        int startX = centerX - totalWidth / 2;

        resetButton = ButtonWidget.builder(
                Text.literal("Reset to Defaults").formatted(Formatting.YELLOW),
                btn -> resetToDefaults())
                .dimensions(startX, bottomY, btnWidth, 20)
                .build();

        cancelButton = ButtonWidget.builder(
                Text.literal("Cancel"),
                btn -> close())
                .dimensions(startX + btnWidth + spacing, bottomY, btnWidth, 20)
                .build();

        saveButton = ButtonWidget.builder(
                Text.literal("Save & Exit").formatted(Formatting.GREEN),
                btn -> saveAndExit())
                .dimensions(startX + (btnWidth + spacing) * 2, bottomY, btnWidth, 20)
                .build();

        addDrawableChild(resetButton);
        addDrawableChild(cancelButton);
        addDrawableChild(saveButton);
    }

    // ── Disable default background blur ────────────────────────────────

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0x80000000);
    }

    // ── Precision ↔ Internal slider conversion ─────────────────────────

    /**
     * Converts the internal slider value (1.0–10.0) to a precision multiplier.
     * The precision range is 0.1–3.0 which maps to the slider range.
     */
    private double precisionFromSlider(double slider) {
        float scale = TotemScale.sliderToScale(slider);
        return Math.max(PRECISION_SCALE_MIN, Math.min(PRECISION_SCALE_MAX, scale));
    }

    /**
     * Converts a precision multiplier (0.1–3.0) back to the internal slider value.
     */
    private double sliderFromPrecision(double precision) {
        return TotemScale.scaleToSlider((float) precision);
    }

    private void setVisualScale(double sliderValue) {
        sliderValue = Math.max(TotemScale.SLIDER_MIN, Math.min(TotemScale.SLIDER_MAX, sliderValue));
        sliderValue = Math.round(sliderValue * 10.0) / 10.0;
        workingVisualScale = sliderValue;
        // Sync the precision slider
        if (scaleSlider != null) {
            scaleSlider.setSilent(precisionFromSlider(sliderValue));
        }
    }

    private void setVisualScaleFromPrecision(double precision) {
        precision = Math.max(PRECISION_SCALE_MIN, Math.min(PRECISION_SCALE_MAX, precision));
        workingVisualScale = sliderFromPrecision(precision);
    }

    private void setPopScaleFromPrecision(double precision) {
        precision = Math.max(PRECISION_SCALE_MIN, Math.min(PRECISION_SCALE_MAX, precision));
        workingPopVisualScale = sliderFromPrecision(precision);
    }

    private Text getStaticToggleText() {
        if (workingStaticTotem) {
            return Text.literal("\u2714 Static: ON").formatted(Formatting.GREEN);
        } else {
            return Text.literal("\u2718 Static: OFF").formatted(Formatting.GRAY);
        }
    }

    // ── Preview size from slider ───────────────────────────────────────

    private int sliderToPreviewSize(double slider) {
        float scale = TotemScale.sliderToScale(slider);
        if (scale <= 0.0f) return MIN_PREVIEW_SIZE;
        float size = PREVIEW_BASE_SIZE * scale;
        return Math.max(MIN_PREVIEW_SIZE, Math.min(MAX_PREVIEW_SIZE, (int) size));
    }

    // ── Rendering ──────────────────────────────────────────────────────

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        // Title
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 8, 0xFFFFFF);

        // ── Numerical readout panel ────────────────────────────────────
        float scale = TotemScale.sliderToScale(workingVisualScale);
        drawScaleReadout(context, scale);

        // ── Preview workspace ──────────────────────────────────────────
        previewSize = sliderToPreviewSize(workingVisualScale);
        int halfSize = previewSize / 2;

        // Apply offset to preview position for visual feedback
        int offsetPixelsX = (int) (workingXOffset * 40);
        int offsetPixelsY = (int) (workingYOffset * 40);

        int drawCenterX = previewCenterX + offsetPixelsX;
        int drawCenterY = previewCenterY + offsetPixelsY;

        int left = drawCenterX - halfSize;
        int top = drawCenterY - halfSize;
        int right = drawCenterX + halfSize;
        int bottom = drawCenterY + halfSize;

        // Draw workspace border (subtle)
        context.drawBorder(left - 2, top - 2, previewSize + 4, previewSize + 4, 0x44FFFFFF);

        // Draw totem item preview centered in workspace
        if (scale > 0.0f) {
            context.getMatrices().push();
            context.getMatrices().translate(drawCenterX, drawCenterY, 0);

            float itemRenderScale = previewSize / 16.0f;
            context.getMatrices().scale(itemRenderScale, itemRenderScale, 1.0f);
            context.getMatrices().translate(-8, -8, 0);

            ItemStack totemStack = new ItemStack(Items.TOTEM_OF_UNDYING);
            context.drawItem(totemStack, 0, 0);
            context.getMatrices().pop();
        } else {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("(Invisible)").formatted(Formatting.DARK_GRAY),
                    drawCenterX, drawCenterY - 4, 0x555555);
        }

        // ── Corner handles ─────────────────────────────────────────────
        int hs = CORNER_HANDLE_SIZE / 2;
        int handleColor = dragging ? 0xFFFFFF00 : 0xFFFFFFFF;
        context.fill(left - hs, top - hs, left + hs, top + hs, handleColor);
        context.fill(right - hs, top - hs, right + hs, top + hs, handleColor);
        context.fill(left - hs, bottom - hs, left + hs, bottom + hs, handleColor);
        context.fill(right - hs, bottom - hs, right + hs, bottom + hs, handleColor);

        // ── Section labels for the sliders ─────────────────────────────
        int sliderX = previewCenterX + MAX_PREVIEW_SIZE / 2 + 20;
        int sliderY = previewCenterY - 70;

        // "Held Totem" header
        context.drawTextWithShadow(this.textRenderer,
                Text.literal("── Held Totem ──").formatted(Formatting.AQUA),
                sliderX, sliderY - 12, 0x55FFFF);

        // "Pop Animation" header
        context.drawTextWithShadow(this.textRenderer,
                Text.literal("── Pop Animation ──").formatted(Formatting.GOLD),
                sliderX, sliderY + 78, 0xFFAA00);

        // "Options" header
        context.drawTextWithShadow(this.textRenderer,
                Text.literal("── Options ──").formatted(Formatting.WHITE),
                sliderX, sliderY + 112, 0xFFFFFF);

        // ── Instructions ───────────────────────────────────────────────
        String instruction = dragging ? "Dragging... release to set size"
                : "Drag corners to resize  |  Scroll to fine-tune  |  Use sliders for precision";
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal(instruction).formatted(Formatting.GRAY),
                this.width / 2, this.height - 48, 0x888888);

        super.render(context, mouseX, mouseY, delta);
    }

    /**
     * Draws the clean numerical readout panel (left side of preview).
     */
    private void drawScaleReadout(DrawContext context, float scale) {
        int barX = previewCenterX - MAX_PREVIEW_SIZE / 2 - 50;
        int panelY = previewCenterY - 60;

        // ── Large numerical value ──────────────────────────────────────
        String bigNumber = String.format("%.1f", workingVisualScale);
        context.drawTextWithShadow(this.textRenderer,
                Text.literal(bigNumber).formatted(Formatting.WHITE, Formatting.BOLD),
                barX, panelY, 0xFFFFFF);

        context.drawTextWithShadow(this.textRenderer,
                Text.literal(" / 10").formatted(Formatting.GRAY),
                barX + this.textRenderer.getWidth(bigNumber), panelY, 0x888888);

        // ── Multiplier line ────────────────────────────────────────────
        String multiplier = TotemScale.formatMultiplier(scale);
        context.drawTextWithShadow(this.textRenderer,
                Text.literal("Held: " + multiplier).formatted(Formatting.AQUA),
                barX, panelY + 14, 0x55FFFF);

        // ── Pop scale info ─────────────────────────────────────────────
        float popScale = TotemScale.sliderToScale(workingPopVisualScale);
        String popMultiplier = TotemScale.formatMultiplier(popScale);
        context.drawTextWithShadow(this.textRenderer,
                Text.literal("Pop: " + popMultiplier).formatted(Formatting.GOLD),
                barX, panelY + 28, 0xFFAA00);

        // ── Offset info ────────────────────────────────────────────────
        context.drawTextWithShadow(this.textRenderer,
                Text.literal(String.format("Pos: %+.2f, %+.2f", workingXOffset, workingYOffset))
                        .formatted(Formatting.GRAY),
                barX, panelY + 42, 0x888888);

        // ── Static indicator ───────────────────────────────────────────
        if (workingStaticTotem) {
            context.drawTextWithShadow(this.textRenderer,
                    Text.literal("\u2714 Static Mode").formatted(Formatting.GREEN),
                    barX, panelY + 56, 0x55FF55);
        }

        // ── Left-side scale bar ────────────────────────────────────────
        int scaleBarX = barX - 14;
        int barTop = previewCenterY - 60;
        int barHeight = 120;
        int barWidth = 6;

        context.fill(scaleBarX, barTop, scaleBarX + barWidth, barTop + barHeight, 0x44FFFFFF);

        double fillFraction = (workingVisualScale - TotemScale.SLIDER_MIN)
                / (TotemScale.SLIDER_MAX - TotemScale.SLIDER_MIN);
        int fillHeight = (int) (barHeight * fillFraction);
        int fillTop = barTop + barHeight - fillHeight;
        int barColor = scale <= 0.0f ? 0xFFFF5555 : (scale <= 1.0f ? 0xFFFFFF55 : 0xFF55FF55);
        context.fill(scaleBarX, fillTop, scaleBarX + barWidth, barTop + barHeight, barColor);

        for (int i = 1; i <= 10; i++) {
            double frac = (i - TotemScale.SLIDER_MIN) / (TotemScale.SLIDER_MAX - TotemScale.SLIDER_MIN);
            int markerY = barTop + barHeight - (int) (barHeight * frac);
            int markerColor = (i == (int) workingVisualScale) ? 0xFFFFFFFF : 0x66FFFFFF;
            context.fill(scaleBarX - 2, markerY, scaleBarX + barWidth + 2, markerY + 1, markerColor);
        }
    }

    // ── Mouse interaction ──────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int halfSize = previewSize / 2;
            int offsetPixelsX = (int) (workingXOffset * 40);
            int offsetPixelsY = (int) (workingYOffset * 40);
            int drawCenterX = previewCenterX + offsetPixelsX;
            int drawCenterY = previewCenterY + offsetPixelsY;

            int left = drawCenterX - halfSize;
            int top = drawCenterY - halfSize;
            int right = drawCenterX + halfSize;
            int bottom = drawCenterY + halfSize;

            int hs = CORNER_HANDLE_SIZE + 4;
            if (isNearPoint(mouseX, mouseY, left, top, hs) ||
                isNearPoint(mouseX, mouseY, right, top, hs) ||
                isNearPoint(mouseX, mouseY, left, bottom, hs) ||
                isNearPoint(mouseX, mouseY, right, bottom, hs)) {
                dragging = true;
                dragStartX = (int) mouseX;
                dragStartY = (int) mouseY;
                dragStartSlider = workingVisualScale;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging && button == 0) {
            int offsetPixelsX = (int) (workingXOffset * 40);
            int offsetPixelsY = (int) (workingYOffset * 40);
            int drawCenterX = previewCenterX + offsetPixelsX;
            int drawCenterY = previewCenterY + offsetPixelsY;

            double distNow = Math.sqrt(
                    Math.pow(mouseX - drawCenterX, 2) +
                    Math.pow(mouseY - drawCenterY, 2));
            double distStart = Math.sqrt(
                    Math.pow(dragStartX - drawCenterX, 2) +
                    Math.pow(dragStartY - drawCenterY, 2));

            if (distStart > 1.0) {
                double ratio = distNow / distStart;
                float startScale = TotemScale.sliderToScale(dragStartSlider);
                float newScale = (float) (startScale * ratio);
                newScale = Math.max(0.0f, Math.min(6.0f, newScale));
                setVisualScale(TotemScale.scaleToSlider(newScale));
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && dragging) {
            dragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // Only scroll-adjust if mouse is not over a slider widget
        double step = verticalAmount > 0 ? 0.1 : -0.1;
        setVisualScale(workingVisualScale + step);
        return true;
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private boolean isNearPoint(double mx, double my, int px, int py, int radius) {
        return Math.abs(mx - px) <= radius && Math.abs(my - py) <= radius;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // ── Actions ────────────────────────────────────────────────────────

    private void saveAndExit() {
        TotemResizeConfigData config = TotemResizeConfig.get();
        config.visualScale = workingVisualScale;
        config.popVisualScale = workingPopVisualScale;
        config.xOffset = workingXOffset;
        config.yOffset = workingYOffset;
        config.staticTotem = workingStaticTotem;
        config.invalidateCache();

        TotemResizeConfig.save();
        close();
    }

    private void resetToDefaults() {
        workingVisualScale = TotemScale.SLIDER_DEFAULT;
        workingPopVisualScale = TotemScale.SLIDER_DEFAULT;
        workingXOffset = 0.0;
        workingYOffset = 0.0;
        workingStaticTotem = false;

        // Re-sync all slider widgets
        if (scaleSlider != null) scaleSlider.setSilent(precisionFromSlider(workingVisualScale));
        if (xOffsetSlider != null) xOffsetSlider.setSilent(workingXOffset);
        if (yOffsetSlider != null) yOffsetSlider.setSilent(workingYOffset);
        if (popScaleSlider != null) popScaleSlider.setSilent(precisionFromSlider(workingPopVisualScale));
        if (staticToggleButton != null) staticToggleButton.setMessage(getStaticToggleText());
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }

    // ═══════════════════════════════════════════════════════════════════
    // ── Inner class: PrecisionSlider ───────────────────────────────────
    // ═══════════════════════════════════════════════════════════════════

    /**
     * A slider widget that maps a continuous range [min, max] and supports
     * programmatic updates (for syncing with drag).
     */
    private static abstract class PrecisionSlider extends SliderWidget {
        private final double minVal;
        private final double maxVal;
        private boolean silent = false;

        protected PrecisionSlider(int x, int y, int width, int height,
                                  String label, double min, double max, double initial) {
            super(x, y, width, height, Text.literal(label), toFraction(initial, min, max));
            this.minVal = min;
            this.maxVal = max;
            updateMessage();
        }

        /** Returns the actual value in [min, max]. */
        public double getValue() {
            double raw = this.value * (maxVal - minVal) + minVal;
            // Round to 2 decimal places
            return Math.round(raw * 100.0) / 100.0;
        }

        /**
         * Set the slider value programmatically without triggering applyValue.
         * Used to sync from drag → slider.
         */
        public void setSilent(double val) {
            silent = true;
            this.value = toFraction(val, minVal, maxVal);
            updateMessage();
            silent = false;
        }

        @Override
        protected void applyValue() {
            if (!silent) {
                applyValueImpl();
            }
        }

        /** Subclass hook for when the user drags this slider. */
        protected abstract void applyValueImpl();

        private static double toFraction(double val, double min, double max) {
            if (max <= min) return 0.0;
            double f = (val - min) / (max - min);
            return Math.max(0.0, Math.min(1.0, f));
        }
    }
}
