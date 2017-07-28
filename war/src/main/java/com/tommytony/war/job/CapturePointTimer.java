package com.tommytony.war.job;

import com.tommytony.war.Team;
import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import com.tommytony.war.config.TeamConfig;
import com.tommytony.war.structure.CapturePoint;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class CapturePointTimer extends BukkitRunnable {

	@Override
	public void run() {
		if (!War.war.isLoaded()) {
			return;
		}

		for (Player player : War.war.getServer().getOnlinePlayers()) {
			Warzone zone = Warzone.getZoneByPlayerName(player.getName());
			Team team = Team.getTeamByPlayerName(player.getName());

			if (zone == null || team == null) {
				continue;
			}

			for (CapturePoint cp : zone.getCapturePoints()) {
				if (cp.getVolume().contains(player.getLocation())) {
					// player is maintaining or contesting capture point.
					if (cp.getController() == null) {
						// take control of unclaimed point
						incrementStrength(cp, player, zone, team);
					} else if (cp.getController() != team.getKind()) {
						// contest other team's point
						decrementStrength(cp, player, zone, team);
					} else if (cp.getController() == team.getKind()) {
						// maintain your team's point
						incrementStrength(cp, player, zone, team);
					}
				}
			}
		}

		for (Warzone zone : War.war.getWarzones()) {
			for (CapturePoint cp : zone.getCapturePoints()) {
				if (cp.getController() != null && cp.getController() != cp.getDefaultController()
						&& cp.getStrength() == cp.getMaxStrength()) {
					int controlTime = cp.getControlTime() + 1;
					cp.setControlTime(controlTime);
					if (controlTime % cp.getMaxStrength() == 0) {
						// give points for every control time which is a multiple of the time taken to capture
						Team team = zone.getTeamByKind(cp.getController());
						team.addPoint();
						zone.broadcast("zone.capturepoint.addpoint",
								cp.getController().getFormattedName(), cp.getName());
						// Detect win conditions
						if (team.getPoints() >= team.getTeamConfig().resolveInt(TeamConfig.MAXSCORE)) {
							zone.handleScoreCapReached(team.getName());
						} else {
							// just added a point
							team.resetSign();
							zone.getLobby().resetTeamGateSign(team);
						}
					}
				}
			}
		}
	}

	private static void decrementStrength(CapturePoint cp, Player player, Warzone zone, Team team) {
		int strength = cp.getStrength();
		if (strength < 1) {
			// strength is already at minimum, ensure attributes are wiped
			cp.setController(null);
			cp.setStrength(0);
			return;
		}
		strength -= 1;
		if (strength == 0) {
			if (cp.antiChatSpam()) {
				zone.broadcast("zone.capturepoint.lose", cp.getController().getFormattedName(), cp.getName());
			}
			cp.setControlTime(0);
			cp.setController(null);
		} else if (strength == cp.getMaxStrength() - 1) {
			if (cp.antiChatSpam()) {
				zone.broadcast("zone.capturepoint.contest", cp.getName(),
						team.getKind().getColor() + player.getName() + ChatColor.WHITE);
			}
		}
		cp.setStrength(strength);
	}

	private static void incrementStrength(CapturePoint cp, Player player, Warzone zone, Team team) {
		int strength = cp.getStrength();
		if (strength > cp.getMaxStrength()) {
			// cap strength at CapturePoint.MAX_STRENGTH
			cp.setStrength(cp.getMaxStrength());
			return;
		} else if (strength == cp.getMaxStrength()) {
			// do nothing
			return;
		}
		strength += 1;
		if (strength == cp.getMaxStrength()) {
			if (cp.antiChatSpam()) {
				zone.broadcast("zone.capturepoint.capture", cp.getController().getFormattedName(), cp.getName());
			}
			team.addPoint();
		} else if (strength == 1) {
			if (cp.antiChatSpam()) {
				zone.broadcast("zone.capturepoint.fortify", team.getKind().getFormattedName(), cp.getName());
			}
			cp.setController(team.getKind());
		}
		cp.setStrength(strength);
	}
}
