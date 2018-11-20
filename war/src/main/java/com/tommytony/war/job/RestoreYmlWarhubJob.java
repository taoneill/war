package com.tommytony.war.job;

import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import com.tommytony.war.mapper.VolumeMapper;
import com.tommytony.war.structure.WarHub;
import com.tommytony.war.volume.Volume;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.sql.SQLException;
import java.util.logging.Level;

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
		
		// materials
		if (warhubConfig.isItemStack("materials.floor")) {
			War.war.getWarhubMaterials().setFloorBlock(
					warhubConfig.getItemStack("materials.floor"));
		}
		if (warhubConfig.isItemStack("materials.outline")) {
			War.war.getWarhubMaterials().setOutlineBlock(
					warhubConfig.getItemStack("materials.outline"));
		}
		if (warhubConfig.isItemStack("materials.gate")) {
			War.war.getWarhubMaterials().setGateBlock(
					warhubConfig.getItemStack("materials.gate"));
		}
		if (warhubConfig.isItemStack("materials.light")) {
			War.war.getWarhubMaterials().setLightBlock(
					warhubConfig.getItemStack("materials.light"));
		}
		World world = War.war.getServer().getWorld(worldName);
		if (world != null) {
			Location hubLocation = new Location(world, hubX, hubY, hubZ);
			WarHub hub = new WarHub(hubLocation, hubOrientation);
			War.war.setWarHub(hub);
			Volume vol;
			try {
				vol = VolumeMapper.loadSimpleVolume("warhub", world);
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
