package com.tommytony.war.mappers;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;

import org.bukkit.inventory.ItemStack;

import bukkit.tommytony.war.War;

import com.tommytony.war.TeamSpawnStyle;
import com.tommytony.war.WarHub;
import com.tommytony.war.Warzone;
import com.tommytony.war.jobs.RestoreWarhubJob;
import com.tommytony.war.jobs.RestoreWarzonesJob;

/**
 * 
 * @author tommytony
 * 
 */
public class WarMapper {

	public static void load() {
		// war.getLogger().info("Loading war config...");
		(War.war.getDataFolder()).mkdir();
		(new File(War.war.getDataFolder().getPath() + "/dat")).mkdir();
		PropertiesFile warConfig = new PropertiesFile(War.war.getDataFolder().getPath() + "/war.txt");
		try {
			warConfig.load();
		} catch (IOException e) {
			War.war.log("Failed to load war.txt file.", Level.WARNING);
			e.printStackTrace();
		}

		// Create file if need be
		boolean newWar = false;
		if (!warConfig.containsKey("warzones")) {
			newWar = true;
			WarMapper.save();
			War.war.log("war.txt settings file created.", Level.INFO);
			try {
				warConfig.load();
			} catch (IOException e) {
				War.war.log("Failed to reload war.txt file after creating it.", Level.WARNING);
				e.printStackTrace();
			}
		}

		// warzones
		String warzonesStr = warConfig.getString("warzones");
		RestoreWarzonesJob restoreWarzones = new RestoreWarzonesJob(warzonesStr, newWar);
		if (War.war.getServer().getScheduler().scheduleSyncDelayedTask(War.war, restoreWarzones) == -1) {
			War.war.log("Failed to schedule warzone-restore job. No warzone was loaded.", Level.WARNING);
		}

		// zone makers
		String[] makers = warConfig.getString("zoneMakers").split(",");
		War.war.getZoneMakerNames().clear();
		for (String makerName : makers) {
			if (makerName != null && !makerName.equals("")) {
				War.war.getZoneMakerNames().add(makerName);
			}
		}

		// command whitelist
		String[] whitelist = warConfig.getString("commandWhitelist").split(",");
		War.war.getCommandWhitelist().clear();
		for (String command : whitelist) {
			if (command != null && !command.equals("")) {
				War.war.getCommandWhitelist().add(command);
			}
		}

		// defaultLoadout
		String defaultLoadoutStr = warConfig.getString("defaultLoadout");
		String[] defaultLoadoutSplit = defaultLoadoutStr.split(";");
		War.war.getDefaultLoadout().clear();
		for (String itemStr : defaultLoadoutSplit) {
			if (itemStr != null && !itemStr.equals("")) {
				String[] itemStrSplit = itemStr.split(",");
				ItemStack item = new ItemStack(Integer.parseInt(itemStrSplit[0]), Integer.parseInt(itemStrSplit[1]));
				War.war.getDefaultLoadout().put(Integer.parseInt(itemStrSplit[2]), item);
			}
		}

		// defaultLifePool
		War.war.setDefaultLifepool(warConfig.getInt("defaultLifePool"));

		// defaultMonumentHeal
		War.war.setDefaultMonumentHeal(warConfig.getInt("defaultMonumentHeal"));

		// defaultFriendlyFire
		War.war.setDefaultFriendlyFire(warConfig.getBoolean("defaultFriendlyFire"));

		// defaultAutoAssignOnly
		War.war.setDefaultAutoAssignOnly(warConfig.getBoolean("defaultAutoAssignOnly"));

		// defaultTeamCap
		War.war.setDefaultTeamCap(warConfig.getInt("defaultTeamCap"));

		// defaultScoreCap
		War.war.setDefaultScoreCap(warConfig.getInt("defaultScoreCap"));

		// pvpInZonesOnly
		War.war.setPvpInZonesOnly(warConfig.getBoolean("pvpInZonesOnly"));

		// defaultBlockHeads
		War.war.setDefaultBlockHeads(warConfig.getBoolean("defaultBlockHeads"));

		// buildInZonesOnly
		War.war.setBuildInZonesOnly(warConfig.getBoolean("buildInZonesOnly"));

		// disablePVPMessage
		War.war.setDisablePvpMessage(warConfig.getBoolean("disablePvpMessage"));

		// defaultSpawnStyle
		String spawnStyle = warConfig.getString("defaultspawnStyle");
		if (spawnStyle != null && !spawnStyle.equals("")) {
			War.war.setDefaultSpawnStyle(TeamSpawnStyle.getStyleByString(spawnStyle));
		}

		// defaultFlagReturn
		String flagReturn = warConfig.getString("defaultFlagReturn");
		if (flagReturn != null && !flagReturn.equals("")) {
			flagReturn = flagReturn.toLowerCase();
			if (flagReturn.equals("flag")) {
				War.war.setDefaultFlagReturn("flag");
			} else if (flagReturn.equals("spawn")) {
				War.war.setDefaultFlagReturn("spawn");
			}
			// default is already initialized to both (see Warzone)
		}

		// defaultReward
		String defaultRewardStr = warConfig.getString("defaultReward");
		if (defaultRewardStr != null && !defaultRewardStr.equals("")) {
			String[] defaultRewardStrSplit = defaultRewardStr.split(";");
			War.war.getDefaultReward().clear();
			for (String itemStr : defaultRewardStrSplit) {
				if (itemStr != null && !itemStr.equals("")) {
					String[] itemStrSplit = itemStr.split(",");
					ItemStack item = new ItemStack(Integer.parseInt(itemStrSplit[0]), Integer.parseInt(itemStrSplit[1]));
					War.war.getDefaultReward().put(Integer.parseInt(itemStrSplit[2]), item);
				}
			}
		}

		// defaultUnbreakableZoneBlocks
		War.war.setDefaultUnbreakableZoneBlocks(warConfig.getBoolean("defaultUnbreakableZoneBlocks"));

		// defaultNoCreatures
		War.war.setDefaultNoCreatures(warConfig.getBoolean("defaultNoCreatures"));

		// defaultResetOnEmpty
		War.war.setDefaultResetOnEmpty(warConfig.getBoolean("defaultResetOnEmpty"));

		// defaultResetOnLoad
		War.war.setDefaultResetOnLoad(warConfig.getBoolean("defaultResetOnLoad"));

		// defaultResetOnUnload
		War.war.setDefaultResetOnUnload(warConfig.getBoolean("defaultResetOnUnload"));

		// warhub
		String hubStr = warConfig.getString("warhub");
		if (hubStr != null && !hubStr.equals("")) {
			RestoreWarhubJob restoreWarhub = new RestoreWarhubJob(hubStr);
			if (War.war.getServer().getScheduler().scheduleSyncDelayedTask(War.war, restoreWarhub) == -1) {
				War.war.log("Failed to schedule warhub-restore job. War hub was not loaded.", Level.WARNING);
			}
		}

		warConfig.close();
	}

