package totemresize.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Handles loading and saving of {@link TotemResizeConfigData} to
 * {@code totemscale.json} in the Fabric config directory.
 *
 * <p>The file stores two independent slider values: {@code heldScale}
 * and {@code popScale}.
 */
public final class TotemResizeConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "totemscale.json";

    private static TotemResizeConfigData data = new TotemResizeConfigData();

    private TotemResizeConfig() {
    }

    public static TotemResizeConfigData get() {
        return data;
    }

    public static void load() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        if (!Files.exists(configPath)) {
            save(); // create default file
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(configPath)) {
            TotemResizeConfigData loaded = GSON.fromJson(reader, TotemResizeConfigData.class);
            if (loaded != null) {
                data = loaded;
                data.clampValues();
                data.invalidateCache();
            }
        } catch (Exception ignored) {
            // Fall back to defaults if file is invalid.
        }
    }

    public static void save() {
        data.clampValues();
        data.invalidateCache();
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        try (BufferedWriter writer = Files.newBufferedWriter(configPath)) {
            GSON.toJson(data, writer);
        } catch (Exception ignored) {
            // Ignore write failures.
        }
    }
}
