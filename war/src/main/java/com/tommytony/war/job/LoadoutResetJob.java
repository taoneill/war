package com.tommytony.war.job;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.tommytony.war.Team;
import com.tommytony.war.Warzone;

public class LoadoutResetJob implements Runnable {

	private final Player player;
	private final Warzone zone;
	private final Team team;
	private final boolean isFirstRespawn;
	private final boolean isToggle;

	public LoadoutResetJob(Warzone zone, Team team, Player player, boolean isFirstRespawn, boolean isToggle) {
		this.zone = zone;
		this.team = team;
		this.player = player;
		this.isFirstRespawn = isFirstRespawn;
		this.isToggle = isToggle;
	}
	
	public void run() {
		this.zone.equipPlayerLoadoutSelection(player, team, isFirstRespawn, isToggle);
		
		if(team.hasFiveKillStreak(player)) {
			player.getInventory().addItem(new ItemStack(Material.EGG));
		}
		if(team.hasSevenKillStreak(player)) {
			player.getInventory().addItem(new ItemStack(Material.RAW_BEEF));
		}
		
		// Stop fire here, since doing it in the same tick as death doesn't extinguish it
		this.player.setFireTicks(0);
	}

}
