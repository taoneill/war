package com.tommytony.war.job;

import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import com.tommytony.war.config.ScoreboardType;
import com.tommytony.war.config.WarzoneConfig;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Switches scoreboards periodically
 * Github #790
 */
public class ScoreboardSwitchTimer extends BukkitRunnable {
	@Override
	public void run() {
		if (!War.war.isLoaded()) {
			return;
		}
		for (Warzone zone : War.war.getEnabledWarzones()) {
			if (zone.getWarzoneConfig().getScoreboardType(WarzoneConfig.SCOREBOARD) == ScoreboardType.SWITCHING) {
				switch (zone.getScoreboardType()) {
					case SWITCHING:
						zone.setScoreboardType(ScoreboardType.POINTS);
						break;
					case POINTS:
						zone.setScoreboardType(ScoreboardType.LIFEPOOL);
						break;
					case LIFEPOOL:
						zone.setScoreboardType(ScoreboardType.TOPKILLS);
						break;
					case TOPKILLS:
						zone.setScoreboardType(ScoreboardType.PLAYERCOUNT);
						break;
					case PLAYERCOUNT:
						zone.setScoreboardType(ScoreboardType.POINTS);
						break;
					default:
						break;
				}
				zone.updateScoreboard();
			}
		}
	}
}
