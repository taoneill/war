package com.tommytony.war.job;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;

import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import com.tommytony.war.config.WarzoneConfig;
import com.tommytony.war.mapper.WarzoneYmlMapper;

public class RestoreYmlWarzonesJob implements Runnable {

	private final List<String> warzones;

	public RestoreYmlWarzonesJob(List<String> warzones) {
		this.warzones = warzones;
	}

	public void run() {
		War.war.getWarzones().clear();
		if (this.warzones != null) {
			for (String warzoneName : this.warzones) {
				if (warzoneName != null && !warzoneName.equals("")) {
					War.war.log("Loading zone " + warzoneName + "...", Level.INFO);
					Warzone zone = WarzoneYmlMapper.load(warzoneName);
					if (zone != null) { // could have failed, would've been logged already
						War.war.getWarzones().add(zone);
						try {
							zone.getVolume().loadCorners();
						} catch (SQLException ex) {
							War.war.log("Failed to load warzone " + warzoneName + ": " + ex.getMessage(), Level.WARNING);
							throw new RuntimeException(ex);
						}
						if (zone.getLobby() != null) {
							zone.getLobby().getVolume().resetBlocks();
						}
						if (zone.getWarzoneConfig().getBoolean(WarzoneConfig.RESETONLOAD)) {
							zone.getVolume().resetBlocks();
						}
						zone.initializeZone();
					}
				}
			}
			if (War.war.getWarzones().size() > 0) {
				War.war.log("Warzones ready.", Level.INFO);
			}
		}
	}

}
