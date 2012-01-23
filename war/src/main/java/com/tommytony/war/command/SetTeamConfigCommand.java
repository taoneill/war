package com.tommytony.war.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


import com.tommytony.war.Team;
import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import com.tommytony.war.config.TeamKind;
import com.tommytony.war.mapper.WarzoneYmlMapper;
import com.tommytony.war.structure.ZoneLobby;

public class SetTeamConfigCommand extends AbstractZoneMakerCommand {

	public SetTeamConfigCommand(WarCommandHandler handler, CommandSender sender, String[] args) throws NotZoneMakerException {
		super(handler, sender, args);
	}

	@Override
	public boolean handle() {
		Warzone zone = null;
		Player player = null;
		CommandSender commandSender = this.getSender();
		boolean isFirstParamWarzone = false;
		boolean wantsToPrint = false;
		
		Team team = null;

		if (this.args.length == 0) {
			return false;
		} else {
			if (!this.args[0].contains(":")) {
				// warzone name maybe in first place
				Warzone zoneByName = Warzone.getZoneByName(this.args[0]);
				if (zoneByName != null) {
					zone = zoneByName;
					isFirstParamWarzone = true;
				} else if (this.args[0].equals("-p") || this.args[0].equals("print")) {
					wantsToPrint = true;
				}
			}

			if (this.getSender() instanceof Player) {
				player = (Player) commandSender;

				if (zone == null) {
					// zone not found, is he standing in it?
					Warzone zoneByLoc = Warzone.getZoneByLocation(player);
					ZoneLobby lobbyByLoc = ZoneLobby.getLobbyByLocation(player);
					if (zoneByLoc == null && lobbyByLoc != null) {
						zoneByLoc = lobbyByLoc.getZone();
					}
					if (zoneByLoc != null) {
						zone = zoneByLoc;
					}
				}
				
				team = Team.getTeamByPlayerName(player.getName());
			}

			if (zone == null) {
				// No warzone found, whatever the mean, escape
				return false;
			} else if (!this.isSenderAuthorOfZone(zone)) {
				return true;
			}

			if (isFirstParamWarzone) {
				if (this.args.length == 1) {
					// Only one param: the warzone name - pritn usage
					return false;
				}
				// More than one param: the arguments need to be shifted
				String[] newargs = new String[this.args.length - 1];
				for (int i = 1; i < this.args.length; i++) {
					newargs[i - 1] = this.args[i];
				}
				this.args = newargs;
			}

			// args have been shifted if needed
			if (this.args.length > 0) {
				TeamKind kind = TeamKind.teamKindFromString(this.args[0]);
				Team teamByName = zone.getTeamByKind(kind);
				
				if (team == null && teamByName == null) {
					// Team not found
					this.badMsg("No such team. Use /teams.");
					return true;
				} else if (this.args.length == 1 && teamByName != null) {
					// only team name, print config
					this.msg(War.war.printConfig(teamByName));
					return true;
				}
				
				if (teamByName != null) {
					// first param was team, shift again
					String[] newargs = new String[this.args.length - 1];
					for (int i = 1; i < this.args.length; i++) {
						newargs[i - 1] = this.args[i];
					}
					this.args = newargs;
				}
				
				if (teamByName != null) {
					// Named team > player's team
					team = teamByName;
				}
			} else {
				// No team param, show usage
				return false;
			}
			
			if (this.args.length > 0 && (this.args[0].equals("-p") || this.args[0].equals("print"))) {
				// only printing
				if (this.args.length == 1) {
					this.msg(War.war.printConfig(team));
					return true;
				} else {
					// first param was to print, shift again
					String[] newargs = new String[this.args.length - 1];
					for (int i = 1; i < this.args.length; i++) {
						newargs[i - 1] = this.args[i];
					}
					this.args = newargs;
				}
				wantsToPrint = true;
			}

			// We have a warzone, a team and indexed-from-0 arguments, let's update
			String namedParamReturn = War.war.updateTeamFromNamedParams(team, player, this.args);
			if (!namedParamReturn.equals("") && !namedParamReturn.equals("PARSE-ERROR")) {
				this.msg("Saving config and resetting warzone " + zone.getName() + ".");
				WarzoneYmlMapper.save(zone, false);
				zone.getVolume().resetBlocks();
				if (zone.getLobby() != null) {
					zone.getLobby().getVolume().resetBlocks();
				}
				zone.initializeZone(); // bring back team spawns etc

				if (wantsToPrint) {
					this.msg("Team config saved. Zone reset." + namedParamReturn + " " + War.war.printConfig(team));
				} else {
					this.msg("Team config saved. Zone reset." + namedParamReturn);
				}

				if (War.war.getWarHub() != null) { // maybe the zone was disabled/enabled
					War.war.getWarHub().getVolume().resetBlocks();
					War.war.getWarHub().initialize();
				}
			} else if (namedParamReturn.equals("PARSE-ERROR")) {
				this.badMsg("Failed to read named parameter(s).");
			} else {
				// empty return means no param was parsed - print command usage
				return false;
			}

			return true;
		}
	}
}
