package com.tommytony.war.jobs;

import org.bukkit.entity.Player;

import com.tommytony.war.Warzone;

public class InitZoneJob implements Runnable {

	private final Warzone zone;
	private final Player respawnExempted;

	public InitZoneJob(Warzone zone) {
		this.zone = zone;
		respawnExempted = null;
	}
	
	public InitZoneJob(Warzone warzone, Player respawnExempted) {
		zone = warzone;
		this.respawnExempted = respawnExempted;
		// TODO Auto-generated constructor stub
	}

	public void run() {
		zone.initializeZone(respawnExempted);
	}

}
