package com.tommytony.war.command;

import java.util.logging.Level;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


import com.tommytony.war.Team;
import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import com.tommytony.war.config.TeamConfig;
import com.tommytony.war.config.TeamKind;
import com.tommytony.war.mapper.WarzoneYmlMapper;
import java.util.Collections;
import org.bukkit.Location;

/**
 * Places a soawn
 *
 * @author Tim Düsterhus
 */
public class SetTeamCommand extends AbstractZoneMakerCommand {
	public SetTeamCommand(WarCommandHandler handler, CommandSender sender, String[] args) throws NotZoneMakerException {
		super(handler, sender, args);
	}

	@Override
	public boolean handle() {
		if (!(this.getSender() instanceof Player)) {
			this.badMsg("You can't do this if you are not in-game.");
			return true;
		}

		Player player = (Player) this.getSender();

		if (this.args.length != 1) {
			return false;
		}
		Warzone zone = Warzone.getZoneByLocation(player);

		if (zone == null) {
			return false;
		} else if (!this.isSenderAuthorOfZone(zone)) {
			return true;
		}

		TeamKind teamKind = TeamKind.teamKindFromString(this.args[0]);
		
		if (teamKind == null) {
			return false;
		} else {
			Team existingTeam = zone.getTeamByKind(teamKind);
			if (existingTeam != null) {
				// add additional spawn
				existingTeam.addTeamSpawn(player.getLocation());
				this.msg("Additional spawn added for team " + existingTeam.getName() + ". Use /deleteteam " + existingTeam.getName() + " to remove all spawns.");
				War.war.log(this.getSender().getName() + " moved team " + existingTeam.getName() + " in warzone " + zone.getName(), Level.INFO);
			} else {
				// new team (use default TeamKind name for now)
				Team newTeam = new Team(teamKind.toString(), teamKind, Collections.<Location>emptyList(), zone);
				newTeam.setRemainingLives(newTeam.getTeamConfig().resolveInt(TeamConfig.LIFEPOOL));
				zone.getTeams().add(newTeam);
				if (zone.getLobby() != null) {
					zone.getLobby().setLocation(zone.getTeleport());
					zone.getLobby().initialize();
				}
				newTeam.addTeamSpawn(player.getLocation());
				this.msg("Team " + newTeam.getName() + " created with spawn here.");
				War.war.log(this.getSender().getName() + " created team " + newTeam.getName() + " in warzone " + zone.getName(), Level.INFO);
			}
		}

		WarzoneYmlMapper.save(zone);

		return true;
	}
}
