package com.tommytony.war.command;

import java.util.logging.Level;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import com.tommytony.war.job.PartialZoneResetJob;
import com.tommytony.war.structure.ZoneLobby;


public class NextBattleCommand extends AbstractZoneMakerCommand {
	public NextBattleCommand(WarCommandHandler handler, CommandSender sender, String[] args) throws NotZoneMakerException {
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
		}

		zone.clearThieves();
		zone.broadcast("zone.battle.next", zone.getName());
		
		PartialZoneResetJob.setSenderToNotify(zone, this.getSender());
		
		zone.reinitialize();
		
		War.war.log(this.getSender().getName() + " used nextbattle in warzone " + zone.getName(), Level.INFO);

		return true;
	}
}
