package com.tommytony.war.jobs;

import java.util.logging.Level;

import bukkit.tommytony.war.War;

import com.tommytony.war.Warzone;
import com.tommytony.war.config.WarzoneConfig;
import com.tommytony.war.mappers.WarzoneTxtMapper;

public class RestoreWarzonesJob implements Runnable {

	private final String warzonesStr;
	private final boolean newWarInstall;

	public RestoreWarzonesJob(String warzonesStr, boolean newWarInstall) {
		this.warzonesStr = warzonesStr;
		this.newWarInstall = newWarInstall;
	}

	public void run() {
		String[] warzoneSplit = this.warzonesStr.split(",");
		War.war.getWarzones().clear();
		for (String warzoneName : warzoneSplit) {
			if (warzoneName != null && !warzoneName.equals("")) {
				War.war.log("Loading zone " + warzoneName + "...", Level.INFO);
				Warzone zone = WarzoneTxtMapper.load(warzoneName, !this.newWarInstall);
				if (zone != null) { // could have failed, would've been logged already
					War.war.getWarzones().add(zone);
					// zone.getVolume().loadCorners();
					zone.getVolume().loadCorners();
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
