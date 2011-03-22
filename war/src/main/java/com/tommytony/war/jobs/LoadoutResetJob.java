package com.tommytony.war.jobs;

import org.bukkit.entity.Player;

import com.tommytony.war.Team;
import com.tommytony.war.Warzone;

public class LoadoutResetJob implements Runnable {

	private final Player player;
	private final Warzone zone;
	private final Team team;

	public LoadoutResetJob(Warzone zone, Team team, Player player) {
		this.zone = zone;
		this.team = team;
		this.player = player;
	}

	public void run() {
		zone.resetInventory(team, player);
	}

}
