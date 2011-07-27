package bukkit.tommytony.war.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import bukkit.tommytony.war.WarCommandHandler;

import com.tommytony.war.Warzone;
import com.tommytony.war.ZoneLobby;

public class TeamsCommand extends AbstractWarCommand {
	public TeamsCommand(WarCommandHandler handler, CommandSender sender, String[] args) {
		super(handler, sender, args);
	}

	public boolean handle() {
		Warzone zone;
		if (this.args.length == 1) {
			zone = Warzone.getZoneByName(this.args[0]);
		} else {
			if (!(this.sender instanceof Player)) {
				return false;
			}
			zone = Warzone.getZoneByLocation((Player) this.sender);
			if (zone == null) {
				ZoneLobby lobby = ZoneLobby.getLobbyByLocation((Player) this.sender);
				if (lobby == null) return false;
				zone = lobby.getZone();
			}
		}
		if (zone == null) {
			return false;
		}

		this.sender.sendMessage(zone.getTeamInformation());

		return true;
	}
}
