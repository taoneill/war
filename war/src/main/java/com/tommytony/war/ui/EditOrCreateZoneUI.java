package com.tommytony.war.ui;

import com.google.common.collect.ImmutableList;
import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import com.tommytony.war.command.ZoneSetter;
import com.tommytony.war.utility.Compat;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Created by Connor on 7/27/2017.
 */
public class EditOrCreateZoneUI extends ChestUI {
	@Override
	public void build(final Player player, Inventory inv) {
		int i = 0;
		ItemStack item = new ItemStack(Material.WOODEN_AXE, 1);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(ChatColor.BOLD + "" + ChatColor.YELLOW + "Create Warzone");
		meta.setLore(ImmutableList.of(ChatColor.GRAY + "Click to create a " + ChatColor.AQUA + "Warzone"));
		item.setItemMeta(meta);
		this.addItem(inv, i++, item, new Runnable() {
			@Override
			public void run() {
				if (!War.war.getServer().getPluginManager().isPluginEnabled("WorldEdit")) {
					player.sendTitle("", ChatColor.RED + "This feature requires WorldEdit.", 10, 20, 10);
					return;
				}
				player.getInventory().addItem(new ItemStack(Material.WOODEN_AXE, 1));
				War.war.getUIManager().getPlayerMessage(player, "Select region for zone using WorldEdit and then type a name:", new StringRunnable() {
					@Override
					public void run() {
						Compat.BlockPair pair = Compat.getWorldEditSelection(player);
						if (pair != null) {
							ZoneSetter setter = new ZoneSetter(player, this.getValue());
							setter.placeCorner1(pair.getBlock1());
							setter.placeCorner2(pair.getBlock2());
						} else {
							War.war.badMsg(player, "Invalid selection. Creation cancelled.");
						}
					}
				});
			}
		});
		for (final Warzone zone : War.war.getEnabledWarzones()) {
			if (!War.war.isWarAdmin(player) && !zone.isAuthor(player)) {
				continue;
			}
			item = new ItemStack(Material.WRITABLE_BOOK);
			meta = item.getItemMeta();
			meta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + zone.getName());
			meta.setLore(ImmutableList.of(ChatColor.GRAY + "Click to edit"));
			item.setItemMeta(meta);
			this.addItem(inv, i++, item, new Runnable() {
				@Override
				public void run() {
					War.war.getUIManager().assignUI(player, new EditZoneUI(zone));
				}
			});
		}
	}

	@Override
	public String getTitle() {
		return ChatColor.RED + "Edit or Create Zones";
	}

	@Override
	public int getSize() {
		int zones = War.war.getEnabledWarzones().size() + 1;
		if (zones % 9 == 0) {
			return zones / 9;
		} else {
			return zones / 9 + 9;
		}
	}
}
