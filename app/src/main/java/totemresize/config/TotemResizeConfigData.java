package totemresize.config;

/**
 * Persisted configuration data for Totem Resizer.
 *
 * <p>Two independent values are stored as integer levels (1–10):
 * <ul>
 *   <li>{@code heldScale} – size of the totem in Main Hand / Off-Hand (first-person).</li>
 *   <li>{@code popScale}  – size of the on-screen animation when a totem is consumed.</li>
 * </ul>
 *
 * <p>Each level maps to a fixed render multiplier via {@link TotemScale}:
 * <pre>
 *   1  → 0.0× (not visible)
 *   2  → 0.1× (barely visible)
 *   3  → 0.4× (small)
 *   4  → 0.7× (slightly smaller than vanilla)
 *   5  → 1.0× (vanilla / regular)
 *   6  → 1.3× (slightly larger)
 *   7  → 1.7× (larger)
 *   8  → 2.2× (big)
 *   9  → 3.0× (very large)
 *   10 → 5.0× (max size)
 * </pre>
 */
public final class TotemResizeConfigData {

    /** User-facing level for held totem (1–10). Serialized to totemscale.json. */
    public int heldScale = 5;

    /** User-facing level for totem pop effect (1–10). Serialized to totemscale.json. */
    public int popScale = 5;

    // ── Cached render scales (NOT serialized – recomputed on load / save) ──
    private transient float cachedHeldRenderScale = 1.0f;
    private transient float cachedPopRenderScale = 1.0f;
    private transient boolean dirty = true;

    /** Clamp both values to [1, 10]. */
    public void clampValues() {
        heldScale = clamp(heldScale);
        popScale = clamp(popScale);
    }

    /** Returns the {@link TotemScale} enum for the held totem level. */
    public TotemScale getHeldTotemScale() {
        return TotemScale.fromLevel(clamp(heldScale));
    }

    /** Returns the {@link TotemScale} enum for the pop effect level. */
    public TotemScale getPopTotemScale() {
        return TotemScale.fromLevel(clamp(popScale));
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
            cachedHeldRenderScale = TotemScale.fromLevel(heldScale).getScale();
            cachedPopRenderScale = TotemScale.fromLevel(popScale).getScale();
            dirty = false;
        }
    }

    private static int clamp(int value) {
        if (value < 1) return 1;
        if (value > 10) return 10;
        return value;
    }
}
