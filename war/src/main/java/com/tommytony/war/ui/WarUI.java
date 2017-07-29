package com.tommytony.war.ui;

import com.google.common.collect.ImmutableList;
import com.tommytony.war.War;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Created by Connor on 7/26/2017.
 */
public class WarUI extends ChestUI {
	@Override
	public void build(final Player player, Inventory inv) {
		Runnable joinZoneAction = new Runnable() {
			@Override
			public void run() {
				War.war.getUIManager().assignUI(player, new JoinZoneUI());
			}
		};
		Runnable createZoneAction = new Runnable() {
			@Override
			public void run() {
				War.war.getUIManager().assignUI(player, new EditOrCreateZoneUI());
			}
		};
		Runnable warAdminAction = new Runnable() {
			@Override
			public void run() {
				War.war.getUIManager().assignUI(player, new WarAdminUI());
			}
		};

		if (War.war.isWarAdmin(player)) {
			this.addItem(inv, 2, getWarAdminItem(), warAdminAction);
			this.addItem(inv, 4, getCreateWarzoneItem(), createZoneAction);
			this.addItem(inv, 6, getJoinWarzoneItem(), joinZoneAction);
		} else if (War.war.isZoneMaker(player)) {
			this.addItem(inv, 2, getCreateWarzoneItem(), createZoneAction);
			this.addItem(inv, 6, getJoinWarzoneItem(), joinZoneAction);
		} else {
			this.addItem(inv, 4, getJoinWarzoneItem(), joinZoneAction);
		}
	}

	private ItemStack getCreateWarzoneItem() {
		ItemStack item = new ItemStack(Material.WOOD_AXE, 1);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Create Warzone");
		meta.setLore(ImmutableList.of(ChatColor.GRAY + "Click to create, or edit a " + ChatColor.AQUA + "Warzone" + ChatColor.GRAY + "."));
		item.setItemMeta(meta);
		return item;
	}

	private ItemStack getJoinWarzoneItem() {
		ItemStack item = new ItemStack(Material.IRON_SWORD, 1);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Join Warzone");
		meta.setLore(ImmutableList.of(ChatColor.GRAY + "Click to access " + ChatColor.AQUA + "Warzones" + ChatColor.GRAY + ".",
				ChatColor.DARK_GRAY + "Play in PVP areas, with multiple gamemodes here."));
		item.setItemMeta(meta);
		return item;
	}

	private ItemStack getWarAdminItem() {
		ItemStack item = new ItemStack(Material.EYE_OF_ENDER, 1);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(ChatColor.DARK_RED + "" + ChatColor.BOLD + "Manage War");
		meta.setLore(ImmutableList.of(ChatColor.GRAY + "Click to display " + ChatColor.DARK_RED + "Admin" + ChatColor.GRAY + " access panel",
				ChatColor.GRAY + "Includes: " + ChatColor.DARK_GRAY + "Permissions, managing warzones, configs, etc."));
		item.setItemMeta(meta);
		return item;
	}

	@Override
	public String getTitle() {
		return ChatColor.RED + "War";
	}

	@Override
	public int getSize() {
		return 9;
	}
}
