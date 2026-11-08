package com.rutaji.qwent3.movout;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public class inventory {

    /** Puts items in the player's inventory; what doesn't fit is dropped at their feet. */
    public static void giveBack(Player player, List<ItemStack> stacks) {
        if (stacks.isEmpty()) {
            return;
        }
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(stacks.toArray(new ItemStack[0]));
        leftover.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
    }

}
