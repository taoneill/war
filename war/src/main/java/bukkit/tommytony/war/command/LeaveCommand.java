package bukkit.tommytony.war.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import bukkit.tommytony.war.War;
import bukkit.tommytony.war.WarCommandHandler;

import com.tommytony.war.Warzone;

public class LeaveCommand extends AbstractWarCommand {
	public LeaveCommand(WarCommandHandler handler, CommandSender sender, String[] args) {
		super(handler, sender, args);
	}

	public boolean handle() {
		if (!(this.sender instanceof Player)) return true;

		Player player = (Player) this.sender;
		if (!War.war.inAnyWarzone(player.getLocation()) || War.war.getPlayerTeam(player.getName()) == null) {
			return false;
		}

		Warzone zone = War.war.getPlayerTeamWarzone(player.getName());
		zone.handlePlayerLeave(player, zone.getTeleport(), true);
		return true;
	}
}
