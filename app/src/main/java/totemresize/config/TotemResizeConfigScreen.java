package totemresize.config;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
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
 *   <li><b>Numerical Readout:</b> A clean 1–10 scale readout is displayed
 *       next to the totem, similar to Totem Tweaks' layout.</li>
 *   <li><b>Scroll Fine-Tuning:</b> Mouse scroll adjusts by ±0.1 increments.</li>
 * </ul>
 *
 * <h2>Persistence</h2>
 * <p>Changes are only finalized and saved to {@code totemscale.json}
 * when the user clicks "Save &amp; Exit". A "Reset to JSON Defaults"
 * button pulls the exact values from the provided baseline file.</p>
 */
public final class TotemResizeConfigScreen extends Screen {

    private static final int PREVIEW_BASE_SIZE = 64;
    private static final int CORNER_HANDLE_SIZE = 8;
    private static final int MIN_PREVIEW_SIZE = 8;
    private static final int MAX_PREVIEW_SIZE = 320;

    private final Screen parent;

    // ── Working copy of slider value (not saved until "Save & Exit") ──
    private double workingVisualScale;

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

    private TotemResizeConfigScreen(Screen parent) {
        super(Text.translatable("totemresize.title"));
        this.parent = parent;

        // Initialize working copy from the current saved config
        TotemResizeConfigData config = TotemResizeConfig.get();
        this.workingVisualScale = config.visualScale;
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

        // ── Bottom row: Reset | Cancel | Save & Exit ───────────────────
        int btnWidth = 100;
        int spacing = 6;
        int totalWidth = btnWidth * 3 + spacing * 2;
        int startX = centerX - totalWidth / 2;

        resetButton = ButtonWidget.builder(
                Text.literal("Reset to JSON Defaults").formatted(Formatting.YELLOW),
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

        // Compute preview center
        previewCenterX = this.width / 2;
        previewCenterY = this.height / 2 + 5;
    }

    // ── Disable default background blur ────────────────────────────────

    /**
     * Override to disable the default Minecraft background blur / dirt texture.
     * Draws a semi-transparent dark overlay instead so the player can see
     * the game world behind the config screen.
     */
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Semi-transparent dark overlay (50% opacity black)
        context.fill(0, 0, this.width, this.height, 0x80000000);
    }

    // ── Accessors for current working slider ───────────────────────────

    private void setVisualScale(double value) {
        value = Math.max(TotemScale.SLIDER_MIN, Math.min(TotemScale.SLIDER_MAX, value));
        // Round to one decimal
        value = Math.round(value * 10.0) / 10.0;
        workingVisualScale = value;
    }

    // ── Preview size from slider ───────────────────────────────────────

    /**
     * Maps the current slider value to a preview pixel size.
     * Slider 1.0 (invisible) → MIN_PREVIEW_SIZE.
     * Slider 5.0 (1.0×) → PREVIEW_BASE_SIZE.
     * Slider 10.0 (6.0×) → MAX_PREVIEW_SIZE.
     */
    private int sliderToPreviewSize(double slider) {
        float scale = TotemScale.sliderToScale(slider);
        if (scale <= 0.0f) return MIN_PREVIEW_SIZE;
        // Linear interpolation: at 1.0× → base, at 6.0× → max
        float size = PREVIEW_BASE_SIZE * scale;
        return Math.max(MIN_PREVIEW_SIZE, Math.min(MAX_PREVIEW_SIZE, (int) size));
    }

    // ── Rendering ──────────────────────────────────────────────────────

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Transparent background (no blur)
        this.renderBackground(context, mouseX, mouseY, delta);

