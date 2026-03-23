package totemresize.config;

/**
 * Persisted configuration data for Totem Resizer.
 *
 * <p>A single slider value is stored as a double (1.0 – 10.0):
 * <ul>
 *   <li>{@code visualScale} – controls <b>both</b> the held-totem size in
 *       first-person and the on-screen pop animation size.</li>
 * </ul>
 *
 * <p>The slider value is converted to a render-scale multiplier by
 * {@link TotemScale#sliderToScale(double)} using piecewise-linear mapping:
 * <pre>
 *   1.0  → 0.0×  (invisible)
 *   5.0  → 1.0×  (JSON baseline / vanilla default)
 *   10.0 → 6.0×  (maximum coverage / full screen)
 * </pre>
 *
 * <p>Value 5 aligns exactly with the scales from the provided
 * totem_of_undying.json (scale: [0.6, 0.6, 0.6]).
 *
 * <h2>Sync</h2>
 * <p>One single 'Visual Scale' updates <b>both</b> the held item and
 * the pop animation simultaneously so they always match.</p>
 */
public final class TotemResizeConfigData {

    /**
     * Unified slider position for totem visual size (1.0 – 10.0).
     * Controls both held-totem and pop-animation rendering.
     * Serialized to {@code totemscale.json}.
     */
    public double visualScale = TotemScale.SLIDER_DEFAULT;

    // ── Legacy field names for backward compatibility with existing config files ──
    // Gson will populate these if the old config had them; we migrate on load.
    @SuppressWarnings("unused")
    private transient double heldSlider = -1;
    @SuppressWarnings("unused")
    private transient double popSlider = -1;

    // ── Cached render scale (NOT serialized – recomputed on load / save) ──
    private transient float cachedRenderScale = 1.0f;
    private transient boolean dirty = true;

    /** Clamp the slider value to [1.0, 10.0]. */
    public void clampValues() {
        visualScale = clamp(visualScale);
    }

    /**
     * Returns the render-scale multiplier (used by both HeldItemRendererMixin
     * and GameRendererMixin).
     */
    public float getRenderScale() {
        ensureCache();
        return cachedRenderScale;
    }

    /** Force re-computation of the cached render scale. */
    public void invalidateCache() {
        dirty = true;
    }

    /** Reset the slider to the JSON baseline default (5.0 = 1.0×). */
    public void resetToJsonDefaults() {
        visualScale = TotemScale.SLIDER_DEFAULT;
        invalidateCache();
    }

    private void ensureCache() {
        if (dirty) {
            clampValues();
            cachedRenderScale = TotemScale.sliderToScale(visualScale);
            dirty = false;
        }
    }

    private static double clamp(double value) {
        if (value < TotemScale.SLIDER_MIN) return TotemScale.SLIDER_MIN;
        if (value > TotemScale.SLIDER_MAX) return TotemScale.SLIDER_MAX;
        // Round to one decimal place for clean display
        return Math.round(value * 10.0) / 10.0;
    }
}
