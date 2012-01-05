package com.tommytony.war.mappers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import bukkit.tommytony.war.War;

import com.tommytony.war.WarHub;
import com.tommytony.war.Warzone;
import com.tommytony.war.jobs.RestoreYmlWarhubJob;
import com.tommytony.war.jobs.RestoreYmlWarzonesJob;

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
			WarYmlMapper.save();
			War.war.log("Converted war.txt to war.yml.", Level.INFO);
		} else if (!warTxtFile.exists() && !warYmlFile.exists()) {
			// Save defaults to disk
			newWar = true;
			WarYmlMapper.save();
			War.war.log("war.yml settings file created.", Level.INFO);
		}
		
		YamlConfiguration warYmlConfig = YamlConfiguration.loadConfiguration(warYmlFile);
		
		// warzones
		List<String> warzones = warYmlConfig.getStringList("war.info.warzones");
		RestoreYmlWarzonesJob restoreWarzones = new RestoreYmlWarzonesJob(warzones, newWar);	// during conversion, this should execute just after the RestoreTxtWarzonesJob
		if (War.war.getServer().getScheduler().scheduleSyncDelayedTask(War.war, restoreWarzones) == -1) {
			War.war.log("Failed to schedule warzone-restore job. No warzone was loaded.", Level.WARNING);
		}

		// zone makers
		List<String> makers = warYmlConfig.getStringList("war.info.zonemakers");
		War.war.getZoneMakerNames().clear();
		for (String makerName : makers) {
			if (makerName != null && !makerName.equals("")) {
				War.war.getZoneMakerNames().add(makerName);
			}
		}

		// command whitelist
		List<String> whitelist = warYmlConfig.getStringList("war.info.commandwhitelist");
		War.war.getCommandWhitelist().clear();
		for (String command : whitelist) {
			if (command != null && !command.equals("")) {
				War.war.getCommandWhitelist().add(command);
			}
		}

		// defaultLoadouts
		ConfigurationSection loadoutsSection = warYmlConfig.getConfigurationSection("team.default.loadout");
		LoadoutYmlMapper.fromConfigToLoadouts(loadoutsSection, War.war.getDefaultInventories().getLoadouts());

		// defaultReward
		ConfigurationSection rewardsSection = warYmlConfig.getConfigurationSection("team.default.reward");
		HashMap<Integer, ItemStack> reward = new HashMap<Integer, ItemStack>();
		LoadoutYmlMapper.fromConfigToLoadout(rewardsSection, reward, "default");
		War.war.getDefaultInventories().setReward(reward);
		
		// War settings
		ConfigurationSection warConfigSection = warYmlConfig.getConfigurationSection("war.config");
		War.war.getWarConfig().loadFrom(warConfigSection);
		
		// Warzone default settings
		ConfigurationSection warzoneConfigSection = warYmlConfig.getConfigurationSection("warzone.default.config");
		War.war.getWarzoneDefaultConfig().loadFrom(warzoneConfigSection);
		
		// Team default settings
		ConfigurationSection teamConfigSection = warYmlConfig.getConfigurationSection("team.default.config");
		War.war.getTeamDefaultConfig().loadFrom(teamConfigSection);

		// warhub
		ConfigurationSection hubConfigSection = warYmlConfig.getConfigurationSection("war.info.warhub");
		if (hubConfigSection != null) {
			RestoreYmlWarhubJob restoreWarhub = new RestoreYmlWarhubJob(hubConfigSection);
			if (War.war.getServer().getScheduler().scheduleSyncDelayedTask(War.war, restoreWarhub) == -1) {
				War.war.log("Failed to schedule warhub-restore job. War hub was not loaded.", Level.WARNING);
			}
		}
	}
	
	public static void save() {
		YamlConfiguration warYmlConfig = new YamlConfiguration();
		(new File(War.war.getDataFolder().getPath())).mkdir();
		(new File(War.war.getDataFolder().getPath() + "/dat")).mkdir();
		
		// warzones
		List<String> warzones = new ArrayList<String>();
		for (Warzone zone : War.war.getWarzones()) {
			warzones.add(zone.getName());
		}		
		warYmlConfig.set("war.info.warzones", warzones);

		// zone makers
		warYmlConfig.set("war.info.zonemakers", War.war.getZoneMakerNames());

		// whitelisted commands during a game
		warYmlConfig.set("war.info.commandwhitelist", War.war.getCommandWhitelist());

		// defaultLoadouts
		ConfigurationSection loadoutsSection = warYmlConfig.createSection("team.default.loadout");
		LoadoutYmlMapper.fromLoadoutsToConfig(War.war.getDefaultInventories().getLoadouts(), loadoutsSection);
		
		// defaultReward
		ConfigurationSection rewardsSection = warYmlConfig.createSection("team.default.reward");
		LoadoutYmlMapper.fromLoadoutToConfig(War.war.getDefaultInventories().getReward(), rewardsSection, "default");
		
		// War settings
		ConfigurationSection warConfigSection = warYmlConfig.createSection("war.config");
		War.war.getWarConfig().saveTo(warConfigSection);
		
		// Warzone default settings
		ConfigurationSection warzoneConfigSection = warYmlConfig.createSection("warzone.default.config");
		War.war.getWarzoneDefaultConfig().saveTo(warzoneConfigSection);
		
		// Team default settings
		ConfigurationSection teamConfigSection = warYmlConfig.createSection("team.default.config");
		War.war.getTeamDefaultConfig().saveTo(teamConfigSection);

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
			
			ConfigurationSection hubConfigSection = warYmlConfig.createSection("war.info.warhub");
			hubConfigSection.set("x", hub.getLocation().getBlockX());
			hubConfigSection.set("y", hub.getLocation().getBlockY());
			hubConfigSection.set("z", hub.getLocation().getBlockZ());
			hubConfigSection.set("world", hub.getLocation().getWorld().getName());
			hubConfigSection.set("orientation", orientationStr);

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
