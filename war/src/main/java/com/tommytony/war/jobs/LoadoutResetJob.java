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
		this.zone.resetInventory(this.team, this.player);
		// Stop fire here, since doing it in the same tick as death doesn't extinguish it
		player.setFireTicks(0);
	}

}
