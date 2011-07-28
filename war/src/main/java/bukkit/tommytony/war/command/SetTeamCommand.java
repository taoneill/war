package bukkit.tommytony.war.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.tommytony.war.Team;
import com.tommytony.war.TeamKind;
import com.tommytony.war.TeamKinds;
import com.tommytony.war.Warzone;
import com.tommytony.war.mappers.WarzoneMapper;

import bukkit.tommytony.war.WarCommandHandler;

public class SetTeamCommand extends AbstractZoneMakerCommand {
	public SetTeamCommand(WarCommandHandler handler, CommandSender sender, String[] args) throws NoZoneMakerException {
		super(handler, sender, args);
	}

	@Override
	public boolean handle() {
		if (!(this.sender instanceof Player)) return true;

		Player player = (Player) this.sender;

		if (this.args.length != 1) {
			return false;
		}
		Warzone zone = Warzone.getZoneByLocation(player);

		if (zone == null) {
			return false;
		}

		TeamKind teamKind = TeamKinds.teamKindFromString(this.args[0]);
		Team existingTeam = zone.getTeamByKind(teamKind);
		if (existingTeam != null) {
			// relocate
			existingTeam.setTeamSpawn(player.getLocation());
			this.msg("Team " + existingTeam.getName() + " spawn relocated.");
		} else {
			// new team (use default TeamKind name for now)
			Team newTeam = new Team(teamKind.getDefaultName(), teamKind, player.getLocation(), zone);
			newTeam.setRemainingLives(zone.getLifePool());
			zone.getTeams().add(newTeam);
			if (zone.getLobby() != null) {
				zone.getLobby().getVolume().resetBlocks();
				// warzone.getVolume().resetWallBlocks(warzone.getLobby().getWall());
				// warzone.addZoneOutline(warzone.getLobby().getWall());
				zone.getLobby().initialize();
			}
			newTeam.setTeamSpawn(player.getLocation());
			this.msg("Team " + newTeam.getName() + " created with spawn here.");
		}

		WarzoneMapper.save(zone, false);

		return true;
	}
}
