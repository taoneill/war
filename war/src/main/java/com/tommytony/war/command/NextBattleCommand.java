package com.tommytony.war.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.tommytony.war.Team;
import com.tommytony.war.Warzone;
import com.tommytony.war.structure.ZoneLobby;


public class NextBattleCommand extends AbstractZoneMakerCommand {
	public NextBattleCommand(WarCommandHandler handler, CommandSender sender, String[] args) throws NotZoneMakerException {
		super(handler, sender, args);
	}

	@Override
	public boolean handle() {
		Warzone zone;
		if (this.args.length == 1) {
			zone = Warzone.getZoneByName(this.args[0]);
		} else if (this.args.length == 0) {
			if (!(this.getSender() instanceof Player)) {
				return false;
			}
			zone = Warzone.getZoneByLocation((Player) this.getSender());
			if (zone == null) {
				ZoneLobby lobby = ZoneLobby.getLobbyByLocation((Player) this.getSender());
				if (lobby == null) {
					return false;
				}
				zone = lobby.getZone();
			}
		} else {
			return false;
		}
		
		if (zone == null) {
			return false;
		}

		zone.clearThieves();
		for (Team team : zone.getTeams()) {
			team.teamcast("The battle was interrupted. " + zone.getTeamInformation() + " Resetting warzone " + zone.getName() + " and life pools...");
		}
		zone.getVolume().resetBlocksAsJob();
		zone.initializeZoneAsJob();

		return true;
	}
}
