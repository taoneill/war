package com.tommytony.war.job;

import org.bukkit.entity.Player;

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
	}

}
