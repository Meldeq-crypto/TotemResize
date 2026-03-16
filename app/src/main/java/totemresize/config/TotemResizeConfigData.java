package totemresize.config;

/**
 * Persisted configuration data for Totem Resizer.
 *
 * <p>{@code sliderValue} is the user-facing integer in the range [1, 10].
 * The actual render scale is derived from a smooth quadratic curve:
 * <ul>
 *   <li>1  → 0.1× (barely visible)</li>
 *   <li>5  → 1.0× (default Minecraft size)</li>
 *   <li>10 → 4.0× (massive, covering majority of screen)</li>
 * </ul>
 *
 * <p>Formula: {@code scale = (1/24)v² − 0.025v + (1/12)}  where v = sliderValue.
 */
public final class TotemResizeConfigData {

    /** User-facing slider value (1–10). Serialized to totemresize.json. */
    public int sliderValue = 5;

    // ── Cached render scale (NOT serialized – recomputed on load / save) ──
    private transient float cachedRenderScale = 1.0f;
    private transient boolean dirty = true;

    /** Clamp the slider value to [1, 10] and recompute the cache. */
    public int clampedSliderValue() {
        if (sliderValue < 1) sliderValue = 1;
        if (sliderValue > 10) sliderValue = 10;
        return sliderValue;
    }

    /**
     * Returns the render-scale that mixins should use.
     * Uses the quadratic: f(v) = (1/24)v² − 0.025v + (1/12)
     * which yields f(1)=0.1, f(5)=1.0, f(10)=4.0.
     */
    public float getRenderScale() {
        if (dirty) {
            int v = clampedSliderValue();
            cachedRenderScale = computeScale(v);
            dirty = false;
        }
        return cachedRenderScale;
    }

    /** Force re-computation of the cached render scale (called after deserialization or save). */
    public void invalidateCache() {
        dirty = true;
    }

    /**
     * Quadratic mapping from slider value (1–10) to a render multiplier.
     * <p>
     * Coefficients solved from the three anchor points:
     * <pre>
     *   f(1)  = 0.1
     *   f(5)  = 1.0
     *   f(10) = 4.0
     * </pre>
     */
    public static float computeScale(int v) {
        // a = 1/24 ≈ 0.0416667,  b = -0.025,  c = 1/12 ≈ 0.0833333
        double a = 1.0 / 24.0;
        double b = -0.025;
        double c = 1.0 / 12.0;
        double scale = a * v * v + b * v + c;
        // Safety clamp
        if (scale < 0.05) scale = 0.05;
        if (scale > 5.0) scale = 5.0;
        return (float) scale;
    }
}
