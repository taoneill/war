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
			this.badMsg("command.console");
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
			this.badMsg("join.disabled");
		} else if (zone.getWarzoneConfig().getBoolean(WarzoneConfig.AUTOASSIGN)) {
			this.badMsg("join.aarequired");
		} else if (!zone.getWarzoneConfig().getBoolean(WarzoneConfig.JOINMIDBATTLE) && zone.isEnoughPlayers()) {
			this.badMsg("join.progress");
		} else {
			Team team = zone.getTeamByKind(kind);
			if (kind == null || team == null) {
				this.badMsg("join.team404");
			} else if (!War.war.canPlayWar(player, team)) {
				this.badMsg("join.permission.single");
			} else if (team.isFull()) {
				this.badMsg("join.full.single", team.getName());
			} else {
				Team previousTeam = Team.getTeamByPlayerName(player.getName());
				if (previousTeam != null) {
					if (previousTeam == team) {
						this.badMsg("join.selfteam");
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
					this.msg("join.inventorystored");
				}
				team.addPlayer(player);
				team.resetSign();
				zone.respawnPlayer(team, player);
				if (War.war.getWarHub() != null) {
					War.war.getWarHub().resetZoneSign(zone);
				}
				zone.broadcast("join.broadcast", player.getName(), team.getName());
			}
		}
		return true;
	}
}
