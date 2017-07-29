package com.tommytony.war.ui;

import com.tommytony.war.Team;
import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import com.tommytony.war.mapper.LoadoutYmlMapper;
import com.tommytony.war.utility.Loadout;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;

/**
 * Created by Connor on 7/29/2017.
 */
class EditLoadoutListUI extends ChestUI {
	private final Warzone zone;
	private final Team team;

	EditLoadoutListUI(Warzone zone) {
		super();
		this.zone = zone;
		this.team = null;
	}

	EditLoadoutListUI(Team team) {
		super();
		this.zone = null;
		this.team = team;
	}

	@Override
	public void build(final Player player, Inventory inv) {
		List<Loadout> loadouts;
		if (zone != null) {
			loadouts = zone.getDefaultInventories().resolveNewLoadouts();
		} else if (team != null) {
			loadouts = team.getInventories().resolveNewLoadouts();
		} else {
			throw new IllegalStateException();
		}
		ItemStack item;
		ItemMeta meta;
		int i = 0;
		item = new ItemStack(Material.GOLD_SPADE, 1);
		meta = item.getItemMeta();
		meta.setDisplayName(ChatColor.GREEN + "Create new loadout");
		item.setItemMeta(meta);
		this.addItem(inv, i++, item, new Runnable() {
			@Override
			public void run() {
				War.war.getUIManager().getPlayerMessage(player, "Type the name for the new loadout (or type cancel):", new StringRunnable() {
					@Override
					public void run() {
						if (this.getValue().equalsIgnoreCase("cancel")) {
							return;
						}
						Loadout ldt;
						if (zone != null) {
							zone.getDefaultInventories().setLoadout(this.getValue(), new HashMap<Integer, ItemStack>());
							ldt = zone.getDefaultInventories().getNewLoadout(this.getValue());
						} else {
							team.getInventories().setLoadout(this.getValue(), new HashMap<Integer, ItemStack>());
							ldt = team.getInventories().getNewLoadout(this.getValue());
						}
						War.war.getUIManager().assignUI(player, new EditLoadoutUI(ldt, zone, team));
					}
				});
			}
		});
		List<String> sortedNames = LoadoutYmlMapper.sortNames(Loadout.toLegacyFormat(loadouts));
		for (String loadoutName : sortedNames) {
			final Loadout ldt = Loadout.getLoadout(loadouts, loadoutName);
			if (ldt == null) {
				War.war.getLogger().warning("Failed to resolve loadout " + loadoutName);
				continue;
			}
			item = new ItemStack(Material.CHEST);
			meta = item.getItemMeta();
			meta.setDisplayName(ChatColor.YELLOW + loadoutName);
			item.setItemMeta(meta);
			this.addItem(inv, i++, item, new Runnable() {
				@Override
				public void run() {
					War.war.getUIManager().assignUI(player, new EditLoadoutUI(ldt, zone, team));
				}
			});
		}

	}

	@Override
	public String getTitle() {
		if (zone != null) {
			return ChatColor.RED + "Warzone \"" + zone.getName() + "\": Loadouts";
		} else if (team != null) {
			return ChatColor.BLUE + "Team \"" + team.getName() + "\": Loadouts";
		}
		return null;
	}

	@Override
	public int getSize() {
		int size = 0;
		if (zone != null) {
			size = zone.getDefaultInventories().getNewLoadouts().size() + 1;
		} else if (team != null) {
			size = team.getInventories().getNewLoadouts().size() + 1;
		}
		if (size % 9 == 0) {
			return size / 9;
		} else {
			return size / 9 + 9;
		}
	}
}
