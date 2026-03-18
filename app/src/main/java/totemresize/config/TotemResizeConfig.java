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
 * <p>The file stores two independent dropdown values: {@code heldScale}
 * and {@code popScale} (each an integer 1–10).
 *
 * <p>Cached render scale floats ({@link #getHeldScale()} and
 * {@link #getPopScale()}) are updated <em>only</em> when
 * {@link #save()} is called (i.e. when the user clicks "Save & Exit"),
 * ensuring in-game rendering is never disrupted mid-configuration.
 */
public final class TotemResizeConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "totemscale.json";

    private static TotemResizeConfigData data = new TotemResizeConfigData();

    // ── Local cached scale values used by mixins (fast path, no object access) ──
    private static volatile float cachedHeldScale = 1.0f;
    private static volatile float cachedPopScale = 1.0f;

    private TotemResizeConfig() {
    }

    public static TotemResizeConfigData get() {
        return data;
    }

    /**
     * Returns the held-totem render scale. Called from the mixin hot path.
     * This value is refreshed only on {@link #load()} and {@link #save()}.
     */
    public static float getHeldScale() {
        return cachedHeldScale;
    }

    /**
     * Returns the pop-animation render scale. Called from the mixin hot path.
     * This value is refreshed only on {@link #load()} and {@link #save()}.
     */
    public static float getPopScale() {
        return cachedPopScale;
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
        refreshCachedScales();
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
        refreshCachedScales();
    }

    /**
     * Copies the computed render scales from the config data object into
     * the static volatile fields used by the mixin hot path.
     * Called after every load / save so changes only take effect when
     * the user clicks "Save & Exit".
     */
    private static void refreshCachedScales() {
        cachedHeldScale = data.getHeldRenderScale();
        cachedPopScale = data.getPopRenderScale();
    }
}
