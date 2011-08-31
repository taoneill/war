package bukkit.tommytony.war.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.tommytony.war.Team;
import com.tommytony.war.Warzone;
import com.tommytony.war.ZoneLobby;
import com.tommytony.war.TeamKind;
import com.tommytony.war.mappers.WarzoneMapper;

import bukkit.tommytony.war.WarCommandHandler;

/**
 * Deletes a team.
 *
 * @author Tim Düsterhus
 */
public class DeleteTeamCommand extends AbstractZoneMakerCommand {
	public DeleteTeamCommand(WarCommandHandler handler, CommandSender sender, String[] args) throws NotZoneMakerException {
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
			if (!(this.getSender() instanceof Player)) {
				return false;
			}
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

		Team team = zone.getTeamByKind(TeamKind.teamKindFromString(this.args[0]));
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
			this.badMsg("No such team.");
		}

		return true;
	}
}
