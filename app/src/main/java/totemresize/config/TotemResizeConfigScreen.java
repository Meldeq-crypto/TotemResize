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
 * <p>Both use a <b>dropdown (enum selector)</b> with the {@link TotemScale}
 * enum, offering 10 fixed-size options from "Not Visible" (0.0×) to
 * "Max Size" (5.0×).
 *
 * <p>Changes are only applied when the user clicks <b>Save &amp; Exit</b>
 * (via the {@code setSavingRunnable} callback), which persists to disk
 * and refreshes the cached render scales used by the mixins.
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

        heldCategory.addEntry(entryBuilder.startEnumSelector(
                Text.translatable("totemresize.option.held_scale"),
                TotemScale.class,
                config.getHeldTotemScale())
            .setDefaultValue(TotemScale.REGULAR)
            .setEnumNameProvider(value -> Text.literal(((TotemScale) value).getDisplayName()))
            .setTooltip(Text.translatable("totemresize.option.held_scale.tooltip"))
            .setSaveConsumer(value -> config.heldScale = value.getLevel())
            .build());

        // ────────────────────────────────────────────────────────────────
        // Category B: Totem Pop
        // ────────────────────────────────────────────────────────────────
        ConfigCategory popCategory = builder.getOrCreateCategory(
            Text.translatable("totemresize.category.pop"));

        popCategory.addEntry(entryBuilder.startEnumSelector(
                Text.translatable("totemresize.option.pop_scale"),
                TotemScale.class,
                config.getPopTotemScale())
            .setDefaultValue(TotemScale.REGULAR)
            .setEnumNameProvider(value -> Text.literal(((TotemScale) value).getDisplayName()))
            .setTooltip(Text.translatable("totemresize.option.pop_scale.tooltip"))
            .setSaveConsumer(value -> config.popScale = value.getLevel())
            .build());

        return builder.build();
    }
}
