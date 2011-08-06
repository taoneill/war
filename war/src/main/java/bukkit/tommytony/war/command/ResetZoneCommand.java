package bukkit.tommytony.war.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.tommytony.war.Team;
import com.tommytony.war.Warzone;
import com.tommytony.war.ZoneLobby;

import bukkit.tommytony.war.War;
import bukkit.tommytony.war.WarCommandHandler;

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
		}

		zone.clearFlagThieves();
		for (Team team : zone.getTeams()) {
			team.teamcast("The war has ended. " + zone.getTeamInformation() + " Resetting warzone " + zone.getName() + " and teams...");
			for (Player p : team.getPlayers()) {
				zone.restorePlayerInventory(p);
				p.teleport(zone.getTeleport());
				War.war.msg(p, "You have left the warzone. Your inventory has been restored.");
			}
			team.resetPoints();
			team.getPlayers().clear();
		}

		this.msg("Reloading warzone " + zone.getName() + ".");
		zone.getVolume().resetBlocksAsJob();
		if (zone.getLobby() != null) {
			zone.getLobby().getVolume().resetBlocksAsJob();
		}
		zone.initializeZoneAsJob();

		return true;
	}
}
