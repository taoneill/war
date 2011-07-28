package bukkit.tommytony.war.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.tommytony.war.Monument;
import com.tommytony.war.Team;
import com.tommytony.war.Warzone;
import com.tommytony.war.ZoneLobby;
import com.tommytony.war.mappers.WarMapper;
import com.tommytony.war.mappers.WarzoneMapper;

import bukkit.tommytony.war.NoZoneMakerException;
import bukkit.tommytony.war.War;
import bukkit.tommytony.war.WarCommandHandler;

public class DeleteZoneCommand extends AbstractZoneMakerCommand {
	public DeleteZoneCommand(WarCommandHandler handler, CommandSender sender, String[] args) throws NoZoneMakerException {
		super(handler, sender, args);
	}

	@Override
	public boolean handle() {
		Warzone zone;

		if (this.args.length == 1) {
			zone = Warzone.getZoneByName(this.args[0]);
		} else if (this.args.length == 0) {
			if (!(this.sender instanceof Player)) {
				return false;
			}
			zone = Warzone.getZoneByLocation((Player) this.sender);
			if (zone == null) {
				ZoneLobby lobby = ZoneLobby.getLobbyByLocation((Player) this.sender);
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

		for (Team t : zone.getTeams()) {
			if (t.getTeamFlag() != null) {
				t.getFlagVolume().resetBlocks();
			}
			t.getSpawnVolume().resetBlocks();

			// reset inventory
			for (Player p : t.getPlayers()) {
				zone.restorePlayerInventory(p);
			}
		}
		for (Monument m : zone.getMonuments()) {
			m.getVolume().resetBlocks();
		}
		if (zone.getLobby() != null) {
			zone.getLobby().getVolume().resetBlocks();
		}
		zone.getVolume().resetBlocks();
		War.war.getWarzones().remove(zone);
		WarMapper.save();
		WarzoneMapper.delete(zone.getName());
		if (War.war.getWarHub() != null) { // warhub has to change
			War.war.getWarHub().getVolume().resetBlocks();
			War.war.getWarHub().initialize();
		}
		this.msg("Warzone " + zone.getName() + " removed.");

		return true;
	}
}
