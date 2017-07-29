package com.tommytony.war.ui;

import com.google.common.collect.ImmutableList;
import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.MessageFormat;

/**
 * Created by Connor on 7/25/2017.
 */
public class JoinZoneUI extends ChestUI {

	@Override
	public void build(final Player player, Inventory inv) {
		ItemStack item = new ItemStack(Material.TNT, 1);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Warhub");
		meta.setLore(ImmutableList.of(ChatColor.GRAY + "Teleports you to the " + ChatColor.RED + "Warhub" + ChatColor.GRAY + " lobby",
				ChatColor.DARK_GRAY + "Warzone doors located here"));
		item.setItemMeta(meta);
		int i = 0;
		this.addItem(inv, i++, item, new Runnable() {
			@Override
			public void run() {
				player.teleport(War.war.getWarHub().getLocation());
			}
		});
		for (final Warzone zone : War.war.getEnabledWarzones()) {
			item = new ItemStack(Material.ENDER_PEARL);
			meta = item.getItemMeta();
			meta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + zone.getName());
			meta.setLore(ImmutableList.of(MessageFormat.format(ChatColor.GRAY + "{0}/{1} players", zone.getPlayerCount(), zone.getMaxPlayers()),
					MessageFormat.format(ChatColor.GRAY + "{0} teams", zone.getTeams().size()),
					ChatColor.DARK_GRAY + "Click to teleport"));
			item.setItemMeta(meta);
			this.addItem(inv, i++, item, new Runnable() {
				@Override
				public void run() {
					War.war.getUIManager().assignUI(player, new JoinTeamUI(zone));
				}
			});
		}
	}

	@Override
	public String getTitle() {
		return ChatColor.RED + "Pick a Warzone";
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
