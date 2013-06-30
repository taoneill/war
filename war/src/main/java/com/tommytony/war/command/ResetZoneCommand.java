package com.tommytony.war.command;

import java.util.logging.Level;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.tommytony.war.Team;
import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import com.tommytony.war.structure.ZoneLobby;
import java.util.Iterator;
import org.kitteh.tag.TagAPI;


public class ResetZoneCommand extends AbstractZoneMakerCommand {
	public ResetZoneCommand(WarCommandHandler handler, CommandSender sender, String[] args) throws NotZoneMakerException {
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

		zone.clearThieves();
		for (Team team : zone.getTeams()) {
			team.teamcast("The war has ended. " + zone.getTeamInformation() + " Resetting warzone " + zone.getName() + " and teams...");
			for (Iterator<Player> it = team.getPlayers().iterator(); it.hasNext();) {
				Player p = it.next();
				it.remove();
				if (War.war.isTagServer()) {
					TagAPI.refreshPlayer(p);
				}
				zone.restorePlayerState(p);
				p.teleport(zone.getTeleport());
				War.war.msg(p, "You have left the warzone. Your inventory is being restored.");
			}
			team.resetPoints();
			team.getPlayers().clear();
		}

		this.msg("Reloading warzone " + zone.getName() + ".");
			
		zone.reinitialize();
		
		War.war.log(this.getSender().getName() + " reset warzone " + zone.getName(), Level.INFO);

		return true;
	}
}
