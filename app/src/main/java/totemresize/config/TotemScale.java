package totemresize.config;

/**
 * Enum representing the 1–10 scale options for the Totem Resizer dropdown.
 *
 * <p>Each option maps to a fixed render multiplier:
 * <ul>
 *   <li>{@link #INVISIBLE}       – 1: Scale 0.0× (not visible)</li>
 *   <li>{@link #BARELY_VISIBLE}  – 2: Scale 0.1×</li>
 *   <li>{@link #SMALL}           – 3: Scale 0.4×</li>
 *   <li>{@link #SLIGHTLY_SMALL}  – 4: Scale 0.7×</li>
 *   <li>{@link #REGULAR}         – 5: Scale 1.0× (vanilla)</li>
 *   <li>{@link #SLIGHTLY_LARGE}  – 6: Scale 1.3×</li>
 *   <li>{@link #LARGER}          – 7: Scale 1.7×</li>
 *   <li>{@link #BIG}             – 8: Scale 2.2×</li>
 *   <li>{@link #VERY_LARGE}      – 9: Scale 3.0×</li>
 *   <li>{@link #MAX_SIZE}        – 10: Scale 5.0×</li>
 * </ul>
 */
public enum TotemScale {
    INVISIBLE      (1,  0.0f,  "1 - Not Visible"),
    BARELY_VISIBLE (2,  0.1f,  "2 - Barely Visible (0.1×)"),
    SMALL          (3,  0.4f,  "3 - Small (0.4×)"),
    SLIGHTLY_SMALL (4,  0.7f,  "4 - Slightly Smaller (0.7×)"),
    REGULAR        (5,  1.0f,  "5 - Regular Size (1.0×)"),
    SLIGHTLY_LARGE (6,  1.3f,  "6 - Slightly Larger (1.3×)"),
    LARGER         (7,  1.7f,  "7 - Larger (1.7×)"),
    BIG            (8,  2.2f,  "8 - Big (2.2×)"),
    VERY_LARGE     (9,  3.0f,  "9 - Very Large (3.0×)"),
    MAX_SIZE       (10, 5.0f,  "10 - Max Size (5.0×)");

    private final int level;
    private final float scale;
    private final String displayName;

    TotemScale(int level, float scale, String displayName) {
        this.level = level;
        this.scale = scale;
        this.displayName = displayName;
    }

    /** The 1–10 integer level persisted in the config file. */
    public int getLevel() {
        return level;
    }

    /** The actual render multiplier. */
    public float getScale() {
        return scale;
    }

    /** Human-readable label shown in the dropdown. */
    public String getDisplayName() {
        return displayName;
    }

    /** Look up a {@code TotemScale} by its 1–10 integer level, defaulting to {@link #REGULAR}. */
    public static TotemScale fromLevel(int level) {
        for (TotemScale ts : values()) {
            if (ts.level == level) {
                return ts;
            }
        }
        return REGULAR;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
