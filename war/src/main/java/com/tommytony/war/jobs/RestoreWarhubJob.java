package com.tommytony.war.jobs;

import java.util.logging.Level;

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
		String[] hubStrSplit = this.hubStr.split(",");

		int hubX = Integer.parseInt(hubStrSplit[0]);
		int hubY = Integer.parseInt(hubStrSplit[1]);
		int hubZ = Integer.parseInt(hubStrSplit[2]);
		World world = null;
		String worldName;
		String hubOrientation = "west";
		if (hubStrSplit.length > 3) {
			worldName = hubStrSplit[3];
			world = this.war.getServer().getWorld(worldName);
			if(hubStrSplit.length > 4) {
				hubOrientation = hubStrSplit[4];
			}
		} else {
			worldName = "DEFAULT";
			world = this.war.getServer().getWorlds().get(0); // default to first world
		}
		if (world != null) {
			Location hubLocation = new Location(world, hubX, hubY, hubZ);
			WarHub hub = new WarHub(this.war, hubLocation, hubOrientation);
			this.war.setWarHub(hub);
			Volume vol = VolumeMapper.loadVolume("warhub", "", this.war, world);
			hub.setVolume(vol);
			hub.getVolume().resetBlocks();
			hub.initialize();

			// In the previous job started by the mapper, warzones were created, but their lobbies are missing the war hub gate (because it didn't exist yet)
			for (Warzone zone : this.war.getWarzones()) {
				if (zone.getLobby() != null) {
					zone.getLobby().getVolume().resetBlocks();
					zone.getLobby().initialize();
				}
			}
			this.war.log("Warhub ready.", Level.INFO);
		} else {
			this.war.log("Failed to restore warhub. The specified world (name: " + worldName + ") does not exist!", Level.WARNING);
		}
	}
}
