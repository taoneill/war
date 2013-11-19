package com.tommytony.war.command;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


import com.tommytony.war.Team;

/**
 * Sends a message to all team-members
 *
 * @author Tim Düsterhus
 */
public class TeamCommand extends AbstractWarCommand {
	public TeamCommand(WarCommandHandler handler, CommandSender sender, String[] args) {
		super(handler, sender, args);
	}

	@Override
	public boolean handle() {
		if (!(this.getSender() instanceof Player)) {
			this.badMsg("command.console");
			return true;
		}

		Player player = (Player) this.getSender();
		Team playerTeam = Team.getTeamByPlayerName(player.getName());
		if (playerTeam == null) {
			return false;
		}

		if (this.args.length < 1) {
			if (playerTeam.isInTeamChat(player)) {
				playerTeam.removeTeamChatPlayer(player);
				this.msg("team.chat.disable");
			} else {
				playerTeam.addTeamChatPlayer(player);
				this.msg("team.chat.enable");
			}
			return true;
		}

		StringBuilder teamMessage = new StringBuilder();
		for (String part : this.args) {
			teamMessage.append(part).append(' ');
		}
		playerTeam.sendTeamChatMessage(player, teamMessage.toString());
		return true;
	}
}
