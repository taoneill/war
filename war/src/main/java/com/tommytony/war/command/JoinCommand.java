package com.tommytony.war.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.tommytony.war.Team;
import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import com.tommytony.war.config.TeamKind;
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
		
		TeamKind kind = TeamKind.teamKindFromString(this.args[0]);

		if (zone.getWarzoneConfig().getBoolean(WarzoneConfig.DISABLED)) {
			this.badMsg("This warzone is disabled.");
		} else if (zone.getWarzoneConfig().getBoolean(WarzoneConfig.AUTOASSIGN)) {
			this.badMsg("This warzone requires you to be automatically assigned to a team. Please enter the autoassign gate instead.");
		} else if (!zone.getWarzoneConfig().getBoolean(WarzoneConfig.JOINMIDBATTLE) && zone.isEnoughPlayers()) {
			this.badMsg("You cannot join a battle in progress in this warzone.");
		} else {
			Team team = zone.getTeamByKind(kind);
			if (kind == null) {
				this.badMsg("There is no team kind called " + args[0] + ".");
			} else if (team == null) {
				this.badMsg("This warzone does not contain a team " + kind.toString() + ".");
			} else if (!War.war.canPlayWar(player, team)) {
				this.badMsg("You don't have permission to join this team.");
			} else if (team.isFull()) {
				this.badMsg("Team " + team.getName() + " is full.");
			} else {
				Team previousTeam = Team.getTeamByPlayerName(player.getName());
				if (previousTeam != null) {
					if (previousTeam == team) {
						War.war.badMsg(player, "You cannot join your own team.");
						return true;
					}
					if (!previousTeam.removePlayer(player.getName())) {
						War.war.log("Could not remove player " + player.getName() + " from team " + previousTeam.getName(), java.util.logging.Level.WARNING);
					}
					previousTeam.resetSign();
				}
				if (player.getWorld() != zone.getWorld()) {
					player.teleport(zone.getWorld().getSpawnLocation());
				}
				if (!zone.hasPlayerState(player.getName())) {
					zone.keepPlayerState(player);
					this.msg("Your inventory is in storage until you use '/war leave'.");
				}
				team.addPlayer(player);
				team.resetSign();
				zone.respawnPlayer(team, player);
				if (War.war.getWarHub() != null) {
					War.war.getWarHub().resetZoneSign(zone);
				}
				for (Team localTeam : zone.getTeams()) {
					localTeam.teamcast(String.format("%s joined team %s.", player.getName(), team.getName()));
				}
			}
		}
		return true;
	}
}
