package totemresize.config;

/**
 * Enum representing the 1–10 scale options for the Totem Resizer dropdown.
 *
 * <p>Each option maps to a fixed render multiplier following the spec:
 * <ul>
 *   <li>{@link #INVISIBLE}       – 1: Scale 0.0× (GPU draws nothing)</li>
 *   <li>{@link #BARELY_VISIBLE}  – 2: Scale 0.1× (minimalist)</li>
 *   <li>{@link #SMALL}           – 3: Scale 0.4× (small)</li>
 *   <li>{@link #SLIGHTLY_SMALL}  – 4: Scale 0.7× (slightly smaller than vanilla)</li>
 *   <li>{@link #VANILLA}         – 5: Scale 1.0× (vanilla standard)</li>
 *   <li>{@link #SLIGHTLY_LARGE}  – 6: Scale 1.5× (slightly larger)</li>
 *   <li>{@link #LARGE}           – 7: Scale 2.0× (large)</li>
 *   <li>{@link #VERY_LARGE}      – 8: Scale 3.0× (extra large)</li>
 *   <li>{@link #HUGE}            – 9: Scale 4.0× (huge)</li>
 *   <li>{@link #SCREEN_OVERLOAD} – 10: Scale 6.0× (screen overload)</li>
 * </ul>
 */
public enum TotemScale {

    INVISIBLE      (1,  0.0f, "1 – Invisible",
            "The GPU will not draw anything. 100% removal of the effect."),
    BARELY_VISIBLE (2,  0.1f, "2 – Minimalist (0.1×)",
            "A tiny hint of the totem – almost invisible."),
    SMALL          (3,  0.4f, "3 – Small (0.4×)",
            "A compact, unobtrusive size."),
    SLIGHTLY_SMALL (4,  0.7f, "4 – Slightly Small (0.7×)",
            "Just a little smaller than vanilla."),
    VANILLA        (5,  1.0f, "5 – Vanilla (1.0×)",
            "The default Minecraft size – no modification."),
    SLIGHTLY_LARGE (6,  1.5f, "6 – Slightly Large (1.5×)",
            "A noticeable bump up from vanilla."),
    LARGE          (7,  2.0f, "7 – Large (2.0×)",
            "Double the regular size – easy to see."),
    VERY_LARGE     (8,  3.0f, "8 – Extra Large (3.0×)",
            "Three times vanilla – fills a good portion of the screen."),
    HUGE           (9,  4.0f, "9 – Huge (4.0×)",
            "Four times the base size – can't miss it."),
    SCREEN_OVERLOAD(10, 6.0f, "10 – Screen Overload (6.0×)",
            "Maximum spectacle – fills the entire screen.");

    private final int level;
    private final float scale;
    private final String displayName;
    private final String description;

    TotemScale(int level, float scale, String displayName, String description) {
        this.level = level;
        this.scale = scale;
        this.displayName = displayName;
        this.description = description;
    }

    /** The 1–10 integer level persisted in the config file. */
    public int getLevel() {
        return level;
    }

    /** The actual render multiplier applied to the PoseStack. */
    public float getScale() {
        return scale;
    }

    /** Human-readable label shown in the dropdown. */
    public String getDisplayName() {
        return displayName;
    }

    /** Short sentence describing what this scale does. */
    public String getDescription() {
        return description;
    }

    /** Look up a {@code TotemScale} by its 1–10 integer level, defaulting to {@link #VANILLA}. */
    public static TotemScale fromLevel(int level) {
        for (TotemScale ts : values()) {
            if (ts.level == level) {
                return ts;
            }
        }
        return VANILLA;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
