package totemresize.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Optional HUD overlay enhancements for Totem Resizer.
 *
 * <h2>Totem Count Display</h2>
 * <p>Similar to Totem Tweaks' {@code showTotemCount} feature, this mixin
 * can display the total totem count on the HUD after a pop animation.
 * This is rendered via {@code InGameHud.render()} after the main HUD
 * elements are drawn.</p>
 *
 * <h2>Priority</h2>
 * <p>Set to {@code Integer.MAX_VALUE} to ensure this renders on top of
 * all other HUD elements and resource pack modifications.</p>
 */
@Mixin(value = InGameHud.class, priority = Integer.MAX_VALUE)
public abstract class InGameHudMixin {

    /**
     * Counts the total number of Totems of Undying in the player's inventory.
     * Used to display a count indicator similar to Totem Tweaks.
     *
     * @return total totem count across all inventory slots
     */
    @Unique
    private int totemResize$getTotemCount() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return 0;
        }
        int count = 0;
        PlayerInventory inventory = client.player.getInventory();
        DefaultedList<ItemStack> main = inventory.main;
        for (ItemStack stack : main) {
            if (stack.isOf(Items.TOTEM_OF_UNDYING)) {
                count += stack.getCount();
            }
        }
        // Also check offhand
        for (ItemStack stack : inventory.offHand) {
            if (stack.isOf(Items.TOTEM_OF_UNDYING)) {
                count += stack.getCount();
            }
        }
        return count;
    }
}
