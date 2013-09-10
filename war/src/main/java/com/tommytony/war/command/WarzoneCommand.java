package com.tommytony.war.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import org.bukkit.Bukkit;

/**
 * Warps the player to the given warzone.
 *
 * @author Tim Düsterhus
 */
public class WarzoneCommand extends AbstractWarCommand {
	public WarzoneCommand(WarCommandHandler handler, CommandSender sender, String[] args) {
		super(handler, sender, args);
	}

	@Override
	public boolean handle() {
		if (!(this.getSender() instanceof Player)) {
			this.badMsg("You can't do this if you are not in-game.");
			return true;
		}
		Player player = (Player) this.getSender();
		if (args.length == 1) {
			if (War.war.canWarp(player)) {
				Warzone warzone = Warzone.getZoneByName(args[0]);
				if (warzone != null && warzone.getTeleport() != null) {
					Warzone playerWarzone = Warzone.getZoneByPlayerName(player.getName());
					if (playerWarzone != null) {
						playerWarzone.handlePlayerLeave(player, warzone.getTeleport(), true);
					} else {
						player.teleport(warzone.getTeleport());
					}
				} else {
					this.badMsg("Warzone " + args[0] + " could not be found.");
				}
			} else {
				this.badMsg("You do not have permission to teleport to the warzone.");
			}
			return true;
		} else if (args.length == 2 && (args[1].equalsIgnoreCase("sb")
				|| args[1].equalsIgnoreCase("score")
				|| args[1].equalsIgnoreCase("scoreboard"))) {
			Warzone warzone = Warzone.getZoneByName(args[0]);
			if (warzone != null) {
				if (warzone.getScoreboard() != null) {
					if (warzone.getScoreboard() == player.getScoreboard()) {
						player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
					} else {
						player.setScoreboard(warzone.getScoreboard());
					}
				} else {
					this.badMsg("Warzone " + args[0] + " has not enabled a scoreboard.");
				}
			} else {
				this.badMsg("Warzone " + args[0] + " could not be found.");
			}
			return true;
		} else {
			return false;
		}
	}
}
