package com.tommytony.war.command;

import org.bukkit.command.CommandSender;

import com.tommytony.war.War;


/**
 * Loads war.
 *
 * @author Tim Düsterhus
 */
public class LoadWarCommand extends AbstractWarAdminCommand {
	public LoadWarCommand(WarCommandHandler handler, CommandSender sender, String[] args) throws NotWarAdminException {
		super(handler, sender, args);
	}

	@Override
	public boolean handle() {
		if (this.args.length != 0) {
			return false;
		}

		War.war.loadWar();
		this.msg("War loaded.");
		return true;
	}
}
