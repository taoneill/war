package com.tommytony.war.jobs;

import org.bukkit.entity.Player;

import com.tommytony.war.Warzone;

public class RestoreDeadmanInventoryJob implements Runnable {

	private final Player player;
	private final Warzone zone;

	public RestoreDeadmanInventoryJob(Player player, Warzone zone) {
		this.player = player;
		this.zone = zone;
	}

	public void run() {
		this.zone.restoreDeadmanInventory(this.player);
		this.player.teleport(this.zone.getTeleport());
	}
}
