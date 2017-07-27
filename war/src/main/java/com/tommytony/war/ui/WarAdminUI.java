package com.tommytony.war.ui;

import com.google.common.collect.ImmutableList;
import com.tommytony.war.War;
import com.tommytony.war.config.TeamConfig;
import com.tommytony.war.config.WarConfig;
import com.tommytony.war.config.WarConfigBag;
import com.tommytony.war.config.WarzoneConfig;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.Wool;

/**
 * Created by Connor on 7/27/2017.
 */
public class WarAdminUI extends ChestUI {
	@Override
	public void build(final Player player, Inventory inv) {
		ItemStack item;
		ItemMeta meta;
		int i = 0;

		for (final WarConfig option : WarConfig.values()) {
			if (option.getTitle() == null) {
				continue;
			}
			if (option.getConfigType() == Boolean.class) {
				item = new Wool(War.war.getWarConfig().getBoolean(option) ? DyeColor.LIME : DyeColor.RED).toItemStack(1);
				meta = item.getItemMeta();
				meta.setDisplayName(option.getTitle());
				meta.setLore(ImmutableList.of(option.getDescription()));
				item.setItemMeta(meta);
				this.addItem(inv, i++, item, new Runnable() {
					@Override
					public void run() {
						War.war.getWarConfig().put(option, !War.war.getWarConfig().getBoolean(option));
						WarConfigBag.afterUpdate(player, option.name() + " set to " + War.war.getWarConfig().getBoolean(option), false);
						War.war.getUIManager().assignUI(player, new WarAdminUI());
					}
				});
			} else {
				item = new ItemStack(Material.COMPASS, 1);
				meta = item.getItemMeta();
				meta.setDisplayName(option.getTitle());
				meta.setLore(ImmutableList.of(option.getDescription()));
				item.setItemMeta(meta);
				this.addItem(inv, i++, item, new Runnable() {
					@Override
					public void run() {
						player.sendTitle(option.getTitle(), War.war.getWarConfig().getValue(option).toString(), 10, 70, 20);
						War.war.getUIManager().assignUI(player, new WarAdminUI());
					}
				});
			}
		}
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
		for (final WarzoneConfig option : WarzoneConfig.values()) {
			if (option.getTitle() == null) {
				continue;
			}
			if (option.getConfigType() == Boolean.class) {
				item = new Wool(War.war.getWarzoneDefaultConfig().getBoolean(option) ? DyeColor.LIME : DyeColor.RED).toItemStack(1);
				meta = item.getItemMeta();
				meta.setDisplayName(option.getTitle());
				meta.setLore(ImmutableList.of(option.getDescription()));
				item.setItemMeta(meta);
				this.addItem(inv, i++, item, new Runnable() {
					@Override
					public void run() {
						War.war.getWarzoneDefaultConfig().put(option, !War.war.getWarzoneDefaultConfig().getBoolean(option));
						WarConfigBag.afterUpdate(player, option.name() + " set to " + War.war.getWarzoneDefaultConfig().getBoolean(option), false);
						War.war.getUIManager().assignUI(player, new WarAdminUI());
					}
				});
			} else {
				item = new ItemStack(Material.COMPASS, 1);
				meta = item.getItemMeta();
				meta.setDisplayName(option.getTitle());
				meta.setLore(ImmutableList.of(option.getDescription()));
				item.setItemMeta(meta);
				this.addItem(inv, i++, item, new Runnable() {
					@Override
					public void run() {
						player.sendTitle(option.getTitle(), War.war.getWarzoneDefaultConfig().getValue(option).toString(), 10, 70, 20);
						War.war.getUIManager().assignUI(player, new WarAdminUI());
					}
				});
			}
		}
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
		for (final TeamConfig option : TeamConfig.values()) {
			if (option.getTitle() == null) {
				continue;
			}
			if (option.getConfigType() == Boolean.class) {
				item = new Wool(War.war.getTeamDefaultConfig().resolveBoolean(option) ? DyeColor.LIME : DyeColor.RED).toItemStack(1);
				meta = item.getItemMeta();
				meta.setDisplayName(option.getTitle());
				meta.setLore(ImmutableList.of(option.getDescription()));
				item.setItemMeta(meta);
				this.addItem(inv, i++, item, new Runnable() {
					@Override
					public void run() {
						War.war.getTeamDefaultConfig().put(option, !War.war.getTeamDefaultConfig().resolveBoolean(option));
						WarConfigBag.afterUpdate(player, option.name() + " set to " + War.war.getTeamDefaultConfig().resolveBoolean(option), false);
						War.war.getUIManager().assignUI(player, new WarAdminUI());
					}
				});
			} else {
				item = new ItemStack(Material.COMPASS, 1);
				meta = item.getItemMeta();
				meta.setDisplayName(option.getTitle());
				meta.setLore(ImmutableList.of(option.getDescription()));
				item.setItemMeta(meta);
				this.addItem(inv, i++, item, new Runnable() {
					@Override
					public void run() {
						player.sendTitle(option.getTitle(), War.war.getTeamDefaultConfig().resolveValue(option).toString(), 10, 70, 20);
						War.war.getUIManager().assignUI(player, new WarAdminUI());
					}
				});
			}
		}
	}

	@Override
	public String getTitle() {
		return ChatColor.DARK_RED + "" + ChatColor.BOLD + "War Admin";
	}

	@Override
	public int getSize() {
		return 9*9;
	}
}
