package com.tommytony.war.job;

import java.util.List;
import java.util.logging.Level;


import com.tommytony.war.War;
import com.tommytony.war.config.WarzoneConfig;
import com.tommytony.war.game.Warzone;
import com.tommytony.war.mapper.WarzoneYmlMapper;

public class RestoreYmlWarzonesJob implements Runnable {

	private final List<String> warzones;
	private final boolean newWarInstall;

	public RestoreYmlWarzonesJob(List<String> warzones, boolean newWarInstall) {
		this.warzones = warzones;
		this.newWarInstall = newWarInstall;
	}

	public void run() {
		War.war.getWarzones().clear();
		if (this.warzones != null) {
			for (String warzoneName : this.warzones) {
				if (warzoneName != null && !warzoneName.equals("")) {
					War.war.log("Loading zone " + warzoneName + "...", Level.INFO);
					Warzone zone = WarzoneYmlMapper.load(warzoneName, !this.newWarInstall);
					if (zone != null) { // could have failed, would've been logged already
						War.war.getWarzones().add(zone);
	
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

}
