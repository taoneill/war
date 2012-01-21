package com.tommytony.war.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


import com.tommytony.war.War;
import com.tommytony.war.WarCommandHandler;
import com.tommytony.war.game.Team;
import com.tommytony.war.game.Warzone;

/**
 * Lists all warzones
 *
 * @author Tim Düsterhus
 */
public class WarzonesCommand extends AbstractWarCommand {
	public WarzonesCommand(WarCommandHandler handler, CommandSender sender, String[] args) {
		super(handler, sender, args);
	}

	@Override
	public boolean handle() {
		if (this.args.length != 0) {
			return false;
		}
		String warzonesMessage = "Warzones: ";
		if (War.war.getWarzones().isEmpty()) {
			warzonesMessage += "none.";
		} else {
			for (Warzone warzone : War.war.getWarzones()) {
				warzonesMessage += warzone.getName() + " (" + warzone.getTeams().size() + " teams, ";
				int playerTotal = 0;
				for (Team team : warzone.getTeams()) {
					playerTotal += team.getPlayers().size();
				}
				warzonesMessage += playerTotal + " players) ";
			}
		}

		this.msg(warzonesMessage + ((this.getSender() instanceof Player) ? " Use /zone <zone-name> to teleport to a warzone." : ""));

		return true;
	}
}
