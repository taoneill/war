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
				// show help
				return false;
			}
		} else if (command.equals("war") || command.equals("War")) {
			// show help
			return false;
		} else {
			arguments = args;
		}

		AbstractWarCommand commandObj = null;
		try {
			if (command.equals("zones") || command.equals("warzones")) {
				commandObj = new WarzonesCommand(this, sender, arguments);
			}
			else if (command.equals("zone") || command.equals("warzone")) {
				commandObj = new WarzoneCommand(this, sender, arguments);
			}
			else if (command.equals("teams")) {
				commandObj = new TeamsCommand(this, sender, arguments);
			}
			else if (command.equals("join")) {
				commandObj = new JoinCommand(this, sender, arguments);
			}
			else if (command.equals("leave")) {
				commandObj = new LeaveCommand(this, sender, arguments);
			}
			else if (command.equals("team")) {
				commandObj = new TeamCommand(this, sender, arguments);
			}
			else if (command.equals("deletezone")) {
				commandObj = new DeletezoneCommand(this, sender, arguments);
			}
			else if (command.equals("resetzone")) {
				commandObj = new ResetzoneCommand(this, sender, arguments);
			}
			else if (command.equals("nextbattle")) {
				commandObj = new NextbattleCommand(this, sender, arguments);
			}
			else if (command.equals("setteam")) {
				commandObj = new SetteamCommand(this, sender, arguments);
			}
			else if (command.equals("deleteteam")) {
				commandObj = new DeleteteamCommand(this, sender, arguments);
			}
			else if (command.equals("teamflag")) {
				commandObj = new SetteamflagCommand(this, sender, arguments);
			}
			else if (command.equals("setmonument")) {
				commandObj = new SetmonumentCommand(this, sender, arguments);
			}
			else if (command.equals("deletemonument")) {
				commandObj = new DeletemonumentCommand(this, sender, arguments);
			}
			else if (command.equals("setwarhub")) {
				commandObj = new SetwarhubCommand(this, sender, arguments);
			}
			else if (command.equals("deletewarhub")) {
				commandObj = new DeletewarhubCommand(this, sender, arguments);
			}
			else if (command.equals("loadwar")) {
				commandObj = new LoadwarCommand(this, sender, arguments);
			}
			else if (command.equals("unloadwar")) {
				commandObj = new UnloadwarCommand(this, sender, arguments);
			}
			else if (command.equals("setwarconfig") || command.equals("warcfg")) {
				commandObj = new SetwarconfigCommand(this, sender, arguments);
			}
			else {
				// we are not responsible for this command
				return true;
			}
		}
		catch (AbstractZoneMakerCommand.NoZoneMakerException e) {
			sender.sendMessage("You can't do this if you are not a warzone maker.");
			return true;
		}
		catch (Exception e) {
			return true;
		}

		return commandObj.handle();
	}
}
