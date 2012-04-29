package com.tommytony.war.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.tommytony.war.Team;
import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import com.tommytony.war.config.TeamKind;
import com.tommytony.war.config.WarzoneConfig;
import com.tommytony.war.structure.ZoneLobby;
import com.tommytony.war.utility.ModifyPermissions;

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
		TeamKind kind;
		if (this.args.length == 2) {
			// zone by name
			zone = Warzone.getZoneByName(this.args[0]);
			kind = TeamKind.teamKindFromString(this.args[1]);
		} else if (this.args.length == 1) {
			zone = Warzone.getZoneByLocation((Player) this.getSender());
			if (zone == null) {
				ZoneLobby lobby = ZoneLobby.getLobbyByLocation((Player) this.getSender());
				if (lobby == null) {
					return false;
				}
				zone = lobby.getZone();
			}
			kind = TeamKind.teamKindFromString(this.args[0]);
		} else {
			return false;
		}
		if (zone == null) {
			return false;
		}
		if (zone.getWarzoneConfig().getBoolean(WarzoneConfig.DISABLED)) {
			this.badMsg("join.disabled");
		} else if (zone.isReinitializing()) {
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
					previousTeam.removePlayer(player);
					previousTeam.resetSign();
				}
				zone.assign(player, team);
			}
		}
		return true;
	}
}
