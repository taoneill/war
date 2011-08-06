package bukkit.tommytony.war.command;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import bukkit.tommytony.war.War;
import bukkit.tommytony.war.WarCommandHandler;

import com.tommytony.war.Team;
import com.tommytony.war.TeamKind;
import com.tommytony.war.TeamKinds;
import com.tommytony.war.Warzone;
import com.tommytony.war.ZoneLobby;

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

		// drop from old team if any
		Team previousTeam = Team.getTeamByPlayerName(player.getName());
		if (previousTeam != null) {
			Warzone oldZone = Warzone.getZoneByPlayerName(player.getName());
			if (!previousTeam.removePlayer(player.getName())) {
				War.war.log("Could not remove player " + player.getName() + " from team " + previousTeam.getName(), java.util.logging.Level.WARNING);
			}
			if (oldZone.isFlagThief(player.getName())) {
				Team victim = oldZone.getVictimTeamForThief(player.getName());
				victim.getFlagVolume().resetBlocks();
				victim.initializeTeamFlag();
				zone.removeThief(player.getName());
				for (Team t : oldZone.getTeams()) {
					t.teamcast("Team " + victim.getName() + " flag was returned.");
				}
			}
			previousTeam.resetSign();
		}

		// join new team
		String name = this.args[0];
		TeamKind kind = TeamKinds.teamKindFromString(this.args[0]);

		if (zone.isDisabled()) {
			this.msg("This warzone is disabled.");
		} else {
			List<Team> teams = zone.getTeams();
			boolean foundTeam = false;
			for (Team team : teams) {
				if (team.getName().startsWith(name) || team.getKind() == kind) {
					if (!zone.hasPlayerInventory(player.getName())) {
						zone.keepPlayerInventory(player);
						this.msg("Your inventory is in storage until you /leave.");
					}
					if (team.getPlayers().size() < zone.getTeamCap()) {
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
