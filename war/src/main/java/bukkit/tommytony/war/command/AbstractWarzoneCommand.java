package bukkit.tommytony.war.command;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import bukkit.tommytony.war.War;
import bukkit.tommytony.war.WarCommandHandler;

import com.tommytony.war.Warzone;
import com.tommytony.war.ZoneLobby;

public abstract class AbstractWarzoneCommand extends AbstractWarCommand {
	protected Warzone zone = null;
	public AbstractWarzoneCommand(WarCommandHandler handler, CommandSender sender, String[] args) {
		super(handler, sender, args);
	}

	public Warzone getWarzoneFromLocation(Player player) {
		return this.getWarzoneFromLocation(player.getLocation());
	}

	public Warzone getWarzoneFromLocation(Location location) {
		Warzone zone = War.war.warzone(location);
		if (zone == null) {
			ZoneLobby lobby = War.war.lobby(location);
			if (lobby == null) return null;
			zone = lobby.getZone();
		}
		return zone;
	}

	public Warzone getWarzoneFromName(String name) {
		for (Warzone zone : War.war.getWarzones()) {
			if (zone.getName().toLowerCase().equals(name.toLowerCase())) {
				return zone;
			}
		}
		return null;
	}
}
