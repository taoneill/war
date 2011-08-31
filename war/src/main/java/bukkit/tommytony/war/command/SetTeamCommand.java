package bukkit.tommytony.war.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.tommytony.war.Team;
import com.tommytony.war.TeamKind;
import com.tommytony.war.Warzone;
import com.tommytony.war.mappers.WarzoneMapper;

import bukkit.tommytony.war.WarCommandHandler;

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
		}

		TeamKind teamKind = TeamKind.teamKindFromString(this.args[0]);
		Team existingTeam = zone.getTeamByKind(teamKind);
		if (existingTeam != null) {
			// relocate
			existingTeam.setTeamSpawn(player.getLocation());
			this.msg("Team " + existingTeam.getName() + " spawn relocated.");
		} else {
			// new team (use default TeamKind name for now)
			Team newTeam = new Team(teamKind.toString(), teamKind, player.getLocation(), zone);
			newTeam.setRemainingLives(zone.getLifePool());
			zone.getTeams().add(newTeam);
			if (zone.getLobby() != null) {
				zone.getLobby().getVolume().resetBlocks();
				zone.getLobby().initialize();
			}
			newTeam.setTeamSpawn(player.getLocation());
			this.msg("Team " + newTeam.getName() + " created with spawn here.");
		}

		WarzoneMapper.save(zone, false);

		return true;
	}
}
