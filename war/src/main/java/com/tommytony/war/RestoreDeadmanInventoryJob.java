package com.tommytony.war;

import org.bukkit.entity.Player;

public class RestoreDeadmanInventoryJob implements Runnable {

	private final Player player;
	private final Warzone zone;
	private boolean giveReward;

	public RestoreDeadmanInventoryJob(Player player, Warzone zone) {
		this.player = player;
		this.zone = zone;
	}

	public void run() {
		zone.restoreDeadmanInventory(player);
		player.teleportTo(zone.getTeleport());
	}
}
