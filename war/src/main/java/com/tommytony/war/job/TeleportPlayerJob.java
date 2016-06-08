package com.tommytony.war.job;

import com.tommytony.war.War;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class TeleportPlayerJob extends BukkitRunnable {
	private final Player player;
	private final Location location;
	private final Location originalLocation;
	private final double originalHealth;

	public TeleportPlayerJob(Player player, Location location) {
		this.player = player;
		this.location = location;
		this.originalLocation = player.getLocation().clone();
		this.originalHealth = player.getHealth();
	}

	@Override
	public void run() {
		if (!player.isOnline()) {
		} else if (hasPlayerMoved()) {
			War.war.badMsg(player, "command.tp.moved");
		} else if (hasPlayerTakenDamage()) {
			War.war.badMsg(player, "command.tp.damaged");
		} else {
			player.teleport(location, PlayerTeleportEvent.TeleportCause.COMMAND);
			War.war.msg(player, "command.tp.success");
		}
	}

	boolean hasPlayerMoved() {
		final double MAX_MOVE_TOLERANCE = 1.5;
		return distance3D(player.getLocation(), originalLocation) > MAX_MOVE_TOLERANCE;
	}

	boolean hasPlayerTakenDamage() {
		final double MAX_DAMAGE_TOLERANCE = 2;
		return Math.abs(originalHealth - player.getHealth()) > MAX_DAMAGE_TOLERANCE;
	}

	double distance3D(Location pos1, Location pos2) {
		double distX = pos2.getX() - pos1.getX();
		double distY = pos2.getY() - pos1.getY();
		double distZ = pos2.getZ() - pos1.getZ();
		return Math.sqrt(Math.pow(distX, 2) + Math.pow(distY, 2) + Math.pow(distZ, 2));
	}
}
