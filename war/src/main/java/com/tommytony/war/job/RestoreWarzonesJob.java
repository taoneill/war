package com.tommytony.war.job;

import java.sql.SQLException;
import java.util.logging.Level;

import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import com.tommytony.war.config.WarzoneConfig;
import com.tommytony.war.mapper.WarYmlMapper;
import com.tommytony.war.mapper.WarzoneTxtMapper;
import com.tommytony.war.mapper.WarzoneYmlMapper;

public class RestoreWarzonesJob implements Runnable {

	private final String warzonesStr;
	private final boolean newWarInstall;
	private final boolean convertingToYml;

	public RestoreWarzonesJob(String warzonesStr, boolean newWarInstall, boolean convertingToYml) {
		this.warzonesStr = warzonesStr;
		this.newWarInstall = newWarInstall;
		this.convertingToYml = convertingToYml;
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
				
		if (convertingToYml) {
			// Loading process is over, we can convert (i.e. save in new format)
			WarYmlMapper.save();
			War.war.log("Converted war.txt to war.yml.", Level.INFO);
			
			for (Warzone zone : War.war.getWarzones()) {
				WarzoneYmlMapper.save(zone);
				War.war.log("Converted warzone-" + zone.getName() + ".txt to warzone-" + zone.getName() + ".yml.", Level.INFO);
			}			
		}
	}

}
