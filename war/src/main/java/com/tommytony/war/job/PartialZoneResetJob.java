package com.tommytony.war.job;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import com.tommytony.war.structure.ZoneLobby;
import com.tommytony.war.volume.ZoneVolume;

public class PartialZoneResetJob extends BukkitRunnable implements Cloneable {
	
	private static CommandSender senderToNotify = null;
	
	private final Warzone zone;
	private final ZoneVolume volume;
	private final int speed;
	private final int total;
	private int completed = 0;
	private final long startTime = System.currentTimeMillis();
	private long messageCounter = System.currentTimeMillis();
	public static final long MESSAGE_INTERVAL = 7500;
	// Ticks between job runs
	public static final int JOB_INTERVAL = 1;

	/**
	 * Reset a warzone's blocks at a certain speed.
	 * 
	 * @param volume
	 *            Warzone to reset.
	 * @param speed
	 *            Blocks to modify per #INTERVAL.
	 */
	public PartialZoneResetJob(Warzone zone, int speed) {
		this.zone = zone;
		this.volume = zone.getVolume();
		this.speed = speed;
		this.total = volume.size();
	}

	@Override
	public void run() {
		try {
			volume.resetSection(completed, speed);
			completed += speed;
			if (completed < total) {
				if (System.currentTimeMillis() - messageCounter > MESSAGE_INTERVAL) {
					messageCounter = System.currentTimeMillis();
					int percent = (int) (((double) completed / (double) total) * 100);
					long seconds = (System.currentTimeMillis() - startTime) / 1000;
					String message = MessageFormat.format(
							War.war.getString("zone.battle.resetprogress"),
							percent, seconds);
					this.sendMessageToAllWarzonePlayers(message);
				}
				War.war.getServer().getScheduler()
						.runTaskLater(War.war, this.clone(), JOB_INTERVAL);
			} else {
				long seconds = (System.currentTimeMillis() - startTime) / 1000;
				String message = MessageFormat.format(
						War.war.getString("zone.battle.resetcomplete"), seconds);
				this.sendMessageToAllWarzonePlayers(message);
				PartialZoneResetJob.setSenderToNotify(null);	// stop notifying
				zone.initializeZone();
				War.war.getLogger().info(
						"Finished reset cycle for warzone " + volume.getName() + " (took " + seconds + " seconds)");
			}
		} catch (SQLException e) {
			War.war.getLogger().log(Level.WARNING,
					"Failed to load zone during reset loop", e);
		}
	}

	private void sendMessageToAllWarzonePlayers(String message) {
		for (Player player : War.war.getServer().getOnlinePlayers()) {
			ZoneLobby lobby = ZoneLobby.getLobbyByLocation(player);
			if (player != PartialZoneResetJob.senderToNotify
					&& (zone.getPlayers().contains(player)
						|| (lobby != null && lobby.getZone() == zone))) {
				War.war.msg(player, message);
			}
		}
		
		if (PartialZoneResetJob.senderToNotify != null) {
			War.war.msg(senderToNotify, message);
		}
	}

	@Override
	protected PartialZoneResetJob clone() {
		try {
			return (PartialZoneResetJob) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new Error(e);
		}
	}

	public static void setSenderToNotify(CommandSender sender) {
		PartialZoneResetJob.senderToNotify = sender;
	}
}
