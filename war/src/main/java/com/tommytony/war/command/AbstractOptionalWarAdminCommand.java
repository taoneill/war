package com.tommytony.war.command;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import com.tommytony.war.War;


/**
 * Represents a command that may only be used by War admins
 *
 */
public abstract class AbstractOptionalWarAdminCommand extends AbstractWarCommand {

	public AbstractOptionalWarAdminCommand(WarCommandHandler handler, CommandSender sender, String[] args, boolean mustBeWarAdmin) throws NotWarAdminException {
		super(handler, sender, args);

		if (mustBeWarAdmin && !isSenderWarAdmin()) {
			throw new NotWarAdminException();
		}		
	}
	
	public boolean isSenderWarAdmin() {
		if (this.getSender() instanceof Player) {
			if (!War.war.isWarAdmin((Player) this.getSender())) {
				return false;
			} else {
				return true;
			}
		} else if (!(this.getSender() instanceof ConsoleCommandSender)) {
			return false;
		} else {
			// ConsoleCommandSender is admin
			return true;
		}
		
	}
}
