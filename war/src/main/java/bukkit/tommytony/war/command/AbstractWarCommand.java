package bukkit.tommytony.war.command;

import org.bukkit.command.CommandSender;

public abstract class AbstractWarCommand {
	protected int parameterCount = 0;
	protected CommandSender sender;
	protected String[] args;
	public AbstractWarCommand(CommandSender sender, String[] args) {
		this.sender = sender;
		this.args = args;
	}

	abstract public boolean handle();
}
