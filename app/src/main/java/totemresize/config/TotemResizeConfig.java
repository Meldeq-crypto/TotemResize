package totemresize.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TotemResizeConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "totemresize.json";

    private static TotemResizeConfigData data = new TotemResizeConfigData();

    private TotemResizeConfig() {
    }

    public static TotemResizeConfigData get() {
        return data;
    }

    public static void load() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        if (!Files.exists(configPath)) {
            save();
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(configPath)) {
            TotemResizeConfigData loaded = GSON.fromJson(reader, TotemResizeConfigData.class);
            if (loaded != null) {
                data = loaded;
                data.clampedSliderValue(); // ensure value is clamped
                data.invalidateCache();    // recompute render scale from loaded value
            }
        } catch (Exception ignored) {
            // Fall back to defaults if file is invalid.
        }
    }

    public static void save() {
        data.clampedSliderValue(); // ensure value is clamped before writing
        data.invalidateCache();    // recompute render scale from new value
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        try (BufferedWriter writer = Files.newBufferedWriter(configPath)) {
            GSON.toJson(data, writer);
        } catch (Exception ignored) {
            // Ignore write failures.
        }
    }
}
