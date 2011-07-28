package bukkit.tommytony.war.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.tommytony.war.Team;
import com.tommytony.war.TeamKinds;
import com.tommytony.war.Warzone;
import com.tommytony.war.ZoneLobby;
import com.tommytony.war.mappers.WarzoneMapper;

import bukkit.tommytony.war.WarCommandHandler;

public class DeleteTeamCommand extends AbstractZoneMakerCommand {
	public DeleteTeamCommand(WarCommandHandler handler, CommandSender sender, String[] args) throws NoZoneMakerException {
		super(handler, sender, args);
	}

	@Override
	public boolean handle() {
		Warzone zone;
		if (this.args.length == 0) {
			return false;
		} else if (this.args.length == 2) {
			zone = Warzone.getZoneByName(this.args[0]);
			this.args[0] = this.args[1];
		} else if (this.args.length == 1) {
			if (!(this.sender instanceof Player)) {
				return false;
			}
			zone = Warzone.getZoneByLocation((Player) this.sender);
			if (zone == null) {
				ZoneLobby lobby = ZoneLobby.getLobbyByLocation((Player) this.sender);
				if (lobby == null) return false;
				zone = lobby.getZone();
			}
		} else {
			return false;
		}
		if (zone == null) {
			return false;
		}

		Team team = zone.getTeamByKind(TeamKinds.teamKindFromString(this.args[0]));
		if (team != null) {
			if (team.getFlagVolume() != null) {
				team.getFlagVolume().resetBlocks();
			}
			team.getSpawnVolume().resetBlocks();
			zone.getTeams().remove(team);
			if (zone.getLobby() != null) {
				zone.getLobby().initialize();
			}
			WarzoneMapper.save(zone, false);
			this.msg("Team " + team.getName() + " removed.");
		} else {
			this.msg("No such team.");
		}

		return true;
	}
}
