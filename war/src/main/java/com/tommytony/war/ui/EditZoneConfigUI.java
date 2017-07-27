package com.tommytony.war.ui;

import com.google.common.collect.ImmutableList;
import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import com.tommytony.war.config.TeamConfig;
import com.tommytony.war.config.TeamConfigBag;
import com.tommytony.war.config.WarzoneConfig;
import com.tommytony.war.config.WarzoneConfigBag;
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
public class EditZoneConfigUI extends ChestUI {
	private final Warzone zone;

	public EditZoneConfigUI(Warzone zone) {
		super();
		this.zone = zone;
	}

	@Override
	public void build(final Player player, Inventory inv) {
		ItemStack item;
		ItemMeta meta;
		int i = 0;

		for (final WarzoneConfig option : WarzoneConfig.values()) {
			if (option.getTitle() == null) {
				continue;
			}
			if (option.getConfigType() == Boolean.class) {
				item = new Wool(zone.getWarzoneConfig().getBoolean(option) ? DyeColor.LIME : DyeColor.RED).toItemStack(1);
				meta = item.getItemMeta();
				meta.setDisplayName(option.getTitle());
				meta.setLore(ImmutableList.of(option.getDescription()));
				item.setItemMeta(meta);
				this.addItem(inv, i++, item, new Runnable() {
					@Override
					public void run() {
						zone.getWarzoneConfig().put(option, !zone.getWarzoneConfig().getBoolean(option));
						WarzoneConfigBag.afterUpdate(zone, player, option.name() + " set to " + zone.getWarzoneConfig().getBoolean(option), false);
						War.war.getUIManager().assignUI(player, new EditZoneConfigUI(zone));
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
						player.sendTitle(option.getTitle(), zone.getWarzoneConfig().getValue(option).toString(), 10, 70, 20);
						War.war.getUIManager().assignUI(player, new EditZoneConfigUI(zone));
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
				War.war.getUIManager().assignUI(player, new EditZoneConfigUI(zone));
			}
		});
		for (final TeamConfig option : TeamConfig.values()) {
			if (option.getTitle() == null) {
				continue;
			}
			if (option.getConfigType() == Boolean.class) {
				item = new Wool(zone.getTeamDefaultConfig().resolveBoolean(option) ? DyeColor.LIME : DyeColor.RED).toItemStack(1);
				meta = item.getItemMeta();
				meta.setDisplayName(option.getTitle());
				meta.setLore(ImmutableList.of(option.getDescription()));
				item.setItemMeta(meta);
				this.addItem(inv, i++, item, new Runnable() {
					@Override
					public void run() {
						zone.getTeamDefaultConfig().put(option, !zone.getTeamDefaultConfig().resolveBoolean(option));
						WarzoneConfigBag.afterUpdate(zone, player, option.name() + " set to " + zone.getTeamDefaultConfig().resolveBoolean(option), false);
						War.war.getUIManager().assignUI(player, new EditZoneConfigUI(zone));
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
						player.sendTitle(option.getTitle(), zone.getTeamDefaultConfig().resolveValue(option).toString(), 10, 70, 20);
						War.war.getUIManager().assignUI(player, new EditZoneConfigUI(zone));
					}
				});
			}
		}
	}

	@Override
	public String getTitle() {
		return ChatColor.RED + "Warzone \"" + zone.getName() + "\": Config";
	}

	@Override
	public int getSize() {
		return 9*6;
	}
}