        // Title
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 8, 0xFFFFFF);

        // ── Numerical readout panel (Totem Tweaks-style clean layout) ──
        float scale = TotemScale.sliderToScale(workingVisualScale);
        drawScaleReadout(context, scale);

        // ── Preview workspace ──────────────────────────────────────────
        previewSize = sliderToPreviewSize(workingVisualScale);
        int halfSize = previewSize / 2;

        int left = previewCenterX - halfSize;
        int top = previewCenterY - halfSize;
        int right = previewCenterX + halfSize;
        int bottom = previewCenterY + halfSize;

        // Draw workspace border (subtle)
        context.drawBorder(left - 2, top - 2, previewSize + 4, previewSize + 4, 0x44FFFFFF);

        // Draw totem item preview centered in workspace
        if (scale > 0.0f) {
            context.getMatrices().push();
            context.getMatrices().translate(previewCenterX, previewCenterY, 0);

            // Scale factor to make item match the preview size
            // An item is 16×16 pixels by default
            float itemRenderScale = previewSize / 16.0f;
            context.getMatrices().scale(itemRenderScale, itemRenderScale, 1.0f);
            context.getMatrices().translate(-8, -8, 0); // center the 16x16 item

            ItemStack totemStack = new ItemStack(Items.TOTEM_OF_UNDYING);
            context.drawItem(totemStack, 0, 0);
            context.getMatrices().pop();
        } else {
            // Invisible state — show placeholder text
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("(Invisible)").formatted(Formatting.DARK_GRAY),
                    previewCenterX, previewCenterY - 4, 0x555555);
        }

        // ── Corner handles ─────────────────────────────────────────────
        int hs = CORNER_HANDLE_SIZE / 2;
        int handleColor = dragging ? 0xFFFFFF00 : 0xFFFFFFFF;
        // Top-left
        context.fill(left - hs, top - hs, left + hs, top + hs, handleColor);
        // Top-right
        context.fill(right - hs, top - hs, right + hs, top + hs, handleColor);
        // Bottom-left
        context.fill(left - hs, bottom - hs, left + hs, bottom + hs, handleColor);
        // Bottom-right
        context.fill(right - hs, bottom - hs, right + hs, bottom + hs, handleColor);

        // ── Instructions ───────────────────────────────────────────────
        String instruction = dragging ? "Dragging... release to set size"
                : "Drag any corner to resize  |  Scroll to fine-tune";
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal(instruction).formatted(Formatting.GRAY),
                this.width / 2, this.height - 48, 0x888888);

        // Call super last to render buttons
        super.render(context, mouseX, mouseY, delta);
    }

    /**
     * Draws the clean numerical readout panel next to the totem preview.
     * Shows: slider value (1–10), multiplier (0.00×–6.00×), JSON baseline info,
     * and synced status — similar to Totem Tweaks' clean layout.
     */
    private void drawScaleReadout(DrawContext context, float scale) {
        int panelX = previewCenterX + MAX_PREVIEW_SIZE / 2 + 30;
        int panelY = previewCenterY - 50;

        // ── Large numerical value ──────────────────────────────────────
        String bigNumber = String.format("%.1f", workingVisualScale);
        context.drawTextWithShadow(this.textRenderer,
                Text.literal(bigNumber).formatted(Formatting.WHITE, Formatting.BOLD),
                panelX, panelY, 0xFFFFFF);

        context.drawTextWithShadow(this.textRenderer,
                Text.literal(" / 10").formatted(Formatting.GRAY),
                panelX + this.textRenderer.getWidth(bigNumber), panelY, 0x888888);

        // ── Multiplier line ────────────────────────────────────────────
        String multiplier = TotemScale.formatMultiplier(scale);
        context.drawTextWithShadow(this.textRenderer,
                Text.literal("Scale: " + multiplier).formatted(Formatting.AQUA),
                panelX, panelY + 14, 0x55FFFF);

        // ── JSON baseline reference ────────────────────────────────────
        context.drawTextWithShadow(this.textRenderer,
                Text.literal("JSON: [0.6, 0.6, 0.6]").formatted(Formatting.DARK_GRAY),
                panelX, panelY + 28, 0x666666);

        context.drawTextWithShadow(this.textRenderer,
                Text.literal("= Slider 5.0 = 1.00\u00D7").formatted(Formatting.DARK_GRAY),
                panelX, panelY + 38, 0x666666);

        // ── Sync indicator ─────────────────────────────────────────────
        context.drawTextWithShadow(this.textRenderer,
                Text.literal("\u2714 Held + Pop synced").formatted(Formatting.GREEN),
                panelX, panelY + 56, 0x55FF55);

        // ── Left-side quick reference scale bar ────────────────────────
        int barX = previewCenterX - MAX_PREVIEW_SIZE / 2 - 40;
        int barTop = previewCenterY - 60;
        int barHeight = 120;
        int barWidth = 6;

        // Background bar
        context.fill(barX, barTop, barX + barWidth, barTop + barHeight, 0x44FFFFFF);

        // Filled portion (based on slider 1-10)
        double fillFraction = (workingVisualScale - TotemScale.SLIDER_MIN)
                / (TotemScale.SLIDER_MAX - TotemScale.SLIDER_MIN);
        int fillHeight = (int) (barHeight * fillFraction);
        int fillTop = barTop + barHeight - fillHeight;
        int barColor = scale <= 0.0f ? 0xFFFF5555 : (scale <= 1.0f ? 0xFFFFFF55 : 0xFF55FF55);
        context.fill(barX, fillTop, barX + barWidth, barTop + barHeight, barColor);

        // Scale markers
        for (int i = 1; i <= 10; i++) {
            double frac = (i - TotemScale.SLIDER_MIN) / (TotemScale.SLIDER_MAX - TotemScale.SLIDER_MIN);
            int markerY = barTop + barHeight - (int) (barHeight * frac);
            int markerColor = (i == (int) workingVisualScale) ? 0xFFFFFFFF : 0x66FFFFFF;
            context.fill(barX - 2, markerY, barX + barWidth + 2, markerY + 1, markerColor);
        }
    }

    // ── Mouse interaction ──────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // Check if mouse is near any corner handle
            int halfSize = previewSize / 2;
            int left = previewCenterX - halfSize;
            int top = previewCenterY - halfSize;
            int right = previewCenterX + halfSize;
            int bottom = previewCenterY + halfSize;

            int hs = CORNER_HANDLE_SIZE + 4; // generous grab area
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
            // Calculate distance from center — further away = bigger
            double distNow = Math.sqrt(
                    Math.pow(mouseX - previewCenterX, 2) +
                    Math.pow(mouseY - previewCenterY, 2));
            double distStart = Math.sqrt(
                    Math.pow(dragStartX - previewCenterX, 2) +
                    Math.pow(dragStartY - previewCenterY, 2));

            if (distStart > 1.0) {
                double ratio = distNow / distStart;
                // Convert: the start slider → start scale → multiply by ratio → back to slider
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
        // Fine-tune: scroll up = +0.1, scroll down = -0.1
        double step = verticalAmount > 0 ? 0.1 : -0.1;
        setVisualScale(workingVisualScale + step);
        return true;
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private boolean isNearPoint(double mx, double my, int px, int py, int radius) {
        return Math.abs(mx - px) <= radius && Math.abs(my - py) <= radius;
    }

    // ── Pause behavior ─────────────────────────────────────────────────

    @Override
    public boolean shouldPause() {
        // Don't pause the game so the player can see the world behind the menu
        return false;
    }

    // ── Actions ────────────────────────────────────────────────────────

    private void saveAndExit() {
        // Apply working value to config data
        TotemResizeConfigData config = TotemResizeConfig.get();
        config.visualScale = workingVisualScale;
        config.invalidateCache();

        // Save to disk and refresh volatile fields
        TotemResizeConfig.save();

        close();
    }

    private void resetToDefaults() {
        // Reset working copy to JSON baseline (slider 5.0 = 1.0×)
        workingVisualScale = TotemScale.SLIDER_DEFAULT;
    }

    @Override
    public void close() {
        // Do NOT save on close — only saveAndExit writes to disk
        MinecraftClient.getInstance().setScreen(parent);
    }
}
