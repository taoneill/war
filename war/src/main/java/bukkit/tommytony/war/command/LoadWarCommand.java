package bukkit.tommytony.war.command;

import org.bukkit.command.CommandSender;

import bukkit.tommytony.war.NoZoneMakerException;
import bukkit.tommytony.war.War;
import bukkit.tommytony.war.WarCommandHandler;

public class LoadWarCommand extends AbstractZoneMakerCommand {
	public LoadWarCommand(WarCommandHandler handler, CommandSender sender, String[] args) throws NoZoneMakerException {
		super(handler, sender, args);
	}

	@Override
	public boolean handle() {
		if (this.args.length != 0) {
			return false;
		}
		War.war.loadWar();

		return true;
	}
}
