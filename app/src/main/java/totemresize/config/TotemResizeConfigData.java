package totemresize.config;

/**
 * Persisted configuration data for Totem Resizer.
 *
 * <p>Two independent slider values are stored:
 * <ul>
 *   <li>{@code heldScale} – size of the totem in Main Hand / Off-Hand (first-person).</li>
 *   <li>{@code popScale}  – size of the on-screen animation when a totem is consumed.</li>
 * </ul>
 *
 * <p>Both use the same 1–10 linear mapping:
 * <ul>
 *   <li>1  → 0.1× (barely visible)</li>
 *   <li>5  → 1.0× (default Minecraft size)</li>
 *   <li>10 → 5.0× (screen-filling)</li>
 * </ul>
 *
 * <p>Formula:
 * <pre>
 *   if (v == 5)  → 1.0
 *   if (v &lt; 5)  → v * 0.225  (linear: 1→0.225, 4→0.9 … nudged so 5→1.0)
 *   if (v &gt; 5)  → 1.0 + (v - 5) * 0.8  (linear: 6→1.8, 10→5.0)
 * </pre>
 * A simplified consistent version:
 * <pre>
 *   v &lt; 5 : scale = 0.1 + (v - 1) * (0.9 / 4)   →  1→0.1,  5→1.0
 *   v ≥ 5 : scale = 1.0 + (v - 5) * (4.0 / 5)   →  5→1.0, 10→5.0
 * </pre>
 */
public final class TotemResizeConfigData {

    /** User-facing slider value for held totem (1–10). Serialized to totemscale.json. */
    public int heldScale = 5;

    /** User-facing slider value for totem pop effect (1–10). Serialized to totemscale.json. */
    public int popScale = 5;

    // ── Cached render scales (NOT serialized – recomputed on load / save) ──
    private transient float cachedHeldRenderScale = 1.0f;
    private transient float cachedPopRenderScale = 1.0f;
    private transient boolean dirty = true;

    /** Clamp both slider values to [1, 10]. */
    public void clampValues() {
        heldScale = clamp(heldScale);
        popScale = clamp(popScale);
    }

    /**
     * Returns the render scale for the held totem (first-person).
     */
    public float getHeldRenderScale() {
        ensureCache();
        return cachedHeldRenderScale;
    }

    /**
     * Returns the render scale for the totem pop overlay effect.
     */
    public float getPopRenderScale() {
        ensureCache();
        return cachedPopRenderScale;
    }

    /** Force re-computation of the cached render scales (called after deserialization or save). */
    public void invalidateCache() {
        dirty = true;
    }

    private void ensureCache() {
        if (dirty) {
            clampValues();
            cachedHeldRenderScale = computeScale(heldScale);
            cachedPopRenderScale = computeScale(popScale);
            dirty = false;
        }
    }

    /**
     * Linear mapping from slider value (1–10) to a render multiplier.
     * <ul>
     *   <li>1  → 0.1×</li>
     *   <li>5  → 1.0× (vanilla default)</li>
     *   <li>10 → 5.0× (screen-filling)</li>
     * </ul>
     *
     * Two linear segments joined at v = 5:
     * <pre>
     *   v ≤ 5 : scale = 0.1 + (v - 1) * (0.9 / 4)
     *   v &gt; 5 : scale = 1.0 + (v - 5) * (4.0 / 5)
     * </pre>
     */
    public static float computeScale(int v) {
        v = clamp(v);
        double scale;
        if (v <= 5) {
            // 1→0.1, 2→0.325, 3→0.55, 4→0.775, 5→1.0
            scale = 0.1 + (v - 1) * (0.9 / 4.0);
        } else {
            // 5→1.0, 6→1.8, 7→2.6, 8→3.4, 9→4.2, 10→5.0
            scale = 1.0 + (v - 5) * (4.0 / 5.0);
        }
        // Safety clamp
        if (scale < 0.05) scale = 0.05;
        if (scale > 5.0) scale = 5.0;
        return (float) scale;
    }

    private static int clamp(int value) {
        if (value < 1) return 1;
        if (value > 10) return 10;
        return value;
    }
}
