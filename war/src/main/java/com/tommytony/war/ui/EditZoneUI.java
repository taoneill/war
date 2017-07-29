package com.tommytony.war.ui;

import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import com.tommytony.war.command.DeleteZoneCommand;
import com.tommytony.war.command.ResetZoneCommand;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Created by Connor on 7/27/2017.
 */
class EditZoneUI extends ChestUI {
	private final Warzone zone;

	EditZoneUI(Warzone zone) {
		super();
		this.zone = zone;
	}

	@Override
	public void build(final Player player, Inventory inv) {
		ItemStack item;
		ItemMeta meta;
		item = new ItemStack(Material.CHEST);
		meta = item.getItemMeta();
		meta.setDisplayName(ChatColor.YELLOW + "Options");
		item.setItemMeta(meta);
		this.addItem(inv, 0, item, new Runnable() {
			@Override
			public void run() {
				War.war.getUIManager().assignUI(player, new EditZoneConfigUI(zone));
			}
		});
		item = new ItemStack(Material.CHEST);
		meta = item.getItemMeta();
		meta.setDisplayName(ChatColor.YELLOW + "Teams");
		item.setItemMeta(meta);
		this.addItem(inv, 1, item, new Runnable() {
			@Override
			public void run() {
				War.war.getUIManager().assignUI(player, new EditTeamsListUI(zone));
			}
		});
		item = new ItemStack(Material.CHEST);
		meta = item.getItemMeta();
		meta.setDisplayName(ChatColor.YELLOW + "Loadouts");
		item.setItemMeta(meta);
		this.addItem(inv, 2, item, new Runnable() {
			@Override
			public void run() {
				War.war.getUIManager().assignUI(player, new EditLoadoutListUI(zone));
			}
		});
		item = new ItemStack(Material.CHEST);
		meta = item.getItemMeta();
		meta.setDisplayName(ChatColor.YELLOW + "Structures");
		item.setItemMeta(meta);
		item = new ItemStack(Material.NETHER_STAR);
		meta = item.getItemMeta();
		meta.setDisplayName(ChatColor.GRAY + "Reset Blocks");
		item.setItemMeta(meta);
		this.addItem(inv, 7, item, new Runnable() {
			@Override
			public void run() {
				ResetZoneCommand.forceResetZone(zone, player);
			}
		});
		item = new ItemStack(Material.TNT);
		meta = item.getItemMeta();
		meta.setDisplayName(ChatColor.DARK_RED + "" + ChatColor.BOLD + "Delete");
		item.setItemMeta(meta);
		this.addItem(inv, 8, item, new Runnable() {
			@Override
			public void run() {
				War.war.getUIManager().getPlayerMessage(player, "Delete zone: are you sure? Type \"" + zone.getName() + "\" to confirm:", new StringRunnable() {
					@Override
					public void run() {
						if (this.getValue().equalsIgnoreCase(zone.getName())) {
							DeleteZoneCommand.forceDeleteZone(zone, player);
						} else {
							War.war.badMsg(player, "Delete aborted.");
						}
					}
				});
			}
		});
	}

	@Override
	public String getTitle() {
		return ChatColor.RED + "Editing Warzone \"" + zone.getName() + "\"";
	}

	@Override
	public int getSize() {
		return 9;
	}
}
