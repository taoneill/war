package com.tommytony.war.jobs;

import org.bukkit.Location;
import org.bukkit.World;

import bukkit.tommytony.war.War;

import com.tommytony.war.WarHub;
import com.tommytony.war.Warzone;
import com.tommytony.war.mappers.VolumeMapper;
import com.tommytony.war.volumes.Volume;

public class RestoreWarhubJob implements Runnable {

	private final War war;
	private final String hubStr;

	public RestoreWarhubJob(War war, String hubStr) {
		this.war = war;
		this.hubStr = hubStr;
	}

	public void run() {
		String[] hubStrSplit = hubStr.split(",");
		
		int hubX = Integer.parseInt(hubStrSplit[0]);
		int hubY = Integer.parseInt(hubStrSplit[1]);
		int hubZ = Integer.parseInt(hubStrSplit[2]);
		World world = null;
		String worldName;
		if (hubStrSplit.length > 3) {
			worldName = hubStrSplit[3];
			world = war.getServer().getWorld(worldName);
		} else {
			worldName = "DEFAULT";
			world = war.getServer().getWorlds().get(0);		// default to first world
		}
		if (world != null) {
			Location hubLocation = new Location(world, hubX, hubY, hubZ);
			WarHub hub = new WarHub(war, hubLocation);
			war.setWarHub(hub);
			Volume vol = VolumeMapper.loadVolume("warhub", "", war, world);
			hub.setVolume(vol);
			hub.getVolume().resetBlocks();
			hub.initialize();
			
			// In the previous job started by the mapper, warzones were created, but their lobbies are missing the war hub gate (because it didn't exist yet)
			for (Warzone zone : war.getWarzones()) {
				if (zone.getLobby() != null) {
					zone.getLobby().getVolume().resetBlocks();
					zone.getLobby().initialize();
				}
			}
			war.logInfo("Warhub ready.");
		} else {
			war.logWarn("Failed to restore warhub. The specified world (name: " + worldName + ") does not exist!");
		}
	}
}
