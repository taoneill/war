package com.tommytony.war.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


import com.tommytony.war.WarCommandHandler;
import com.tommytony.war.Warzone;
import com.tommytony.war.mapper.WarzoneYmlMapper;
import com.tommytony.war.structure.Cake;

/**
 * Places a cake
 *
 * @author tommytony
 */
public class SetCakeCommand extends AbstractZoneMakerCommand {
	public SetCakeCommand(WarCommandHandler handler, CommandSender sender, String[] args) throws NotZoneMakerException {
		super(handler, sender, args);
	}

	@Override
	public boolean handle() {
		if (!(this.getSender() instanceof Player)) {
			this.badMsg("You can't do this if you are not in-game.");
			return true;
		}

		Player player = (Player) this.getSender();

		if (this.args.length != 1) {
			return false;
		}

		Warzone zone = Warzone.getZoneByLocation(player);

		if (zone == null) {
			return false;
		} else if (!this.isSenderAuthorOfZone(zone)) {
			return true;
		}
		
		if (this.args[0].equals(zone.getName())) {
			return false;
		}

		if (zone.hasCake(this.args[0])) {
			// move the existing cake
			Cake cake = zone.getCake(this.args[0]);
			cake.getVolume().resetBlocks();
			cake.setLocation(player.getLocation());
			this.msg("Cake " + cake.getName() + " was moved.");
		} else {
			// create a new cake
			Cake cake = new Cake(this.args[0], zone, player.getLocation());
			zone.getCakes().add(cake);
			this.msg("Cake " + cake.getName() + " created.");
		}

		WarzoneYmlMapper.save(zone, false);

		return true;
	}
}
