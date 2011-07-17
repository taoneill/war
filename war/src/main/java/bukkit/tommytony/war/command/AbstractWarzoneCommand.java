package bukkit.tommytony.war.command;

import org.bukkit.command.CommandSender;

import bukkit.tommytony.war.WarCommandHandler;

import com.tommytony.war.Warzone;

public abstract class AbstractWarzoneCommand extends AbstractWarCommand {
	protected int parameterCount = 0;
	protected Warzone zone = null;
	public AbstractWarzoneCommand(WarCommandHandler handler, CommandSender sender, String[] args) {
		super(handler, sender, args);

		// TODO: Find the warzone either from name (support console too) or from location
	}


}
