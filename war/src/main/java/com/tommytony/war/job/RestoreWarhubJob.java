package com.tommytony.war.job;

import java.sql.SQLException;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.World;


import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import com.tommytony.war.mapper.VolumeMapper;
import com.tommytony.war.structure.WarHub;
import com.tommytony.war.volume.Volume;

public class RestoreWarhubJob implements Runnable {

	private final String hubStr;

	public RestoreWarhubJob(String hubStr) {
		this.hubStr = hubStr;
	}

	public void run() {
		String[] hubStrSplit = this.hubStr.split(",");

		int hubX = Integer.parseInt(hubStrSplit[0]);
		int hubY = Integer.parseInt(hubStrSplit[1]);
		int hubZ = Integer.parseInt(hubStrSplit[2]);
		World world = null;
		String worldName;
		String hubOrientation = "west";
		if (hubStrSplit.length > 3) {
			worldName = hubStrSplit[3];
			world = War.war.getServer().getWorld(worldName);
			if (hubStrSplit.length > 4) {
				hubOrientation = hubStrSplit[4];
			}
		} else {
			worldName = "DEFAULT";
			world = War.war.getServer().getWorlds().get(0); // default to first world
		}
		if (world != null) {
			Location hubLocation = new Location(world, hubX, hubY, hubZ);
			WarHub hub = new WarHub(hubLocation, hubOrientation);
			War.war.setWarHub(hub);
			Volume vol;
			try {
				vol = VolumeMapper.loadVolume("warhub", "", world);
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
			hub.setVolume(vol);
			hub.getVolume().resetBlocks();
			hub.initialize();

			// In the previous job started by the mapper, warzones were created, but their lobbies are missing the war hub gate (because it didn't exist yet)
			for (Warzone zone : War.war.getWarzones()) {
				if (zone.getLobby() != null) {
					zone.getLobby().getVolume().resetBlocks();
					zone.getLobby().initialize();
				}
			}
			War.war.log("Warhub ready.", Level.INFO);
		} else {
			War.war.log("Failed to restore warhub. The specified world (name: " + worldName + ") does not exist!", Level.WARNING);
		}
	}
}
