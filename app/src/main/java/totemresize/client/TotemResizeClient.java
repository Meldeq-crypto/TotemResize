package totemresize.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import totemresize.config.TotemResizeConfig;
import totemresize.config.TotemResizeConfigScreen;
import totemresize.net.TotemResizePayload;

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

        registerClientPayload();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openConfigKey.wasPressed()) {
                MinecraftClient.getInstance().setScreen(TotemResizeConfigScreen.create(null));
            }
        });
    }

    private static void registerClientPayload() {
        PayloadTypeRegistry.playS2C().register(TotemResizePayload.ID, TotemResizePayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(TotemResizePayload.ID, (payload, context) -> {
            // Client-only payload; no-op.
        });
    }

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
}
