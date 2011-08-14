package bukkit.tommytony.war.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import bukkit.tommytony.war.War;
import bukkit.tommytony.war.WarCommandHandler;

import com.tommytony.war.Warzone;
import com.tommytony.war.ZoneLobby;
import com.tommytony.war.mappers.WarzoneMapper;

public class SaveZoneCommand extends AbstractZoneMakerCommand {

	public SaveZoneCommand(WarCommandHandler handler, CommandSender sender, String[] args) throws NotZoneMakerException {
		super(handler, sender, args);
	}

	@Override
	public boolean handle() {
		Warzone zone = null;
		CommandSender commandSender = this.getSender();
		boolean isFirstParamWarzone = false;

		if(this.args.length > 0 && !this.args[0].contains(":")) {
			// warzone name maybe in first place
			Warzone zoneByName = Warzone.getZoneByName(this.args[0]);
			if (zoneByName != null) {
				zone = zoneByName;
				isFirstParamWarzone = true;
			}
		}

		if (this.getSender() instanceof Player) {
			Player player = (Player)commandSender;

			Warzone zoneByLoc = Warzone.getZoneByLocation(player);
			ZoneLobby lobbyByLoc = ZoneLobby.getLobbyByLocation(player);
			if(zoneByLoc == null && lobbyByLoc != null) {
				zoneByLoc = lobbyByLoc.getZone();
			}
			if(zoneByLoc != null) {
				zone = zoneByLoc;
			}
		}

		if (zone == null) {
			// No warzone found, whatever the mean, escape
			return false;
		}

		if (isFirstParamWarzone) {
			if(this.args.length > 1) {
				// More than one param: the arguments need to be shifted
				String[] newargs = new String[this.args.length - 1];
				for (int i = 1; i < this.args.length; i++) {
					newargs[i-1] = args[i];
				}
				this.args = newargs;
			}
		}

		// We have a warzone and indexed-from-0 arguments, let's updatethis.msg(player, "Saving warzone " + warzone.getName() + ".");
		int savedBlocks = zone.saveState(true);

		// changed settings: must reinitialize with new settings
		War.war.updateZoneFromNamedParams(zone, commandSender, args);
		WarzoneMapper.save(zone, true);
		if(this.args.length > 0) {
			// the config may have changed, requiring a reset for spawn styles etc.
			zone.getVolume().resetBlocks();
		}
		if (zone.getLobby() != null) {
			zone.getLobby().getVolume().resetBlocks();
		}
		zone.initializeZone(); // bring back team spawns etc

		if (War.war.getWarHub() != null) { // maybe the zone was disabled/enabled
			War.war.getWarHub().getVolume().resetBlocks();
			War.war.getWarHub().initialize();
		}


		this.msg("Warzone " + zone.getName() + " initial state changed. Saved " + savedBlocks + " blocks.");

		return true;
	}
}
