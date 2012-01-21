package com.tommytony.war.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


import com.tommytony.war.War;
import com.tommytony.war.WarCommandHandler;
import com.tommytony.war.game.Monument;
import com.tommytony.war.game.Team;
import com.tommytony.war.game.Warzone;
import com.tommytony.war.game.ZoneLobby;
import com.tommytony.war.mapper.WarYmlMapper;
import com.tommytony.war.mapper.WarzoneYmlMapper;

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

		for (Team t : zone.getTeams()) {
			if (t.getTeamFlag() != null) {
				t.getFlagVolume().resetBlocks();
			}
			t.getSpawnVolume().resetBlocks();

			// reset inventory
			for (Player p : t.getPlayers()) {
				zone.restorePlayerState(p);
			}
		}
		for (Monument m : zone.getMonuments()) {
			m.getVolume().resetBlocks();
		}
		if (zone.getLobby() != null) {
			zone.getLobby().getVolume().resetBlocks();
		}
		zone.getVolume().resetBlocks();
		War.war.getWarzones().remove(zone);
		WarYmlMapper.save();
		WarzoneYmlMapper.delete(zone.getName());
		if (War.war.getWarHub() != null) { // warhub has to change
			War.war.getWarHub().getVolume().resetBlocks();
			War.war.getWarHub().initialize();
		}
		this.msg("Warzone " + zone.getName() + " removed.");

		return true;
	}
}
