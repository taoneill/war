package com.tommytony.war.jobs;

import org.bukkit.entity.Player;

import com.tommytony.war.Team;
import com.tommytony.war.Warzone;

public class RespawnPlayerJob implements Runnable {

	private final Team team;
	private final Player player;
	private final Warzone zone;

	public RespawnPlayerJob(Warzone zone, Team playerTeam, Player player) {
		this.zone = zone;
		// TODO Auto-generated constructor stub
		this.team = playerTeam;
		this.player = player;
	}

	public void run() {
		this.zone.respawnPlayer(this.team, this.player);
	}

}
