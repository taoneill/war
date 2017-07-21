package com.tommytony.war.job;

import com.tommytony.war.Team;
import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import com.tommytony.war.structure.CapturePoint;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class CapturePointTimer extends BukkitRunnable {
	private static int iteration = 0;

	@Override
	public void run() {
		iteration += 1;
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
	}

	private static void decrementStrength(CapturePoint cp, Player player, Warzone zone, Team team) {
		if (iteration % 5 != 0) {
			return;
		}

		int strength = cp.getStrength();
		if (strength < 1) {
			// strength is already at minimum, ensure attributes are wiped
			cp.setController(null);
			cp.setStrength(0);
			return;
		}
		strength -= 1;
		if (strength == 0) {
			zone.broadcast("Team {0} has lost control of capture point {1}.", cp.getController().getFormattedName(),
					cp.getName());
			cp.setController(null);
		} else if (strength == 3) {
			zone.broadcast("Capture point {0} is being contested by {1}!", cp.getName(),
					team.getKind().getColor() + player.getName() + ChatColor.WHITE);
		}
		cp.setStrength(strength);
	}

	private static void incrementStrength(CapturePoint cp, Player player, Warzone zone, Team team) {
		if (iteration % 5 != 0) {
			return;
		}

		int strength = cp.getStrength();
		if (strength > 4) {
			// cap strength at 4
			cp.setStrength(4);
			return;
		} else if (strength == 4) {
			// do nothing
			return;
		}
		strength += 1;
		if (strength == 4) {
			zone.broadcast("Team {0} has captured point {1}, gaining 1 extra point.",
					cp.getController().getFormattedName(), cp.getName());
			team.addPoint();
		} else if (strength == 1) {
			zone.broadcast("Team {0} is gaining control of point {1}!",
					team.getKind().getFormattedName(), cp.getName());
			cp.setController(team.getKind());
		}
		cp.setStrength(strength);
	}
}
