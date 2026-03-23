package totemresize.config;

/**
 * Utility class that converts a continuous slider value (1.0 – 10.0) into
 * a render-scale multiplier used by the Mixins.
 *
 * <h2>Slider Mapping (Piecewise-Linear)</h2>
 * <pre>
 *   Slider  1.0  →  Scale 0.0×  (Invisible – GPU draws nothing)
 *   Slider  5.0  →  Scale 1.0×  (JSON Baseline / vanilla default)
 *   Slider 10.0  →  Scale 6.0×  (Full Screen / maximum coverage)
 * </pre>
 *
 * <p>The mapping is:
 * <ul>
 *   <li><b>[1.0, 5.0]</b> → {@code (slider − 1) / 4}   (0.0× … 1.0×)</li>
 *   <li><b>[5.0, 10.0]</b> → {@code 1 + (slider − 5)}   (1.0× … 6.0×)</li>
 * </ul>
 *
 * <h2>JSON Baseline (from totem_of_undying.json)</h2>
 * <p>Value 5 on the slider aligns exactly with the scales provided in the
 * attached JSON resource pack file:
 * <ul>
 *   <li>Held first-person scale: [0.6, 0.6, 0.6]</li>
 *   <li>Held first-person translation: [11, -3, -10]</li>
 *   <li>Held first-person rotation: [0, -30, 0]</li>
 * </ul>
 *
 * <p>The multiplier returned by {@link #sliderToScale(double)} is applied
 * <em>on top of</em> whatever transform the JSON (or a resource pack's JSON)
 * specifies. This ensures 3D models and custom offsets from packs are
 * preserved but resized.
 */
public final class TotemScale {

    // ── Slider range constants ─────────────────────────────────────────

    /** Minimum slider value (fully invisible). */
    public static final double SLIDER_MIN = 1.0;

    /** Maximum slider value (6× coverage). */
    public static final double SLIDER_MAX = 10.0;

    /** Default slider value (1.0× = JSON baseline). */
    public static final double SLIDER_DEFAULT = 5.0;

    // ── JSON Baseline display values ───────────────────────────────────
    // These come from the provided totem_of_undying.json and represent
    // the "1.0×" default at slider position 5.

    /** Baseline held-item scale from JSON (uniform). */
    public static final float JSON_HELD_SCALE = 0.6f;

    /** Baseline held-item translation X from JSON. */
    public static final float JSON_HELD_TX = 11.0f;
    /** Baseline held-item translation Y from JSON. */
    public static final float JSON_HELD_TY = -3.0f;
    /** Baseline held-item translation Z from JSON. */
    public static final float JSON_HELD_TZ = -10.0f;

    /** Baseline held-item rotation Y from JSON. */
    public static final float JSON_HELD_RY = -30.0f;

    private TotemScale() {
    }

    /**
     * Converts a slider value in [1.0, 10.0] to a render-scale multiplier.
     *
     * @param slider the user-facing slider position
     * @return the render multiplier (0.0 – 6.0)
     */
    public static float sliderToScale(double slider) {
        if (slider <= SLIDER_MIN) return 0.0f;
        if (slider >= SLIDER_MAX) return 6.0f;

        if (slider <= 5.0) {
            // [1.0, 5.0] → [0.0, 1.0]
            return (float) ((slider - 1.0) / 4.0);
        } else {
            // (5.0, 10.0] → (1.0, 6.0]
            return (float) (1.0 + (slider - 5.0));
        }
    }

    /**
     * Inverse of {@link #sliderToScale(double)}. Converts a scale multiplier
     * back to a slider value.
     *
     * @param scale the render multiplier (0.0 – 6.0)
     * @return slider value (1.0 – 10.0)
     */
    public static double scaleToSlider(float scale) {
        if (scale <= 0.0f) return SLIDER_MIN;
        if (scale >= 6.0f) return SLIDER_MAX;

        if (scale <= 1.0f) {
            // [0.0, 1.0] → [1.0, 5.0]
            return 1.0 + scale * 4.0;
        } else {
            // (1.0, 6.0] → (5.0, 10.0]
            return 5.0 + (scale - 1.0);
        }
    }

    /**
     * Formats a slider value as a human-readable scale string.
     * e.g. slider 5.0 → "1.00×", slider 1.0 → "0.00× (Invisible)"
     */
    public static String formatScale(double slider) {
        float scale = sliderToScale(slider);
        if (scale == 0.0f) {
            return "0.00\u00D7 (Invisible)";
        }
        return String.format("%.2f\u00D7", scale);
    }

    /**
     * Formats a scale multiplier as a human-readable string.
     */
    public static String formatMultiplier(float scale) {
        if (scale <= 0.0f) {
            return "0.00\u00D7 (Invisible)";
        }
        return String.format("%.2f\u00D7", scale);
    }
}
