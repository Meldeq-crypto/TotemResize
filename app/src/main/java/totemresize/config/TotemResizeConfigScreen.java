package totemresize.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Creates a Cloth Config screen with two independent categories:
 * <ol>
 *   <li><b>Held Totem</b> – size of the totem in Main Hand / Off-Hand.</li>
 *   <li><b>Totem Pop</b>  – size of the on-screen totem animation when consumed.</li>
 * </ol>
 *
 * <p>Both use a 1–10 integer slider.  Changes are only applied when the user
 * clicks <b>Save &amp; Exit</b> (via the {@code setSavingRunnable} callback).
 */
public final class TotemResizeConfigScreen {
    private TotemResizeConfigScreen() {
    }

    public static Screen create(Screen parent) {
        TotemResizeConfigData config = TotemResizeConfig.get();

        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Text.translatable("totemresize.title"))
            .setSavingRunnable(TotemResizeConfig::save);

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // ────────────────────────────────────────────────────────────────
        // Category A: Held Totem
        // ────────────────────────────────────────────────────────────────
        ConfigCategory heldCategory = builder.getOrCreateCategory(
            Text.translatable("totemresize.category.held"));

        heldCategory.addEntry(entryBuilder.startIntSlider(
                Text.translatable("totemresize.option.held_scale"),
                clamp(config.heldScale),
                1, 10)
            .setDefaultValue(5)
            .setTextGetter(value -> {
                float preview = TotemResizeConfigData.computeScale(value);
                return Text.literal(value + "  (\u2248 " + String.format("%.2f", preview) + "\u00d7 scale)");
            })
            .setTooltip(Text.translatable("totemresize.option.held_scale.tooltip"))
            .setSaveConsumer(value -> config.heldScale = value)
            .build());

        // ────────────────────────────────────────────────────────────────
        // Category B: Totem Pop
        // ────────────────────────────────────────────────────────────────
        ConfigCategory popCategory = builder.getOrCreateCategory(
            Text.translatable("totemresize.category.pop"));

        popCategory.addEntry(entryBuilder.startIntSlider(
                Text.translatable("totemresize.option.pop_scale"),
                clamp(config.popScale),
                1, 10)
            .setDefaultValue(5)
            .setTextGetter(value -> {
                float preview = TotemResizeConfigData.computeScale(value);
                return Text.literal(value + "  (\u2248 " + String.format("%.2f", preview) + "\u00d7 scale)");
            })
            .setTooltip(Text.translatable("totemresize.option.pop_scale.tooltip"))
            .setSaveConsumer(value -> config.popScale = value)
            .build());

        return builder.build();
    }

    /** Clamp helper so the slider never receives an out-of-range initial value. */
    private static int clamp(int v) {
        if (v < 1) return 1;
        if (v > 10) return 10;
        return v;
    }
}
