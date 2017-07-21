package com.tommytony.war.command;

import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import com.tommytony.war.mapper.WarzoneYmlMapper;
import com.tommytony.war.structure.CapturePoint;
import com.tommytony.war.structure.Monument;
import com.tommytony.war.structure.ZoneLobby;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.logging.Level;

/**
 * Deletes a capture point.
 *
 * @author Connor Monahan
 */
public class DeleteCapturePointCommand extends AbstractZoneMakerCommand {
	public DeleteCapturePointCommand(WarCommandHandler handler, CommandSender sender, String[] args) throws NotZoneMakerException {
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

		CapturePoint cp = zone.getCapturePoint(this.args[0]);
		if (cp != null) {
			cp.getVolume().resetBlocks();
			zone.getCapturePoints().remove(cp);
			WarzoneYmlMapper.save(zone);
			this.msg("Capture point " + cp.getName() + " removed.");
			War.war.log(this.getSender().getName() + " deleted capture point " + cp.getName() + " in warzone " + zone.getName(), Level.INFO);
		} else {
			this.badMsg("No such capture point.");
		}

		return true;
	}
}
