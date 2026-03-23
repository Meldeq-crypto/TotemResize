package totemresize.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Utility helpers for Totem Resizer.
 *
 * <h2>Resource Pack Synergy</h2>
 * <p>The mod detects if a resource pack provides its own
 * {@code totem_of_undying.json} model. If it does, the mod reads the
 * resource pack's display values and multiplies them by the user's custom
 * scale — instead of overriding them. This preserves 3D models, custom
 * offsets, and translations from resource packs while still allowing
 * the user to resize.
 */
public final class TotemResizeUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger("TotemResize");
    private static final Identifier TOTEM_MODEL_ID =
            Identifier.of("minecraft", "models/item/totem_of_undying.json");

    private TotemResizeUtil() {
    }

    /**
     * Checks whether the given ItemStack is a Totem of Undying.
     */
    public static boolean isTotem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return stack.isOf(Items.TOTEM_OF_UNDYING);
    }

    /**
     * Represents parsed display transforms from a totem model JSON.
     */
    public static class DisplayTransform {
        public float scaleX = 0.6f, scaleY = 0.6f, scaleZ = 0.6f;
        public float transX = 11.0f, transY = -3.0f, transZ = -10.0f;
        public float rotX = 0.0f, rotY = -30.0f, rotZ = 0.0f;

        /** Whether this transform was loaded from a resource pack (vs. defaults). */
        public boolean fromResourcePack = false;
    }

    /**
     * Attempts to load the first-person right-hand display transforms from
     * the currently active resource pack's totem_of_undying.json.
     *
     * <p>If a resource pack provides the model, returns its values with
     * {@code fromResourcePack = true}. Otherwise returns the JSON baseline
     * defaults.
     *
     * @return the display transform (never null)
     */
    public static DisplayTransform loadResourcePackTransform() {
        DisplayTransform result = new DisplayTransform();

        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.getResourceManager() == null) {
                return result;
            }

            Optional<Resource> resourceOpt = client.getResourceManager().getResource(TOTEM_MODEL_ID);
            if (resourceOpt.isEmpty()) {
                return result;
            }

            Resource resource = resourceOpt.get();
            try (InputStream is = resource.getInputStream();
                 InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {

                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                if (!root.has("display")) {
                    return result;
                }

                JsonObject display = root.getAsJsonObject("display");

                // Try firstperson_righthand first, fall back to firstperson_lefthand
                JsonObject fpDisplay = null;
                if (display.has("firstperson_righthand")) {
                    fpDisplay = display.getAsJsonObject("firstperson_righthand");
                } else if (display.has("firstperson_lefthand")) {
                    fpDisplay = display.getAsJsonObject("firstperson_lefthand");
                }

                if (fpDisplay != null) {
                    if (fpDisplay.has("scale")) {
                        JsonArray scale = fpDisplay.getAsJsonArray("scale");
                        result.scaleX = scale.get(0).getAsFloat();
                        result.scaleY = scale.get(1).getAsFloat();
                        result.scaleZ = scale.get(2).getAsFloat();
                    }
                    if (fpDisplay.has("translation")) {
                        JsonArray trans = fpDisplay.getAsJsonArray("translation");
                        result.transX = trans.get(0).getAsFloat();
                        result.transY = trans.get(1).getAsFloat();
                        result.transZ = trans.get(2).getAsFloat();
                    }
                    if (fpDisplay.has("rotation")) {
                        JsonArray rot = fpDisplay.getAsJsonArray("rotation");
                        result.rotX = rot.get(0).getAsFloat();
                        result.rotY = rot.get(1).getAsFloat();
                        result.rotZ = rot.get(2).getAsFloat();
                    }
                    result.fromResourcePack = true;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("[TotemResize] Failed to load resource pack totem model: {}", e.getMessage());
        }

        return result;
    }
}
