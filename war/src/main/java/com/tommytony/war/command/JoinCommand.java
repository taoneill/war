package com.tommytony.war.command;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


import com.tommytony.war.Team;
import com.tommytony.war.TeamKind;
import com.tommytony.war.War;
import com.tommytony.war.WarCommandHandler;
import com.tommytony.war.Warzone;
import com.tommytony.war.config.TeamConfig;
import com.tommytony.war.config.WarzoneConfig;
import com.tommytony.war.structure.ZoneLobby;

/**
 * Joins a team.
 *
 * @author Tim Düsterhus
 */
public class JoinCommand extends AbstractWarCommand {
	public JoinCommand(WarCommandHandler handler, CommandSender sender, String[] args) {
		super(handler, sender, args);
	}

	@Override
	public boolean handle() {
		if (!(this.getSender() instanceof Player)) {
			this.badMsg("You can't do this if you are not in-game.");
			return true;
		}

		Player player = (Player) this.getSender();
		if (!War.war.canPlayWar(player)) {
			this.badMsg("Cannot play war. You need the war.player permission.");
			return true;
		}

		Warzone zone;
		if (this.args.length == 0) {
			return false;
		} else if (this.args.length == 2) {
			// zone by name
			zone = Warzone.getZoneByName(this.args[0]);
			// move the team-name to first place :)
			this.args[0] = this.args[1];
		} else if (this.args.length == 1) {
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
		
		String name = this.args[0];
		TeamKind kind = TeamKind.teamKindFromString(this.args[0]);

		// drop from old team if any
		Team previousTeam = Team.getTeamByPlayerName(player.getName());
		if (previousTeam != null) {
			if (previousTeam.getName().startsWith(name) || previousTeam.getKind() == kind) {
				// trying to join own team
				War.war.badMsg(player, "Can't join your own team.");
				return true;
			}
			
			if (!previousTeam.removePlayer(player.getName())) {
				War.war.log("Could not remove player " + player.getName() + " from team " + previousTeam.getName(), java.util.logging.Level.WARNING);
			}
			previousTeam.resetSign();
		}

		// join new team
		

		if (zone.getWarzoneConfig().getBoolean(WarzoneConfig.DISABLED)) {
			this.msg("This warzone is disabled.");
		} else {
			List<Team> teams = zone.getTeams();
			boolean foundTeam = false;
			for (Team team : teams) {
				if (team.getName().startsWith(name) || team.getKind() == kind) {
					if (!zone.hasPlayerState(player.getName())) {
						zone.keepPlayerState(player);
						this.msg("Your inventory is in storage until you use '/war leave'.");
					}
					if (team.getPlayers().size() < team.getTeamConfig().resolveInt(TeamConfig.TEAMSIZE)) {
						team.addPlayer(player);
						team.resetSign();
						zone.respawnPlayer(team, player);
						if (War.war.getWarHub() != null) {
							War.war.getWarHub().resetZoneSign(zone);
						}
						foundTeam = true;
					} else {
						this.msg("Team " + team.getName() + " is full.");
						foundTeam = true;
					}
				}
			}

			if (foundTeam) {
				for (Team team : teams) {
					team.teamcast("" + player.getName() + " joined " + team.getName());
				}
			} else {
				this.msg("No such team. Try /teams.");
			}
		}
		return true;
	}
}
