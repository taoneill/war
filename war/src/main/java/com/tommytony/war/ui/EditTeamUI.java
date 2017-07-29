package com.tommytony.war.ui;

import com.tommytony.war.Team;
import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import com.tommytony.war.config.TeamConfigBag;
import com.tommytony.war.mapper.WarzoneYmlMapper;
import com.tommytony.war.volume.Volume;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Created by Connor on 7/27/2017.
 */
class EditTeamUI extends ChestUI {
	private final Team team;

	EditTeamUI(Team team) {
		super();
		this.team = team;
	}

	@Override
	public void build(final Player player, Inventory inv) {
		ItemStack item;
		ItemMeta meta;
		int i = 0;
		item = new ItemStack(Material.GOLD_SPADE, 1);
		meta = item.getItemMeta();
		meta.setDisplayName(ChatColor.GREEN + "Add additional spawn");
		item.setItemMeta(meta);
		this.addItem(inv, i++, item, new Runnable() {
			@Override
			public void run() {
				if (team.getZone().getVolume().contains(player.getLocation())) {
					team.addTeamSpawn(player.getLocation());
					player.sendTitle("", "Additional spawn added", 10, 20, 10);
				} else {
					player.sendTitle("", ChatColor.RED + "Can't add a spawn outside of the zone!", 10, 20, 10);
				}
			}
		});
		item = new ItemStack(Material.CHEST, 1);
		meta = item.getItemMeta();
		meta.setDisplayName(ChatColor.YELLOW + "Loadouts");
		item.setItemMeta(meta);
		this.addItem(inv, i++, item, new Runnable() {
			@Override
			public void run() {
				War.war.getUIManager().assignUI(player, new EditLoadoutListUI(team));
			}
		});
		item = new ItemStack(Material.TNT, 1);
		meta = item.getItemMeta();
		meta.setDisplayName(ChatColor.DARK_RED + "" + ChatColor.BOLD + "Delete");
		item.setItemMeta(meta);
		this.addItem(inv, getSize() - 1, item, new Runnable() {
			@Override
			public void run() {
				if (team.getFlagVolume() != null) {
					team.getFlagVolume().resetBlocks();
				}
				for (Volume spawnVolume : team.getSpawnVolumes().values()) {
					spawnVolume.resetBlocks();
				}
				final Warzone zone = team.getZone();
				zone.getTeams().remove(team);
				if (zone.getLobby() != null) {
					zone.getLobby().setLocation(zone.getTeleport());
					zone.getLobby().initialize();
				}
				WarzoneYmlMapper.save(zone);
				War.war.msg(player, "Team " + team.getName() + " removed.");
			}
		});
		item = new ItemStack(Material.SNOW_BALL);
		meta = item.getItemMeta();
		meta.setDisplayName(ChatColor.GRAY + "" + ChatColor.BOLD + "Restore Defaults");
		item.setItemMeta(meta);
		this.addItem(inv, getSize() - 2, item, new Runnable() {
			@Override
			public void run() {
				team.getTeamConfig().reset();
				TeamConfigBag.afterUpdate(team, player, "All options set to defaults in team " + team.getName() + " by " + player.getName(), false);
				War.war.getUIManager().assignUI(player, new EditTeamUI(team));
			}
		});
		final TeamConfigBag config = team.getTeamConfig();
		UIConfigHelper.addTeamConfigOptions(this, player, inv, config, team, team.getZone(), i);
	}

	@Override
	public String getTitle() {
		return ChatColor.RED + "Warzone \"" + team.getZone().getName() + "\": Team \"" + team.getName() + "\"";
	}

	@Override
	public int getSize() {
		return 9*3;
	}
}
