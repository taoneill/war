package bukkit.tommytony.war.command;

import org.bukkit.command.CommandSender;

import bukkit.tommytony.war.WarCommandHandler;

public abstract class AbstractWarCommand {

	protected CommandSender sender;
	protected String[] args;
	protected WarCommandHandler handler;
	public AbstractWarCommand(WarCommandHandler handler, CommandSender sender, String[] args) {
		this.handler = handler;
		this.sender = sender;
		this.args = args;
	}

	abstract public boolean handle();

	public void msg(String message) {
		this.sender.sendMessage(message);
	}
}
