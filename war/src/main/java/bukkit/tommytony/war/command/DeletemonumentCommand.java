package bukkit.tommytony.war.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.tommytony.war.Monument;
import com.tommytony.war.Warzone;
import com.tommytony.war.ZoneLobby;
import com.tommytony.war.mappers.WarzoneMapper;

import bukkit.tommytony.war.WarCommandHandler;

public class DeletemonumentCommand extends AbstractZoneMakerCommand {
	public DeletemonumentCommand(WarCommandHandler handler, CommandSender sender, String[] args) throws NoZoneMakerException {
		super(handler, sender, args);
	}

	@Override
	public boolean handle() {
		Warzone zone;
		if (this.args.length == 1) {
			zone = Warzone.getZoneByName(this.args[0]);
			this.args[0] = this.args[1];
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

		Monument monument = zone.getMonument(this.args[0]);
		if (monument != null) {
			monument.getVolume().resetBlocks();
			zone.getMonuments().remove(monument);
			WarzoneMapper.save(zone, false);
			this.msg("Monument " + monument.getName() + " removed.");
		} else {
			this.msg("No such monument.");
		}

		return true;
	}
}
