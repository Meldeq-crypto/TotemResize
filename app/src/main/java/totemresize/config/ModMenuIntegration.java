package totemresize.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Mod Menu entry point so the "Configure" button appears in the mod list.
 *
 * <p>Opens the Interactive Canvas config screen — no Cloth Config dependency
 * needed. Registered via {@code fabric.mod.json} → entrypoints → "modmenu".
 */
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return TotemResizeConfigScreen::create;
    }
}
