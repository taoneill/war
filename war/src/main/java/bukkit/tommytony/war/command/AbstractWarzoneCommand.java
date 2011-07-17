package bukkit.tommytony.war.command;

import com.tommytony.war.Warzone;

public abstract class AbstractWarzoneCommand {

	protected Warzone zone = null;
	public AbstractWarzoneCommand(CommandSender sender, String[] args) {
		super(sender, args);

	}


}
