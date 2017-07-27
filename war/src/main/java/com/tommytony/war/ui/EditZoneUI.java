package com.tommytony.war.ui;

import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Created by Connor on 7/27/2017.
 */
public class EditZoneUI extends ChestUI {
	private final Warzone zone;

	public EditZoneUI(Warzone zone) {
		super();
		this.zone = zone;
	}

	@Override
	public void build(final Player player, Inventory inv) {
		ItemStack item;
		ItemMeta meta;
		item = new ItemStack(Material.CHEST);
		meta = item.getItemMeta();
		meta.setDisplayName("Options");
		item.setItemMeta(meta);
		this.addItem(inv, 0, item, new Runnable() {
			@Override
			public void run() {
				War.war.getUIManager().assignUI(player, new EditZoneConfigUI(zone));
			}
		});
		item = new ItemStack(Material.CHEST);
		meta = item.getItemMeta();
		meta.setDisplayName("Teams");
		item.setItemMeta(meta);
		this.addItem(inv, 1, item, new Runnable() {
			@Override
			public void run() {
				War.war.getUIManager().assignUI(player, new EditTeamsListUI(zone));
			}
		});
		item = new ItemStack(Material.CHEST);
		meta = item.getItemMeta();
		meta.setDisplayName("Loadouts");
		item.setItemMeta(meta);
		item = new ItemStack(Material.CHEST);
		meta = item.getItemMeta();
		meta.setDisplayName("Structures");
		item.setItemMeta(meta);
	}

	@Override
	public String getTitle() {
		return ChatColor.RED + "Warzone \"" + zone.getName() + "\"";
	}

	@Override
	public int getSize() {
		return 9;
	}
}
