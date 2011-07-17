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

public class JoinCommand extends AbstractWarCommand {
	public JoinCommand(WarCommandHandler handler, CommandSender sender, String[] args) {
		super(handler, sender, args);
	}

	public boolean handle() {
		if (!(this.sender instanceof Player)) return true;

		Player player = (Player) this.sender;
		if (!War.war.canPlayWar(player)) {
			//War.war.badMsg(player, "Cannot play war.");
			return true;
		}
		if (this.args.length < 1) {
			return false;
		}
		Warzone zone = War.war.getWarzoneFromLocation(player);
		if (zone == null) {
			War.war.badMsg(player, "No such warzone.");
			return true;
		}
		// drop from old team if any
		Team previousTeam = War.war.getPlayerTeam(player.getName());
		if (previousTeam != null) {
			Warzone oldZone = War.war.getPlayerTeamWarzone(player.getName());
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
			War.war.badMsg(player, "This warzone is disabled.");
		} else {
			List<Team> teams = zone.getTeams();
			boolean foundTeam = false;
			for (Team team : teams) {
				if (team.getName().startsWith(name) || team.getKind() == kind) {
					if (!zone.hasPlayerInventory(player.getName())) {
						zone.keepPlayerInventory(player);
						War.war.msg(player, "Your inventory is in storage until you /leave.");
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
						War.war.badMsg(player, "Team " + team.getName() + " is full.");
						foundTeam = true;
					}
				}
			}
			if (foundTeam) {
				for (Team team : teams) {
					team.teamcast("" + player.getName() + " joined " + team.getName());
				}
			} else {
				War.war.badMsg(player, "No such team. Try /teams.");
			}
		}
		return true;
	}
}
