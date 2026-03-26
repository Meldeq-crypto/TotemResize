package totemresize.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Central configuration manager for Totem Resizer.
 *
 * <h2>Design</h2>
 * <ul>
 *   <li>Persists {@link TotemResizeConfigData} to {@code totemscale.json} in
 *       the Fabric config directory.</li>
 *   <li>Exposes <b>public static volatile</b> render-scale floats so every
 *       Mixin can read them with zero indirection.</li>
 *   <li>Changes only propagate when the user clicks "Save &amp; Exit" in the
 *       config screen — via {@link #save()}.</li>
 * </ul>
 *
 * <h2>Separate Scales</h2>
 * <p>The held-item scale and pop-animation scale are now independent.
 * X/Y offsets and a static-totem toggle are also exposed.</p>
 *
 * <h2>Backward Compatibility</h2>
 * <p>If an existing {@code totemscale.json} contains the old
 * {@code heldSlider}/{@code popSlider} fields, we migrate by averaging
 * them into the new {@code visualScale} field on first load.</p>
 */
public final class TotemResizeConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "totemscale.json";

    private static TotemResizeConfigData data = new TotemResizeConfigData();

    // ── Public static volatile fields read directly by Mixins ──────────

    /** Current held-totem render scale (read by HeldItemRendererMixin). */
    public static volatile float heldScale = 1.0f;

    /** Current pop-animation render scale (read by GameRendererMixin). */
    public static volatile float popScale = 1.0f;

    /** Current X-axis offset for the held totem. */
    public static volatile float xOffset = 0.0f;

    /** Current Y-axis offset for the held totem. */
    public static volatile float yOffset = 0.0f;

    /** Whether to disable the vanilla bobbing animation for the held totem. */
    public static volatile boolean staticTotem = false;

    private TotemResizeConfig() {
    }

    /** Returns the mutable config data object (used by the config screen). */
    public static TotemResizeConfigData get() {
        return data;
    }

    // ── Persistence ────────────────────────────────────────────────────

    /**
     * Loads config from disk (or creates a default file if absent).
     * Handles migration from the old dual-slider format.
     * Updates the public static volatile fields immediately.
     */
    public static void load() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        if (!Files.exists(configPath)) {
            save(); // create default file
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(configPath)) {
            // First try to detect and migrate from old format
            String content = new String(Files.readAllBytes(configPath));
            JsonElement jsonElement = JsonParser.parseString(content);

            if (jsonElement.isJsonObject()) {
                JsonObject obj = jsonElement.getAsJsonObject();

                // Migration: old format had heldSlider + popSlider, new has visualScale
                if (obj.has("heldSlider") && obj.has("popSlider") && !obj.has("visualScale")) {
                    double oldHeld = obj.get("heldSlider").getAsDouble();
                    double oldPop = obj.get("popSlider").getAsDouble();
                    data = new TotemResizeConfigData();
                    data.visualScale = (oldHeld + oldPop) / 2.0;
                    data.popVisualScale = data.visualScale;
                    data.clampValues();
                    data.invalidateCache();
                    save();
                    return;
                }
            }

            // Normal load
            TotemResizeConfigData loaded = GSON.fromJson(content, TotemResizeConfigData.class);
            if (loaded != null) {
                data = loaded;
                // Migration: if popVisualScale was not in the file, default to visualScale
                if (!content.contains("popVisualScale")) {
                    data.popVisualScale = data.visualScale;
                }
                data.clampValues();
                data.invalidateCache();
            }
        } catch (Exception ignored) {
            // Fall back to defaults if the file is corrupted.
        }
        refreshPublicFields();
    }

    /**
     * Saves the current config to disk and refreshes the public static
     * volatile fields so all Mixins see the update <b>instantly</b>.
     */
    public static void save() {
        data.clampValues();
        data.invalidateCache();
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        try (BufferedWriter writer = Files.newBufferedWriter(configPath)) {
            GSON.toJson(data, writer);
        } catch (Exception ignored) {
        }
        refreshPublicFields();
    }

    /**
     * Resets config to defaults and saves.
     */
    public static void resetToJsonDefaults() {
        data.resetToJsonDefaults();
        save();
    }

    /**
     * Copies the computed values into the public static volatile fields.
     * Called after every load/save so the mixin hot-path always has the
     * freshest values with zero indirection.
     */
    private static void refreshPublicFields() {
        heldScale   = data.getRenderScale();
        popScale    = data.getPopRenderScale();
        xOffset     = (float) data.xOffset;
        yOffset     = (float) data.yOffset;
        staticTotem = data.staticTotem;
    }
}
