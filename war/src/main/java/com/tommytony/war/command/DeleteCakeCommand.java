package com.tommytony.war.command;

import java.util.logging.Level;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import com.tommytony.war.mapper.WarzoneYmlMapper;
import com.tommytony.war.structure.Cake;
import com.tommytony.war.structure.ZoneLobby;

/**
 * Deletes a cake.
 *
 * @author tommytony
 */
public class DeleteCakeCommand extends AbstractZoneMakerCommand {
	public DeleteCakeCommand(WarCommandHandler handler, CommandSender sender, String[] args) throws NotZoneMakerException {
		super(handler, sender, args);
	}

	@Override
	public boolean handle() {
		Warzone zone;

		if (this.args.length == 0) {
			return false;
		} else if (this.args.length == 2) {
			zone = Warzone.getZoneByName(this.args[0]);
			this.args[0] = this.args[1];
		} else if (this.args.length == 1) {
			if (!(this.getSender() instanceof Player)) {
				return false;
			}
			zone = Warzone.getZoneByLocation((Player) this.getSender());
			if (zone == null) {
				ZoneLobby lobby = ZoneLobby.getLobbyByLocation((Player) this.getSender());
				if (lobby == null) {
					return false;
				}
				zone = lobby.getZone();
			}
		} else {
			return false;
		}

		if (zone == null) {
			return false;
		} else if (!this.isSenderAuthorOfZone(zone)) {
			return true;
		}

		Cake cake = zone.getCake(this.args[0]);
		if (cake != null) {
			cake.getVolume().resetBlocks();
			zone.getCakes().remove(cake);
			WarzoneYmlMapper.save(zone);
			this.msg("Cake " + cake.getName() + " removed.");
			War.war.log(this.getSender().getName() + " deleted cake " + cake.getName() + " in warzone " + zone.getName(), Level.INFO);
		} else {
			this.badMsg("No such cake.");
		}

		return true;
	}
}
