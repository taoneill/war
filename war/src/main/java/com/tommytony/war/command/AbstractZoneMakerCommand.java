package com.tommytony.war.command;

import org.bukkit.command.CommandSender;


/**
 * Represents a command that may only be used by zone makers
 *
 * @author Tim Düsterhus
 */
public abstract class AbstractZoneMakerCommand extends AbstractOptionalZoneMakerCommand {

	public AbstractZoneMakerCommand(WarCommandHandler handler, CommandSender sender, String[] args) throws NotZoneMakerException {
		super(handler, sender, args, true);
	}
}
