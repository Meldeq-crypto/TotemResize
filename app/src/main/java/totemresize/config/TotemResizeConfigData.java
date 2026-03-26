package totemresize.config;

/**
 * Persisted configuration data for Totem Resizer.
 *
 * <h2>Fields</h2>
 * <ul>
 *   <li>{@code visualScale} – held-totem size multiplier (0.1 – 3.0).</li>
 *   <li>{@code xOffset} – horizontal offset for the held totem.</li>
 *   <li>{@code yOffset} – vertical offset for the held totem.</li>
 *   <li>{@code popVisualScale} – independent pop-animation size multiplier (0.1 – 3.0).</li>
 *   <li>{@code staticTotem} – when true, disables vanilla bobbing animation.</li>
 * </ul>
 *
 * <h2>Slider Mapping</h2>
 * <p>The slider value is converted to a render-scale multiplier by
 * {@link TotemScale#sliderToScale(double)} using piecewise-linear mapping:
 * <pre>
 *   1.0  → 0.0×  (invisible)
 *   5.0  → 1.0×  (JSON baseline / vanilla default)
 *   10.0 → 6.0×  (maximum coverage / full screen)
 * </pre>
 *
 * <p>The precision sliders use direct multiplier values (0.1 – 3.0) which
 * are converted to/from the internal slider representation automatically.</p>
 */
public final class TotemResizeConfigData {

    /**
     * Unified slider position for held totem visual size (1.0 – 10.0).
     * Serialized to {@code totemscale.json}.
     */
    public double visualScale = TotemScale.SLIDER_DEFAULT;

    /**
     * X-axis offset for the held totem position.
     * Range: -1.0 to 1.0, default 0.0.
     */
    public double xOffset = 0.0;

    /**
     * Y-axis offset for the held totem position.
     * Range: -1.0 to 1.0, default 0.0.
     */
    public double yOffset = 0.0;

    /**
     * Independent slider position for the totem pop animation (1.0 – 10.0).
     * Separate from the held-item scale.
     */
    public double popVisualScale = TotemScale.SLIDER_DEFAULT;

    /**
     * When true, locks the totem in place and disables vanilla bobbing.
     */
    public boolean staticTotem = false;

    // ── Legacy field names for backward compatibility with existing config files ──
    @SuppressWarnings("unused")
    private transient double heldSlider = -1;
    @SuppressWarnings("unused")
    private transient double popSlider = -1;

    // ── Cached render scales (NOT serialized – recomputed on load / save) ──
    private transient float cachedRenderScale = 1.0f;
    private transient float cachedPopRenderScale = 1.0f;
    private transient boolean dirty = true;

    /** Clamp all values to their valid ranges. */
    public void clampValues() {
        visualScale = clampSlider(visualScale);
        popVisualScale = clampSlider(popVisualScale);
        xOffset = clampOffset(xOffset);
        yOffset = clampOffset(yOffset);
    }

    /**
     * Returns the held-item render-scale multiplier.
     */
    public float getRenderScale() {
        ensureCache();
        return cachedRenderScale;
    }

    /**
     * Returns the pop-animation render-scale multiplier.
     */
    public float getPopRenderScale() {
        ensureCache();
        return cachedPopRenderScale;
    }

    /** Force re-computation of the cached render scales. */
    public void invalidateCache() {
        dirty = true;
    }

    /** Reset all values to defaults. */
    public void resetToJsonDefaults() {
        visualScale = TotemScale.SLIDER_DEFAULT;
        popVisualScale = TotemScale.SLIDER_DEFAULT;
        xOffset = 0.0;
        yOffset = 0.0;
        staticTotem = false;
        invalidateCache();
    }

    private void ensureCache() {
        if (dirty) {
            clampValues();
            cachedRenderScale = TotemScale.sliderToScale(visualScale);
            cachedPopRenderScale = TotemScale.sliderToScale(popVisualScale);
            dirty = false;
        }
    }

    private static double clampSlider(double value) {
        if (value < TotemScale.SLIDER_MIN) return TotemScale.SLIDER_MIN;
        if (value > TotemScale.SLIDER_MAX) return TotemScale.SLIDER_MAX;
        return Math.round(value * 10.0) / 10.0;
    }

    private static double clampOffset(double value) {
        if (value < -1.0) return -1.0;
        if (value > 1.0) return 1.0;
        return Math.round(value * 100.0) / 100.0;
    }
}
