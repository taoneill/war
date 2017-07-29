package com.tommytony.war.command;

import com.tommytony.war.War;
import com.tommytony.war.ui.WarUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.logging.Level;


/**
 * Handles commands received by War
 *
 * @author Tim Düsterhus
 * @package bukkit.tommytony.war
 */
public class WarCommandHandler {

	/**
	 * Handles a command
	 *
	 * @param sender
	 *                The sender of the command
	 * @param cmd
	 *                The command
	 * @param args
	 *                The arguments
	 * @return Success
	 */
	public boolean handle(CommandSender sender, Command cmd, String[] args) {
		String command = cmd.getName();
		String[] arguments = null;

		// parse prefixed commands
		if ((command.equals("war") || command.equals("War")) && args.length > 0) {
			command = args[0];
			arguments = new String[args.length - 1];
			for (int i = 1; i <= arguments.length; i++) {
				arguments[i - 1] = args[i];
			}

			if (arguments.length == 1 && (arguments[0].equals("help") || arguments[0].equals("h"))) {
				// show /war help
				War.war.badMsg(sender, cmd.getUsage());
				return true;
			}
		} else if (command.equals("war") || command.equals("War")) {
			if (sender instanceof Player) {
				War.war.getUIManager().assignUI((Player) sender, new WarUI());
			} else {
				War.war.badMsg(sender, "Use /war help for information.");
			}
			return true;
		} else {
			arguments = args;
		}

		AbstractWarCommand commandObj = null;
		try {
			if (command.equals("warhub")) {
				commandObj = new WarhubCommand(this, sender, arguments);
			} else if (command.equals("zones") || command.equals("warzones")) {
				commandObj = new WarzonesCommand(this, sender, arguments);
			} else if (command.equals("zone") || command.equals("warzone")) {
				commandObj = new WarzoneCommand(this, sender, arguments);
			} else if (command.equals("teams")) {
				commandObj = new TeamsCommand(this, sender, arguments);
			} else if (command.equals("join")) {
				commandObj = new JoinCommand(this, sender, arguments);
			} else if (command.equals("leave")) {
				commandObj = new LeaveCommand(this, sender, arguments);
			} else if (command.equals("team")) {
				commandObj = new TeamCommand(this, sender, arguments);
			} else if (command.equals("setzone")) {
				commandObj = new SetZoneCommand(this, sender, arguments);
			} else if (command.equals("deletezone")) {
				commandObj = new DeleteZoneCommand(this, sender, arguments);
			} else if (command.equals("setzonelobby")) {
				commandObj = new SetZoneLobbyCommand(this, sender, arguments);
			} else if (command.equals("savezone")) {
				commandObj = new SaveZoneCommand(this, sender, arguments);
			} else if (command.equals("resetzone")) {
				commandObj = new ResetZoneCommand(this, sender, arguments);
			} else if (command.equals("nextbattle")) {
				commandObj = new NextBattleCommand(this, sender, arguments);
			} else if (command.equals("renamezone")) {
				commandObj = new RenameZoneCommand(this, sender, arguments);
			} else if (command.equals("setteam")) {
				commandObj = new SetTeamCommand(this, sender, arguments);
			} else if (command.equals("deleteteam")) {
				commandObj = new DeleteTeamCommand(this, sender, arguments);
			} else if (command.equals("setteamflag")) {
				commandObj = new SetTeamFlagCommand(this, sender, arguments);
			} else if (command.equals("deleteteamflag")) {
				commandObj = new DeleteTeamFlagCommand(this, sender, arguments);
			} else if (command.equals("setmonument")) {
				commandObj = new SetMonumentCommand(this, sender, arguments);
			} else if (command.equals("deletemonument")) {
				commandObj = new DeleteMonumentCommand(this, sender, arguments);
			} else if (command.equals("setcapturepoint")) {
				commandObj = new SetCapturePointCommand(this, sender, arguments);
			} else if (command.equals("deletecapturepoint")) {
				commandObj = new DeleteCapturePointCommand(this, sender, arguments);
			} else if (command.equals("setbomb")) {
				commandObj = new SetBombCommand(this, sender, arguments);
			} else if (command.equals("deletebomb")) {
				commandObj = new DeleteBombCommand(this, sender, arguments);
			} else if (command.equals("setcake")) {
				commandObj = new SetCakeCommand(this, sender, arguments);
			} else if (command.equals("deletecake")) {
				commandObj = new DeleteCakeCommand(this, sender, arguments);
			}else if (command.equals("setteamconfig") || command.equals("teamcfg")) {
				commandObj = new SetTeamConfigCommand(this, sender, arguments);
			} else if (command.equals("setzoneconfig") || command.equals("zonecfg")) {
				commandObj = new SetZoneConfigCommand(this, sender, arguments);
			} else if (command.equals("setwarhub")) {
				commandObj = new SetWarHubCommand(this, sender, arguments);
			} else if (command.equals("deletewarhub")) {
				commandObj = new DeleteWarhubCommand(this, sender, arguments);
			} else if (command.equals("loadwar")) {
				commandObj = new LoadWarCommand(this, sender, arguments);
			} else if (command.equals("unloadwar")) {
				commandObj = new UnloadWarCommand(this, sender, arguments);
			} else if (command.equals("setwarconfig") || command.equals("warcfg")) {
				commandObj = new SetWarConfigCommand(this, sender, arguments);
			} else if (command.equals("zonemaker") || command.equals("zm")) {
				commandObj = new ZoneMakerCommand(this, sender, arguments);
			}
			// we are not responsible for any other command
		} catch (NotWarAdminException e) {
			War.war.badMsg(sender, "war.notadmin");
		} catch (NotZoneMakerException e) {
			War.war.badMsg(sender, "war.notzm");
		} catch (Exception e) {
			War.war.log("An error occured while handling command " + cmd.getName() + ". Exception:" + e.getClass().toString() + " " + e.getMessage(), Level.WARNING);
			e.printStackTrace();
		}

		if(commandObj != null) {
			boolean handled = commandObj.handle();
			if(!handled) {
				War.war.badMsg(sender, cmd.getUsage());
			}
		}

		return true;
	}
}