	public static void save() {
		PropertiesFile warConfig = new PropertiesFile(War.war.getDataFolder().getPath() + "/war.txt");
		String warzonesStr = "";

		// warzones
		for (Warzone zone : War.war.getWarzones()) {
			warzonesStr += zone.getName() + ",";
		}
		warConfig.setString("warzones", warzonesStr);

		// zone makers: default is none and it means everyone can use /setzone
		String makersStr = ""; // everyone
		for (String name : War.war.getZoneMakerNames()) {
			makersStr += name + ",";
		}
		warConfig.setString("zoneMakers", makersStr);

		// whitelisted commands during a game
		String commandWhitelistStr = ""; // everyone
		for (String command : War.war.getCommandWhitelist()) {
			commandWhitelistStr += command + ",";
		}
		warConfig.setString("commandWhitelist", commandWhitelistStr);

		// defaultLoadout
		String defaultLoadoutStr = "";
		HashMap<Integer, ItemStack> items = War.war.getDefaultLoadout();
		for (Integer slot : items.keySet()) {
			ItemStack item = items.get(slot);
			if (item != null) {
				defaultLoadoutStr += item.getTypeId() + "," + item.getAmount() + "," + slot + ";";
			}
		}
		warConfig.setString("defaultLoadout", defaultLoadoutStr);

		// defaultLifepool
		warConfig.setInt("defaultLifePool", War.war.getDefaultLifepool());

		// defaultMonumentHeal
		warConfig.setInt("defaultMonumentHeal", War.war.getDefaultMonumentHeal());

		// defaultFriendlyFire
		warConfig.setBoolean("defaultFriendlyFire", War.war.isDefaultFriendlyFire());

		// defaultAutoAssignOnly
		warConfig.setBoolean("defaultAutoAssignOnly", War.war.isDefaultAutoAssignOnly());

		// defaultTeamCap
		warConfig.setInt("defaultTeamCap", War.war.getDefaultTeamCap());

		// defaultScoreCap
		warConfig.setInt("defaultScoreCap", War.war.getDefaultScoreCap());

		// pvpInZonesOnly
		warConfig.setBoolean("pvpInZonesOnly", War.war.isPvpInZonesOnly());

		// defaultBlockHeads
		warConfig.setBoolean("defaultBlockHeads", War.war.isDefaultBlockHeads());

		// buildInZonesOnly
		warConfig.setBoolean("buildInZonesOnly", War.war.isBuildInZonesOnly());

		// disablePVPMessage
		warConfig.setBoolean("disablePvpMessage", War.war.isDisablePvpMessage());

		// spawnStyle
		warConfig.setString("spawnStyle", War.war.getDefaultSpawnStyle().toString());

		// defaultReward
		String defaultRewardStr = "";
		HashMap<Integer, ItemStack> rewardItems = War.war.getDefaultReward();
		for (Integer slot : rewardItems.keySet()) {
			ItemStack item = items.get(slot);
			if (item != null) {
				defaultRewardStr += item.getTypeId() + "," + item.getAmount() + "," + slot + ";";
			}
		}
		warConfig.setString("defaultReward", defaultRewardStr);

		// defaultUnbreakableZoneBlocks
		warConfig.setBoolean("defaultUnbreakableZoneBlocks", War.war.isDefaultUnbreakableZoneBlocks());

		// defaultNoCreatures
		warConfig.setBoolean("defaultNoCreatures", War.war.isDefaultNoCreatures());

		// defaultResetOnEmpty
		warConfig.setBoolean("defaultResetOnEmpty", War.war.isDefaultResetOnEmpty());

		// defaultResetOnLoad
		warConfig.setBoolean("defaultResetOnLoad", War.war.isDefaultResetOnLoad());

		// defaultResetOnUnload
		warConfig.setBoolean("defaultResetOnUnload", War.war.isDefaultResetOnUnload());

		// defaultDropLootOnDeath
		// warConfig.setBoolean("defaultDropLootOnDeath", war.isDefaultDropLootOnDeath());

		// warhub
		String hubStr = "";
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
			hubStr = hub.getLocation().getBlockX() + "," + hub.getLocation().getBlockY() + "," + hub.getLocation().getBlockZ() + ","
					+ hub.getLocation().getWorld().getName() + "," + orientationStr;
			VolumeMapper.save(hub.getVolume(), "");
		}
		warConfig.setString("warhub", hubStr);

		warConfig.save();
		warConfig.close();
	}
}
