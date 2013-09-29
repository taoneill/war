package com.tommytony.war.command;

import org.bukkit.command.CommandSender;

import com.tommytony.war.War;


/**
 * Represents a war command
 *
 * @author Tim Düsterhus
 */
public abstract class AbstractWarCommand {

	/**
	 * The sender of this command
	 *
	 * @var	sender
	 */
	private CommandSender sender;

	/**
	 * The arguments of this command
	 *
	 * @var	args
	 */
	protected String[] args;

	/**
	 * Instance of WarCommandHandler
	 *
	 * @var	handler
	 */
	protected WarCommandHandler handler;

	public AbstractWarCommand(WarCommandHandler handler, CommandSender sender, String[] args) {
		this.handler = handler;
		this.setSender(sender);
		this.args = args;
	}

	/**
	 * Handles the command
	 *
	 * @return	true if command was used the right way
	 */
	abstract public boolean handle();

	/**
	 * Sends a success message
	 *
	 * @param 	message	message to send
	 */
	public void msg(String message) {
		War.war.msg(this.getSender(), message);
	}

	/**
	 * Sends a failure message
	 *
	 * @param 	message	message to send
	 */
	public void badMsg(String message) {
		War.war.badMsg(this.getSender(), message);
	}

	/**
	 * Sends a success message.
	 * @param message Message or key to translate
	 * @param args Arguments for the formatter
	 */
	public void msg(String message, Object... args) {
		War.war.msg(this.getSender(), message, args);
	}

	/**
	 * Sends a failure message.
	 * @param message Message or key to translate
	 * @param args Arguments for the formatter
	 */
	public void badMsg(String message, Object... args) {
		War.war.badMsg(this.getSender(), message, args);
	}

	/**
	 * Changes the command-sender
	 *
	 * @param 	sender	new sender
	 */
	public void setSender(CommandSender sender) {
		this.sender = sender;
	}

	/**
	 * Gets the command-sender
	 *
	 * @return	Command-Sender
	 */
	public CommandSender getSender() {
		return this.sender;
	}
}
