package totemresize.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Creates a Cloth Config screen with two clearly separated sub-categories:
 * <ol>
 *   <li><b>Handheld Display</b> – size of the totem in Main Hand / Off-Hand.</li>
 *   <li><b>On-Screen Animation</b> – size of the pop overlay when consumed.</li>
 * </ol>
 *
 * <p>Both use a <b>dropdown (enum selector)</b> with the {@link TotemScale}
 * enum, offering 10 fixed-size options from "Invisible" (0.0×) to
 * "Screen Overload" (6.0×).
 *
 * <p>Each dropdown has a description text underneath explaining what the
 * currently selected size does.
 *
 * <p>The saving callback performs {@link TotemResizeConfig#save()} <b>and</b>
 * {@link TotemResizeConfig#load()} so the public static volatile fields
 * used by the Mixins are refreshed instantly.
 */
public final class TotemResizeConfigScreen {
    private TotemResizeConfigScreen() {
    }

    public static Screen create(Screen parent) {
        TotemResizeConfigData config = TotemResizeConfig.get();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("totemresize.title"))
                .setSavingRunnable(() -> {
                    // Save to disk, then reload so the public static volatile
                    // fields in TotemResizeConfig are refreshed immediately.
                    TotemResizeConfig.save();
                    TotemResizeConfig.load();
                });

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // ────────────────────────────────────────────────────────────────
        // Single category with two sub-categories for a professional look
        // ────────────────────────────────────────────────────────────────
        ConfigCategory mainCategory = builder.getOrCreateCategory(
                Text.translatable("totemresize.category.main"));

        // ── Sub-Category 1: Handheld Display ───────────────────────────
        SubCategoryBuilder heldSub = entryBuilder.startSubCategory(
                Text.translatable("totemresize.subcategory.held"));
        heldSub.setExpanded(true);

        heldSub.add(entryBuilder.startTextDescription(
                Text.translatable("totemresize.subcategory.held.desc")
                        .formatted(Formatting.GRAY))
                .build());

        heldSub.add(entryBuilder.startEnumSelector(
                        Text.translatable("totemresize.option.held_scale"),
                        TotemScale.class,
                        config.getHeldTotemScale())
                .setDefaultValue(TotemScale.VANILLA)
                .setEnumNameProvider(value -> Text.literal(((TotemScale) value).getDisplayName()))
                .setTooltip(Text.translatable("totemresize.option.held_scale.tooltip"))
                .setSaveConsumer(value -> config.heldScale = value.getLevel())
                .build());

        heldSub.add(entryBuilder.startTextDescription(
                Text.translatable("totemresize.option.held_scale.detail")
                        .formatted(Formatting.DARK_GRAY, Formatting.ITALIC))
                .build());

        mainCategory.addEntry(heldSub.build());

        // ── Sub-Category 2: On-Screen Animation (The Pop) ─────────────
        SubCategoryBuilder popSub = entryBuilder.startSubCategory(
                Text.translatable("totemresize.subcategory.pop"));
        popSub.setExpanded(true);

        popSub.add(entryBuilder.startTextDescription(
                Text.translatable("totemresize.subcategory.pop.desc")
                        .formatted(Formatting.GRAY))
                .build());

        popSub.add(entryBuilder.startEnumSelector(
                        Text.translatable("totemresize.option.pop_scale"),
                        TotemScale.class,
                        config.getPopTotemScale())
                .setDefaultValue(TotemScale.VANILLA)
                .setEnumNameProvider(value -> Text.literal(((TotemScale) value).getDisplayName()))
                .setTooltip(Text.translatable("totemresize.option.pop_scale.tooltip"))
                .setSaveConsumer(value -> config.popScale = value.getLevel())
                .build());

        popSub.add(entryBuilder.startTextDescription(
                Text.translatable("totemresize.option.pop_scale.detail")
                        .formatted(Formatting.DARK_GRAY, Formatting.ITALIC))
                .build());

        mainCategory.addEntry(popSub.build());

        return builder.build();
    }
}
