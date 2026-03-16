package totemresize.util;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public final class TotemResizeUtil {
    private TotemResizeUtil() {
    }

    public static boolean isTotem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return stack.isOf(Items.TOTEM_OF_UNDYING);
    }
}
