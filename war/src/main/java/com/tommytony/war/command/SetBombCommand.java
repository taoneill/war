package com.tommytony.war.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


import com.tommytony.war.Warzone;
import com.tommytony.war.mapper.WarzoneYmlMapper;
import com.tommytony.war.structure.Bomb;

/**
 * Places a bomb
 *
 * @author tommytony
 */
public class SetBombCommand extends AbstractZoneMakerCommand {
	public SetBombCommand(WarCommandHandler handler, CommandSender sender, String[] args) throws NotZoneMakerException {
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

		if (zone.hasBomb(this.args[0])) {
			// move the existing bomb
			Bomb bomb = zone.getBomb(this.args[0]);
			bomb.getVolume().resetBlocks();
			bomb.setLocation(player.getLocation());
			this.msg("Bomb " + bomb.getName() + " was moved.");
		} else {
			// create a new bomb
			Bomb bomb = new Bomb(this.args[0], zone, player.getLocation());
			zone.getBombs().add(bomb);
			this.msg("Bomb " + bomb.getName() + " created.");
		}

		WarzoneYmlMapper.save(zone, false);

		return true;
	}
}
