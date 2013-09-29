package com.tommytony.war.command;

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
			player.teleport(War.war.getWarHub().getLocation());
		}
		return true;
	}
}
