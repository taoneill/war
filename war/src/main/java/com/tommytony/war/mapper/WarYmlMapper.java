package com.tommytony.war.mapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;


import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import com.tommytony.war.job.RestoreYmlWarhubJob;
import com.tommytony.war.job.RestoreYmlWarzonesJob;
import com.tommytony.war.structure.WarHub;

public class WarYmlMapper {

	public static void load() {
		(War.war.getDataFolder()).mkdir();
		(new File(War.war.getDataFolder().getPath() + "/dat")).mkdir();
		File warTxtFile = new File(War.war.getDataFolder().getPath() + "/war.txt");
		File warYmlFile = new File(War.war.getDataFolder().getPath() + "/war.yml");
		
		boolean newWar = false;
		if (warTxtFile.exists() && !warYmlFile.exists()) {
			// Load both War and warzones (with delay) in old format, save War to new format immediatly
			WarTxtMapper.load(true);
			// Warzones are getting loaded by TxtMapper launched job. That job will in turn save the Warzones to their new format.
			return;
		} else if (!warTxtFile.exists() && !warYmlFile.exists()) {
			// Save defaults to disk
			newWar = true;
			WarYmlMapper.save();
			War.war.log("war.yml settings file created.", Level.INFO);
		}
		
		YamlConfiguration warYmlConfig = YamlConfiguration.loadConfiguration(warYmlFile);
		ConfigurationSection warRootSection = warYmlConfig.getConfigurationSection("set");
		
		// warzones
		List<String> warzones = warRootSection.getStringList("war.info.warzones");
		RestoreYmlWarzonesJob restoreWarzones = new RestoreYmlWarzonesJob(warzones, newWar);	// during conversion, this should execute just after the RestoreTxtWarzonesJob
		if (War.war.getServer().getScheduler().scheduleSyncDelayedTask(War.war, restoreWarzones) == -1) {
			War.war.log("Failed to schedule warzone-restore job. No warzone was loaded.", Level.WARNING);
		}

		// zone makers
		List<String> makers = warRootSection.getStringList("war.info.zonemakers");
		War.war.getZoneMakerNames().clear();
		for (String makerName : makers) {
			if (makerName != null && !makerName.equals("")) {
				War.war.getZoneMakerNames().add(makerName);
			}
		}

		// command whitelist
		List<String> whitelist = warRootSection.getStringList("war.info.commandwhitelist");
		War.war.getCommandWhitelist().clear();
		for (String command : whitelist) {
			if (command != null && !command.equals("")) {
				War.war.getCommandWhitelist().add(command);
			}
		}
		
		// defaultLoadouts
		ConfigurationSection loadoutsSection = warRootSection.getConfigurationSection("team.default.loadout");
		LoadoutYmlMapper.fromConfigToLoadouts(loadoutsSection, War.war.getDefaultInventories().getLoadouts());

		// defaultReward
		ConfigurationSection rewardsSection = warRootSection.getConfigurationSection("team.default.reward");
		HashMap<Integer, ItemStack> reward = new HashMap<Integer, ItemStack>();
		LoadoutYmlMapper.fromConfigToLoadout(rewardsSection, reward, "default");
		War.war.getDefaultInventories().setReward(reward);
		
		// War settings
		ConfigurationSection warConfigSection = warRootSection.getConfigurationSection("war.config");
		War.war.getWarConfig().loadFrom(warConfigSection);
		
		// Warzone default settings
		ConfigurationSection warzoneConfigSection = warRootSection.getConfigurationSection("warzone.default.config");
		War.war.getWarzoneDefaultConfig().loadFrom(warzoneConfigSection);
		
		// Team default settings
		ConfigurationSection teamConfigSection = warRootSection.getConfigurationSection("team.default.config");
		War.war.getTeamDefaultConfig().loadFrom(teamConfigSection);

		// warhub
		ConfigurationSection hubConfigSection = warRootSection.getConfigurationSection("war.info.warhub");
		if (hubConfigSection != null) {
			RestoreYmlWarhubJob restoreWarhub = new RestoreYmlWarhubJob(hubConfigSection);
			if (War.war.getServer().getScheduler().scheduleSyncDelayedTask(War.war, restoreWarhub, 20) == -1) {
				War.war.log("Failed to schedule warhub-restore job. War hub was not loaded.", Level.WARNING);
			}
		}
	}
	
