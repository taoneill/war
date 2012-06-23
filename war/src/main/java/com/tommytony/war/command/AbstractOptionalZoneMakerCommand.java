package com.tommytony.war.command;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import com.tommytony.war.War;
import com.tommytony.war.Warzone;


/**
 * Represents a command that may only used by zone makers or regular users
 *
 * @author tommytony
 */
public abstract class AbstractOptionalZoneMakerCommand extends AbstractWarCommand {

	public AbstractOptionalZoneMakerCommand(WarCommandHandler handler, CommandSender sender, String[] args, boolean zoneMakersOnly) throws NotZoneMakerException {
		super(handler, sender, args);

		if (zoneMakersOnly && !this.isSenderZoneMaker()) {
			throw new NotZoneMakerException();
		}
	}
	
	public boolean isSenderZoneMaker() {
		if (this.getSender() instanceof Player) {
			// for players check War.isZoneMaker()
			if (!War.war.isZoneMaker((Player) this.getSender())) {
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
	
	public boolean isSenderAuthorOfZone(Warzone zone) {
		if (this.getSender() instanceof Player) {
			if (War.war.isWarAdmin((Player) this.getSender())) {
				// War admin has rights over all warzones
				return true;
			}
			
			// Not War admin, is he author?
			boolean isAuthor = zone.isAuthor((Player) this.getSender());
			if (!isAuthor) {
				War.war.badMsg(this.getSender(), "You can't do this because you are not an author of the " + zone.getName() + " warzone." );
			}
			return isAuthor;
		} else {
			// From console, you can do anything
			return true;
		}
	}
}
