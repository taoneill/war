package bukkit.tommytony.war.command;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import bukkit.tommytony.war.War;
import bukkit.tommytony.war.WarCommandHandler;

public abstract class AbstractZoneMakerCommand extends AbstractWarCommand {

	public AbstractZoneMakerCommand(WarCommandHandler handler, CommandSender sender, String[] args) throws NotZoneMakerException {
		super(handler, sender, args);
		if (sender instanceof Player) {
			if (!War.war.isZoneMaker((Player) sender)) {
				throw new NotZoneMakerException();
			}
		} else if (!(sender instanceof ConsoleCommandSender)) {
			throw new NotZoneMakerException();
		}
	}
}
