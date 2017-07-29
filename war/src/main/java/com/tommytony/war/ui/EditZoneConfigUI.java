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

		i = UIConfigHelper.addWarzoneConfigOptions(this, player, inv, zone.getWarzoneConfig(), zone, i);
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
		UIConfigHelper.addTeamConfigOptions(this, player, inv, zone.getTeamDefaultConfig(), null, zone, i);
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
