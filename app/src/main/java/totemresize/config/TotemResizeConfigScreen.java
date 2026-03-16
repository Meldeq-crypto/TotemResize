package totemresize.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public final class TotemResizeConfigScreen {
    private TotemResizeConfigScreen() {
    }

    /**
     * Creates the Cloth Config screen.
     *
     * <p>The slider stores a <em>temporary</em> value while the user moves it.
     * The real config is only mutated inside the {@code setSaveConsumer}, and
     * {@link TotemResizeConfig#save()} is called by the {@code setSavingRunnable}
     * – both triggered only when the user clicks <b>Save &amp; Exit</b>.
     */
    public static Screen create(Screen parent) {
        TotemResizeConfigData config = TotemResizeConfig.get();

        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Text.translatable("totemresize.title"))
            .setSavingRunnable(TotemResizeConfig::save);

        ConfigCategory category = builder.getOrCreateCategory(
            Text.translatable("totemresize.category.general"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // Integer slider 1–10.  The save consumer only writes on Save & Exit.
        category.addEntry(entryBuilder.startIntSlider(
                Text.translatable("totemresize.option.totem_scale"),
                config.clampedSliderValue(),
                1, 10)
            .setDefaultValue(5)
            .setTextGetter(value -> {
                float preview = TotemResizeConfigData.computeScale(value);
                return Text.literal(value + "  (≈ " + String.format("%.2f", preview) + "× scale)");
            })
            .setTooltip(Text.translatable("totemresize.option.totem_scale.tooltip"))
            .setSaveConsumer(value -> config.sliderValue = value)
            .build());

        return builder.build();
    }
}
