package com.tommytony.war.command;

import com.tommytony.war.config.WarConfig;
import com.tommytony.war.job.TeleportPlayerJob;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


import com.tommytony.war.War;
import com.tommytony.war.Warzone;

/**
 * Warps the player to the warhub.
 *
 * @author Tim Düsterhus
 */
public class WarhubCommand extends AbstractWarCommand {
	public WarhubCommand(WarCommandHandler handler, CommandSender sender, String[] args) {
		super(handler, sender, args);
	}

	@Override
	public boolean handle() {
		if (!(this.getSender() instanceof Player)) {
			this.badMsg("command.console");
			return true;
		}
		if (this.args.length != 0) {
			return false;
		}
		Player player = (Player) this.getSender();
		if (War.war.getWarHub() == null) {
			this.badMsg("warhub.none");
		} else if (!War.war.canWarp(player)) {
			this.badMsg("warhub.permission");
		} else {
			Warzone playerWarzone = Warzone.getZoneByPlayerName(player.getName());
			if (playerWarzone != null) { // was in zone
				playerWarzone.handlePlayerLeave(player, War.war.getWarHub().getLocation(), true);
			}
			int warmup = War.war.getWarConfig().getInt(WarConfig.TPWARMUP);
			if (warmup > 0 && !player.hasPermission("war.warmupexempt")) {
				final int TICKS_PER_SECOND = 20;
				TeleportPlayerJob job = new TeleportPlayerJob(player, War.war.getWarHub().getLocation());
				job.runTaskLater(War.war, warmup);
				this.msg("command.tp.init", warmup / TICKS_PER_SECOND);
			} else {
				player.teleport(War.war.getWarHub().getLocation());
			}
		}
		return true;
	}
}
