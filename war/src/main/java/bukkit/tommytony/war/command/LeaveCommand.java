package bukkit.tommytony.war.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import bukkit.tommytony.war.WarCommandHandler;

import com.tommytony.war.Warzone;

public class LeaveCommand extends AbstractWarCommand {
	public LeaveCommand(WarCommandHandler handler, CommandSender sender, String[] args) {
		super(handler, sender, args);
	}

	@Override
	public boolean handle() {
		if (!(this.getSender() instanceof Player)) {
			this.badMsg("You can't do this if you are not in-game.");
			return true;
		}

		if (this.args.length != 0) {
			return false;
		}

		Player player = (Player) this.getSender();
		Warzone zone = Warzone.getZoneByPlayerName(player.getName());
		if (zone == null) {
			return false;
		}

		zone.handlePlayerLeave(player, zone.getTeleport(), true);
		return true;
	}
}
