package bukkit.tommytony.war.command;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import bukkit.tommytony.war.War;
import bukkit.tommytony.war.WarCommandHandler;

public abstract class AbstractZoneMakerCommand extends AbstractWarCommand {

	public AbstractZoneMakerCommand(WarCommandHandler handler, CommandSender sender, String[] args) throws NoZoneMakerException {
		super(handler, sender, args);
		if (sender instanceof Player) {
			if (War.war.isZoneMaker((Player) sender)) throw new NoZoneMakerException();
		}
		else if (!(sender instanceof ConsoleCommandSender)) {
			throw new NoZoneMakerException();
		}
	}

	public class NoZoneMakerException extends Exception {
		private static final long serialVersionUID = -70491862705766496L;
	}
}
