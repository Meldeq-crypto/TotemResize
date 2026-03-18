package totemresize.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import totemresize.config.TotemResizeConfig;
import totemresize.config.TotemResizeConfigScreen;

public class TotemResizeClient implements ClientModInitializer {
    public static final String MOD_ID = "totemresize";
    public static final String KEY_CATEGORY = "key.categories.totemresize";
    public static final String KEY_OPEN_CONFIG = "key.totemresize.open_config";

    private static KeyBinding openConfigKey;

    @Override
    public void onInitializeClient() {
        TotemResizeConfig.load();

        openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            KEY_OPEN_CONFIG,
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            KEY_CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openConfigKey.wasPressed()) {
                MinecraftClient.getInstance().setScreen(TotemResizeConfigScreen.create(null));
            }
        });
    }
}
