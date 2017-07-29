package com.tommytony.war.ui;

import com.tommytony.war.War;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Created by Connor on 7/27/2017.
 */
public class WarAdminUI extends ChestUI {
	@Override
	public void build(final Player player, Inventory inv) {
		ItemStack item;
		ItemMeta meta;
		int i = 0;

		i = UIConfigHelper.addWarConfigOptions(this, player, inv, War.war.getWarConfig(), i);
		item = new ItemStack(Material.STAINED_GLASS_PANE);
		meta = item.getItemMeta();
		meta.setDisplayName(">>>> Warzone Default Config >>>>");
		item.setItemMeta(meta);
		this.addItem(inv, i++, item, new Runnable() {
			@Override
			public void run() {
				War.war.getUIManager().assignUI(player, new WarAdminUI());
			}
		});
		i = UIConfigHelper.addWarzoneConfigOptions(this, player, inv, War.war.getWarzoneDefaultConfig(), null, i);
		item = new ItemStack(Material.STAINED_GLASS_PANE);
		meta = item.getItemMeta();
		meta.setDisplayName(">>>> Team Default Config >>>>");
		item.setItemMeta(meta);
		this.addItem(inv, i++, item, new Runnable() {
			@Override
			public void run() {
				War.war.getUIManager().assignUI(player, new WarAdminUI());
			}
		});
		UIConfigHelper.addTeamConfigOptions(this, player, inv, War.war.getTeamDefaultConfig(), null, null, i);
	}

	@Override
	public String getTitle() {
		return ChatColor.DARK_RED + "" + ChatColor.BOLD + "War Admin Panel";
	}

	@Override
	public int getSize() {
		return 9 * 7;
	}
}
