package bukkit.tommytony.war;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import bukkit.tommytony.war.command.*;

/**
 * @author 	Tim Düsterhus
 * @package 	bukkit.tommytony.war
 */
public class WarCommandHandler {

	public boolean handle(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		String command = cmd.getName();
		String[] arguments = null;
		if ((command.equals("war") || command.equals("War")) && args.length > 0) {
			command = args[0];
			arguments = new String[args.length - 1];
			for (int i = 1; i <= arguments.length; i++) {
				arguments[i - 1] = args[i];
			}
			if (arguments.length == 1 && (arguments[0].equals("help") || arguments[0].equals("h"))) {
				return false;
			}
		} else if (command.equals("war") || command.equals("War")) {
			return false;
		} else {
			arguments = args;
		}

		AbstractWarCommand commandObj = null;
		if (command.equals("zones") || command.equals("warzones")) {
			commandObj = new WarzonesCommand(this, sender, arguments);
		}
		else if (command.equals("zone") || command.equals("warzone")) {
			commandObj = new WarzoneCommand(this, sender, arguments);
		}
		else {
			// we are not responsible for this command
			return true;
		}

		return commandObj.handle();
	}
}
