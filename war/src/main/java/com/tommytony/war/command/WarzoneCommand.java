package com.tommytony.war.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


import com.tommytony.war.War;
import com.tommytony.war.WarCommandHandler;
import com.tommytony.war.game.Warzone;

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

		if (this.args.length != 1) {
			return false;
		}
		Player player = (Player) this.getSender();

		if (!War.war.canWarp(player)) {
			this.badMsg("Can't warp to zone. You need the 'war.warp' permission.");
		} else {
			Warzone warzone = Warzone.getZoneByName(this.args[0]);
			if (warzone != null && warzone.getTeleport() != null) {
				Warzone playerWarzone = Warzone.getZoneByPlayerName(player.getName());
				if (playerWarzone != null) {
					playerWarzone.handlePlayerLeave(player, warzone.getTeleport(), true);
				} else {
					player.teleport(warzone.getTeleport());
				}
				return true;
			}

			this.badMsg("No such warzone.");
		}
		return true;
	}
}
