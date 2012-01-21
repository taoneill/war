package com.tommytony.war.job;

import org.bukkit.entity.Player;

import com.tommytony.war.Warzone;

public class InitZoneJob implements Runnable {

	private final Warzone zone;
	private final Player respawnExempted;

	public InitZoneJob(Warzone zone) {
		this.zone = zone;
		this.respawnExempted = null;
	}

	public InitZoneJob(Warzone warzone, Player respawnExempted) {
		this.zone = warzone;
		this.respawnExempted = respawnExempted;
	}

	public void run() {
		this.zone.initializeZone(this.respawnExempted);
	}

}
