package com.tommytony.war.jobs;

import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import bukkit.tommytony.war.War;

import com.tommytony.war.WarHub;
import com.tommytony.war.Warzone;
import com.tommytony.war.mappers.VolumeMapper;
import com.tommytony.war.volumes.Volume;

public class RestoreYmlWarhubJob implements Runnable {

	private final ConfigurationSection warhubConfig;

	public RestoreYmlWarhubJob(ConfigurationSection warhubConfig) {
		this.warhubConfig = warhubConfig;
	}

	public void run() {
		int hubX = warhubConfig.getInt("x");
		int hubY = warhubConfig.getInt("y");
		int hubZ = warhubConfig.getInt("z");

		String worldName = warhubConfig.getString("world");
		String hubOrientation = warhubConfig.getString("orientation");

		World world = War.war.getServer().getWorld(worldName);
		if (world != null) {
			Location hubLocation = new Location(world, hubX, hubY, hubZ);
			WarHub hub = new WarHub(hubLocation, hubOrientation);
			War.war.setWarHub(hub);
			Volume vol = VolumeMapper.loadVolume("warhub", "", world);
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
