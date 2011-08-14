package bukkit.tommytony.war.command;

import org.bukkit.command.CommandSender;

import bukkit.tommytony.war.War;
import bukkit.tommytony.war.WarCommandHandler;

/**
 * Unloads war.
 *
 * @author Tim Düsterhus
 */
public class UnloadWarCommand extends AbstractZoneMakerCommand {
	public UnloadWarCommand(WarCommandHandler handler, CommandSender sender, String[] args) throws NotZoneMakerException {
		super(handler, sender, args);
	}

	@Override
	public boolean handle() {
		if (this.args.length != 0) {
			return false;
		}

		War.war.unloadWar();
		this.msg("War unloaded.");
		return true;
	}
}
