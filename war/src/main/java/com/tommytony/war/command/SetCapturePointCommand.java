package com.tommytony.war.command;

import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import com.tommytony.war.config.TeamKind;
import com.tommytony.war.mapper.WarzoneYmlMapper;
import com.tommytony.war.structure.CapturePoint;
import com.tommytony.war.structure.Monument;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.logging.Level;

/**
 * Sets a capture point
 *
 * @author Connor Monahan
 */
public class SetCapturePointCommand extends AbstractZoneMakerCommand {
	public SetCapturePointCommand(WarCommandHandler handler, CommandSender sender, String[] args) throws NotZoneMakerException {
		super(handler, sender, args);
	}

	@Override
	public boolean handle() {
		if (!(this.getSender() instanceof Player)) {
			this.badMsg("You can't do this if you are not in-game.");
			return true;
		}

		Player player = (Player) this.getSender();

		if (this.args.length < 1) {
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

		if (zone.hasCapturePoint(this.args[0])) {
			// move the existing capture point
			CapturePoint cp = zone.getCapturePoint(this.args[0]);
			cp.getVolume().resetBlocks();
			cp.setLocation(player.getLocation());
			this.msg("Capture point " + cp.getName() + " was moved.");
			War.war.log(this.getSender().getName() + " moved capture point " + cp.getName() + " in warzone " + zone.getName(), Level.INFO);
		} else {
			// create a new capture point
			TeamKind controller = null;
			int strength = 0;
			if (args.length > 1) {
				controller = TeamKind.teamKindFromString(args[1]);
				strength = 4;
				if (controller == null || zone.getTeamByKind(controller) == null) {
					this.badMsg("Failed to create capture point: team {0} does not exist", args[1]);
					return true;
				}
			}
			CapturePoint cp = new CapturePoint(this.args[0], player.getLocation(), controller, strength, zone);
			zone.getCapturePoints().add(cp);
			War.war.log(this.getSender().getName() + " created capture point " + cp.getName() + " in warzone " + zone.getName(), Level.INFO);
		}

		WarzoneYmlMapper.save(zone);

		return true;
	}
}
