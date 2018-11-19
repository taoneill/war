package com.tommytony.war.utility;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Simple fixes to account for removed Bukkit functionality
 */
public class Compat {
    public static ItemStack createDamagedIS(Material mat, int amount, int damage) {
        ItemStack is = new ItemStack(mat, amount);
        ItemMeta meta = is.getItemMeta();
        ((Damageable) meta).setDamage(damage); // hope this works
        is.setItemMeta(meta);
        return is;
    }
}
