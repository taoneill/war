package com.tommytony.war.job;

import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import com.tommytony.war.mapper.VolumeMapper;
import com.tommytony.war.structure.WarHub;
import com.tommytony.war.structure.HubLobbyMaterials;
import com.tommytony.war.volume.Volume;

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
		int floorId = 20;	// default glass
		int floorData = 0;	
		ConfigurationSection floorMaterialSection = warhubConfig.getConfigurationSection("materials.floor");
		if (floorMaterialSection != null) {
			floorId = floorMaterialSection.getInt("id");
			floorData = floorMaterialSection.getInt("data");
		}
		
		int outlineId = 5;	// default planks
		int outlineData = 0;	
		ConfigurationSection outlineMaterialSection = warhubConfig.getConfigurationSection("materials.outline");
		if (outlineMaterialSection != null) {
			outlineId = outlineMaterialSection.getInt("id");
			outlineData = outlineMaterialSection.getInt("data");
		}
		
		int gateId = 49;	// default obsidian
		int gateData = 0;	
		ConfigurationSection gateMaterialSection = warhubConfig.getConfigurationSection("materials.gate");
		if (gateMaterialSection != null) {
			gateId = gateMaterialSection.getInt("id");
			gateData = gateMaterialSection.getInt("data");
		}
		
		int lightId = 89;	// default glowstone
		int lightData = 0;	
		ConfigurationSection lightMaterialSection = warhubConfig.getConfigurationSection("materials.light");
		if (lightMaterialSection != null) {
			lightId = lightMaterialSection.getInt("id");
			lightData = lightMaterialSection.getInt("data");
		}
		
		War.war.setWarhubMaterials(new HubLobbyMaterials(floorId, (byte)floorData, outlineId, (byte)outlineData, gateId, (byte)gateData, lightId, (byte)lightData));

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
