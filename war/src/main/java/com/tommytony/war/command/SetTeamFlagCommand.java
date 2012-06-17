package com.tommytony.war.command;

import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


import com.tommytony.war.Team;
import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import com.tommytony.war.config.TeamKind;
import com.tommytony.war.mapper.WarzoneYmlMapper;

/**
 * Places a teamflag
 *
 * @author Tim Düsterhus
 */
public class SetTeamFlagCommand extends AbstractZoneMakerCommand {
	public SetTeamFlagCommand(WarCommandHandler handler, CommandSender sender, String[] args) throws NotZoneMakerException {
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

		TeamKind kind = TeamKind.teamKindFromString(this.args[0]);
		Team team = zone.getTeamByKind(kind);
		if (team == null) {
			// no such team yet
			this.msg("Place the team spawn first.");
		} else if (team.getFlagVolume() == null) {
			// new team flag
			team.setTeamFlag(player.getLocation());
			Location playerLoc = player.getLocation();
			player.teleport(new Location(playerLoc.getWorld(), playerLoc.getBlockX() + 1, playerLoc.getBlockY(), playerLoc.getBlockZ()));
			this.msg("Team " + team.getName() + " flag added here.");
			WarzoneYmlMapper.save(zone);
			War.war.log(this.getSender().getName() + " created team " + team.getName() + " flag in warzone " + zone.getName(), Level.INFO);
		} else {
			// relocate flag
			team.getFlagVolume().resetBlocks();
			team.setTeamFlag(player.getLocation());
			Location playerLoc = player.getLocation();
			player.teleport(new Location(playerLoc.getWorld(), playerLoc.getBlockX() + 1, playerLoc.getBlockY(), playerLoc.getBlockZ() + 1));
			this.msg("Team " + team.getName() + " flag moved.");
			WarzoneYmlMapper.save(zone);
			War.war.log(this.getSender().getName() + " moved team " + team.getName() + " flag in warzone " + zone.getName(), Level.INFO);
		}

		return true;
	}
}