	public static void save() {
		YamlConfiguration warYmlConfig = new YamlConfiguration();
		ConfigurationSection warRootSection = warYmlConfig.createSection("set");
		(new File(War.war.getDataFolder().getPath())).mkdir();
		(new File(War.war.getDataFolder().getPath() + "/dat")).mkdir();

		// War settings
		ConfigurationSection warConfigSection = warRootSection.createSection("war.config");
		War.war.getWarConfig().saveTo(warConfigSection);
		
		// Warzone default settings
		ConfigurationSection warzoneConfigSection = warRootSection.createSection("warzone.default.config");
		War.war.getWarzoneDefaultConfig().saveTo(warzoneConfigSection);
		
		// Team default settings
		ConfigurationSection teamDefault = warRootSection.createSection("team.default");
		ConfigurationSection teamConfigSection = teamDefault.createSection("config");
		War.war.getTeamDefaultConfig().saveTo(teamConfigSection);
		
		// defaultLoadouts
		ConfigurationSection loadoutSection = teamDefault.createSection("loadout");
		LoadoutYmlMapper.fromLoadoutsToConfig(War.war.getDefaultInventories().getLoadouts(), loadoutSection);
				
		// defaultReward
		ConfigurationSection rewardsSection = teamDefault.createSection("reward");
		LoadoutYmlMapper.fromLoadoutToConfig("default", War.war.getDefaultInventories().getReward(), rewardsSection);

		ConfigurationSection warInfoSection = warRootSection.createSection("war.info");
		
		// warzones
		List<String> warzones = new ArrayList<String>();
		for (Warzone zone : War.war.getWarzones()) {
			warzones.add(zone.getName());
		}		
		warInfoSection.set("warzones", warzones);
		
		// zone makers
		warInfoSection.set("zonemakers", War.war.getZoneMakerNames());
		
		// whitelisted commands during a game
		warInfoSection.set("commandwhitelist", War.war.getCommandWhitelist());
		
		// warhub
		WarHub hub = War.war.getWarHub();
		if (hub != null) {
			String orientationStr = "";
			switch (hub.getOrientation()) {
				case SOUTH:
					orientationStr = "south";
					break;
				case EAST:
					orientationStr = "east";
					break;
				case NORTH:
					orientationStr = "north";
					break;
				case WEST:
				default:
					orientationStr = "west";
					break;
			}
			
			ConfigurationSection hubConfigSection = warInfoSection.createSection("warhub");
			hubConfigSection.set("x", hub.getLocation().getBlockX());
			hubConfigSection.set("y", hub.getLocation().getBlockY());
			hubConfigSection.set("z", hub.getLocation().getBlockZ());
			hubConfigSection.set("world", hub.getLocation().getWorld().getName());
			hubConfigSection.set("orientation", orientationStr);
			
			ConfigurationSection floorSection = hubConfigSection.createSection("materials.floor");
			floorSection.set("id", War.war.getWarhubMaterials().getFloorId());
			floorSection.set("data", War.war.getWarhubMaterials().getFloorData());
			ConfigurationSection outlineSection = hubConfigSection.createSection("materials.outline");
			outlineSection.set("id", War.war.getWarhubMaterials().getOutlineId());
			outlineSection.set("data", War.war.getWarhubMaterials().getOutlineData());
			ConfigurationSection gateSection = hubConfigSection.createSection("materials.gate");
			gateSection.set("id", War.war.getWarhubMaterials().getGateId());
			gateSection.set("data", War.war.getWarhubMaterials().getGateData());
			ConfigurationSection lightSection = hubConfigSection.createSection("materials.light");
			lightSection.set("id", War.war.getWarhubMaterials().getLightId());
			lightSection.set("data", War.war.getWarhubMaterials().getLightData());

			VolumeMapper.save(hub.getVolume(), "");
		}

		// Save to disk
		File warConfigFile = new File(War.war.getDataFolder().getPath() + "/war.yml");
		try {
			warYmlConfig.save(warConfigFile);
		} catch (IOException e) {
			War.war.log("Failed to save war.yml", Level.WARNING);
			e.printStackTrace();
		}
	}
}
