package com.tommytony.war.command;

import com.tommytony.war.Team;
import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import com.tommytony.war.Warzone.LeaveCause;
import com.tommytony.war.job.PartialZoneResetJob;
import com.tommytony.war.structure.ZoneLobby;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Iterator;
import java.util.logging.Level;


public class ResetZoneCommand extends AbstractZoneMakerCommand {
	public ResetZoneCommand(WarCommandHandler handler, CommandSender sender, String[] args) throws NotZoneMakerException {
		super(handler, sender, args);
	}

	public static void forceResetZone(Warzone zone, CommandSender sender) {
		zone.clearThieves();
		for (Team team : zone.getTeams()) {
			team.teamcast("The war has ended. " + zone.getTeamInformation() + " Resetting warzone " + zone.getName() + " and teams...");
			for (Iterator<Player> it = team.getPlayers().iterator(); it.hasNext(); ) {
				Player p = it.next();
				it.remove();
				team.removePlayer(p);
				if (!zone.getReallyDeadFighters().contains(p.getName())) {
					p.teleport(zone.getEndTeleport(LeaveCause.RESET));
				}
			}
			team.resetPoints();
			team.getPlayers().clear();
		}

		War.war.msg(sender, "Reloading warzone " + zone.getName() + ".");

		PartialZoneResetJob.setSenderToNotify(zone, sender);

		zone.reinitialize();

		War.war.log(sender.getName() + " reset warzone " + zone.getName(), Level.INFO);
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

		forceResetZone(zone, this.getSender());

		return true;
	}
}
