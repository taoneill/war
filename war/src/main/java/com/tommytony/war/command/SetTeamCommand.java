package com.tommytony.war.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


import com.tommytony.war.Team;
import com.tommytony.war.Warzone;
import com.tommytony.war.config.TeamConfig;
import com.tommytony.war.config.TeamKind;
import com.tommytony.war.mapper.WarzoneYmlMapper;

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
				// relocate
				existingTeam.setTeamSpawn(player.getLocation());
				this.msg("Team " + existingTeam.getName() + " spawn relocated.");
			} else {
				// new team (use default TeamKind name for now)
				Team newTeam = new Team(teamKind.toString(), teamKind, player.getLocation(), zone);
				newTeam.setRemainingLives(newTeam.getTeamConfig().resolveInt(TeamConfig.LIFEPOOL));
				zone.getTeams().add(newTeam);
				if (zone.getLobby() != null) {
					zone.getLobby().setLocation(zone.getTeleport());
					zone.getLobby().initialize();
				}
				newTeam.setTeamSpawn(player.getLocation());
				this.msg("Team " + newTeam.getName() + " created with spawn here.");
			}
		}

		WarzoneYmlMapper.save(zone, false);

		return true;
	}
}
