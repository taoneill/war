package bukkit.tommytony.war.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import bukkit.tommytony.war.WarCommandHandler;

import com.tommytony.war.Warzone;

public class TeamsCommand extends AbstractWarzoneCommand {
	public TeamsCommand(WarCommandHandler handler, CommandSender sender, String[] args) {
		super(handler, sender, args);
	}

	public boolean handle() {
		Warzone zone;
		if (this.args.length == 1) {
			zone = this.getWarzoneFromName(this.args[0]);
		} else {
			if (!(this.sender instanceof Player)) {
				return false;
			}
			zone = this.getWarzoneFromLocation((Player) this.sender);
		}
		if (zone == null) {
			return true;
		}
//		zone.getTeams();
		return true;
	}
}
