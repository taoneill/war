package bukkit.tommytony.war.command;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import bukkit.tommytony.war.War;
import bukkit.tommytony.war.WarCommandHandler;

/**
 * Represents a command that may only be used by War admins
 *
 */
public abstract class AbstractWarAdminCommand extends AbstractWarCommand {

	public AbstractWarAdminCommand(WarCommandHandler handler, CommandSender sender, String[] args) throws NotWarAdminException {
		super(handler, sender, args);

		if (sender instanceof Player) {
			if (!War.war.isWarAdmin((Player) sender)) {
				throw new NotWarAdminException();
			}
		} else if (!(sender instanceof ConsoleCommandSender)) {
			throw new NotWarAdminException();
		}
	}
}
