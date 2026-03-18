package totemresize.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
 *       Mixin can read them with zero indirection.  Changes propagate the
 *       instant {@link #save()} (or {@link #load()}) is called – i.e. the
 *       moment the user clicks "Save &amp; Exit".</li>
 * </ul>
 */
public final class TotemResizeConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "totemscale.json";

    private static TotemResizeConfigData data = new TotemResizeConfigData();

    // ── Public static volatile fields read directly by Mixins ──────────
    // Marked public + volatile so any Mixin hot-path can read them with
    // a single field-read – no method call, no object dereference.
    // Updated ONLY inside load() and save(), guaranteeing the game never
    // sees a half-written state.

    /** Current held-totem render scale (read by HeldItemRendererMixin). */
    public static volatile float heldScale = 1.0f;

    /** Current pop-animation render scale (read by InGameHudMixin). */
    public static volatile float popScale = 1.0f;

    private TotemResizeConfig() {
    }

    /** Returns the mutable config data object (used by the config screen). */
    public static TotemResizeConfigData get() {
        return data;
    }

    // ── Convenience getters (kept for backwards compatibility) ─────────

    /** @return current held-totem render scale */
    public static float getHeldScale() {
        return heldScale;
    }

    /** @return current pop-animation render scale */
    public static float getPopScale() {
        return popScale;
    }

    // ── Persistence ────────────────────────────────────────────────────

    /**
     * Loads config from disk (or creates a default file if absent).
     * Updates the public static volatile fields immediately.
     */
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
            // Fall back to defaults if the file is corrupted.
        }
        refreshPublicFields();
    }

    /**
     * Saves the current config to disk and refreshes the public static
     * volatile fields so all Mixins see the update <b>instantly</b>.
     *
     * <p>This is the callback wired to Cloth Config's "Save &amp; Exit".
     */
    public static void save() {
        data.clampValues();
        data.invalidateCache();
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        try (BufferedWriter writer = Files.newBufferedWriter(configPath)) {
            GSON.toJson(data, writer);
        } catch (Exception ignored) {
            // Ignore write failures.
        }
        refreshPublicFields();
    }

    /**
     * Copies the computed scales into the public static volatile fields.
     * Called after every load/save so the mixin hot-path always has the
     * freshest values with zero indirection.
     */
    private static void refreshPublicFields() {
        heldScale = data.getHeldRenderScale();
        popScale  = data.getPopRenderScale();
    }
}
