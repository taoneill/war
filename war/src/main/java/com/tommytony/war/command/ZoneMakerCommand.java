package com.tommytony.war.command;

import java.util.logging.Level;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


import com.tommytony.war.War;
import com.tommytony.war.mapper.WarYmlMapper;

/**
 * Makes a player zonemaker and other way round.
 *
 * @author Tim Düsterhus
 */
public class ZoneMakerCommand extends AbstractWarCommand {

	public ZoneMakerCommand(WarCommandHandler handler, CommandSender sender, String[] args) throws NotZoneMakerException {
		super(handler, sender, args);

		if (sender instanceof Player) { // i hate java for this.
			if (!War.war.isZoneMaker((Player) sender)) {
				for (String name : War.war.getZoneMakersImpersonatingPlayers()) {
					if (((Player) sender).getName().equals(name)) {
						return;
					}
				}
				throw new NotZoneMakerException();
			}
		}
	}

	@Override
	public boolean handle() {
		if (!(this.getSender() instanceof Player)) {
			this.badMsg("You can't do this if you are not in-game.");
			return true;
		}
		Player player = (Player) this.getSender();

		if (War.war.isZoneMaker(player)) {
			if (this.args.length == 0) {
				War.war.getZoneMakersImpersonatingPlayers().add(player.getName());
				this.msg("You are now impersonating a regular player. Type /zonemaker again to toggle back to war maker mode.");
			} else if (this.args.length == 1) {
				// make someone zonemaker or remove the right
				if (War.war.getZoneMakerNames().contains(this.args[0])) {
					// kick
					War.war.getZoneMakerNames().remove(this.args[0]);
					this.msg(this.args[0] + " is not a zone maker anymore.");
					// TODO store zone makers using UUIDs
					Player kickedMaker = War.war.getServer().getPlayer(this.args[0]);
					if (kickedMaker != null) {
						War.war.msg(kickedMaker, player.getName() + " took away your warzone maker priviledges.");
						War.war.log(player.getName() + " took away zonemaker rights from " + kickedMaker, Level.INFO);
					}
				} else {
					// add
					War.war.getZoneMakerNames().add(this.args[0]);
					this.msg(this.args[0] + " is now a zone maker.");
					Player newMaker = War.war.getServer().getPlayer(this.args[0]);
					if (newMaker != null) {
						War.war.msg(newMaker, player.getName() + " made you warzone maker.");
						War.war.log(player.getName() + " made " + newMaker + " a zonemaker", Level.INFO);
					}
				}
			} else {
				return false;
			}
		} else {
			if (this.args.length != 0) {
				return false;
			}

			War.war.getZoneMakersImpersonatingPlayers().remove(player.getName());
			this.msg("You are back as a zone maker.");
			WarYmlMapper.save();
		}

		return true;
	}
}
