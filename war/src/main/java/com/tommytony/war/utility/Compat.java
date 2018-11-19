package com.tommytony.war.utility;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.tommytony.war.War;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
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

    public static class BlockPair {
        final Block block1;
        final Block block2;

        BlockPair(Block block1, Block block2) {
            this.block1 = block1;
            this.block2 = block2;
        }

        public Block getBlock1() {
            return block1;
        }

        public Block getBlock2() {
            return block2;
        }
    }

    public static BlockPair getWorldEditSelection(Player player) {
        if (!War.war.getServer().getPluginManager().isPluginEnabled("WorldEdit")) {
            return null;
        }
        BukkitPlayer wp = BukkitAdapter.adapt(player);
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(wp);
        try {
            Region selection = session.getSelection(wp.getWorld());
            if (selection instanceof CuboidRegion) {
                return new BlockPair(
                        player.getWorld().getBlockAt(selection.getMinimumPoint().getBlockX(), selection.getMinimumPoint().getBlockY(), selection.getMinimumPoint().getBlockZ()),
                        player.getWorld().getBlockAt(selection.getMaximumPoint().getBlockX(), selection.getMaximumPoint().getBlockY(), selection.getMaximumPoint().getBlockZ())
                );
            }
            return null;
        } catch (IncompleteRegionException e) {
            return null;
        }

    }

}
