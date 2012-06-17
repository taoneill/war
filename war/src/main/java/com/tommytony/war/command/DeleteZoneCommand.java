package com.tommytony.war.command;

import java.util.logging.Level;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


import com.tommytony.war.Team;
import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import com.tommytony.war.mapper.WarYmlMapper;
import com.tommytony.war.mapper.WarzoneYmlMapper;
import com.tommytony.war.structure.Monument;
import com.tommytony.war.structure.ZoneLobby;

/**
 * Deletes a warzone.
 *
 * @author Tim Düsterhus
 */
public class DeleteZoneCommand extends AbstractZoneMakerCommand {
	public DeleteZoneCommand(WarCommandHandler handler, CommandSender sender, String[] args) throws NotZoneMakerException {
		super(handler, sender, args);
	}

	@Override
	public boolean handle() {
		Warzone zone;

		if (this.args.length == 1) {
			zone = Warzone.getZoneByName(this.args[0]);
		} else if (this.args.length == 0) {
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

		War.war.getWarzones().remove(zone);
		WarYmlMapper.save();
		
		WarzoneYmlMapper.delete(zone);
		
		if (War.war.getWarHub() != null) { // warhub has to change
			War.war.getWarHub().getVolume().resetBlocks();
			War.war.getWarHub().initialize();
		}
		
		String msg = "Warzone " + zone.getName() + " removed by " + this.getSender().getName() + ".";
		War.war.log(msg, Level.INFO);
		this.msg(msg);

		return true;
	}
}
