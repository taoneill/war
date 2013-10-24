package com.tommytony.war.job;

import java.sql.SQLException;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import com.tommytony.war.mapper.VolumeMapper;
import com.tommytony.war.structure.WarHub;
import com.tommytony.war.volume.Volume;

public class RestoreYmlWarhubJob implements Runnable {

	private final ConfigurationSection warhubConfig;

	public RestoreYmlWarhubJob(ConfigurationSection warhubConfig) {
		this.warhubConfig = warhubConfig;
	}

	@SuppressWarnings("deprecation")
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
		} else {
			ConfigurationSection floorMaterialSection = warhubConfig
					.getConfigurationSection("materials.floor");
			if (floorMaterialSection != null) {
				War.war.getWarhubMaterials().setFloorBlock(
					new ItemStack(floorMaterialSection.getInt("id"), 1,
						(short) floorMaterialSection.getInt("data")));
			}
		}
		if (warhubConfig.isItemStack("materials.outline")) {
			War.war.getWarhubMaterials().setOutlineBlock(
					warhubConfig.getItemStack("materials.outline"));
		} else {
			ConfigurationSection floorMaterialSection = warhubConfig
					.getConfigurationSection("materials.outline");
			if (floorMaterialSection != null) {
				War.war.getWarhubMaterials().setOutlineBlock(
					new ItemStack(floorMaterialSection.getInt("id"), 1,
						(short) floorMaterialSection.getInt("data")));
			}
		}
		if (warhubConfig.isItemStack("materials.gate")) {
			War.war.getWarhubMaterials().setGateBlock(
					warhubConfig.getItemStack("materials.gate"));
		} else {
			ConfigurationSection floorMaterialSection = warhubConfig
					.getConfigurationSection("materials.gate");
			if (floorMaterialSection != null) {
				War.war.getWarhubMaterials().setGateBlock(
					new ItemStack(floorMaterialSection.getInt("id"), 1,
						(short) floorMaterialSection.getInt("data")));
			}
		}
		if (warhubConfig.isItemStack("materials.light")) {
			War.war.getWarhubMaterials().setLightBlock(
					warhubConfig.getItemStack("materials.light"));
		} else {
			ConfigurationSection floorMaterialSection = warhubConfig
					.getConfigurationSection("materials.light");
			if (floorMaterialSection != null) {
				War.war.getWarhubMaterials().setLightBlock(
					new ItemStack(floorMaterialSection.getInt("id"), 1,
						(short) floorMaterialSection.getInt("data")));
			}
		}
		World world = War.war.getServer().getWorld(worldName);
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
