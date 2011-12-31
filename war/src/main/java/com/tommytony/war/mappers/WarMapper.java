package com.tommytony.war.mappers;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import bukkit.tommytony.war.War;

import com.tommytony.war.FlagReturn;
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
		(War.war.getDataFolder()).mkdir();
		(new File(War.war.getDataFolder().getPath() + "/dat")).mkdir();
		YamlConfiguration warConfig = new YamlConfiguration();
		File config = new File(War.war.getDataFolder().getPath() + "/war.yml");
		try {
			warConfig.load(config);
		} catch (Exception e) {
			War.war.log("Failed to load war.txt file.", Level.WARNING);
			e.printStackTrace();
		}

		// Create file if need be
		boolean newWar = false;
		if (!warConfig.contains("warzones")) {
			newWar = true;
			WarMapper.save();
			War.war.log("war.yml settings file created.", Level.INFO);
			try {
				warConfig.load(config);
			} catch (Exception e) {
				War.war.log("Failed to reload war.yml file after creating it.", Level.WARNING);
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
		
		// defaultExtraLoadouts
		String extraLoadoutStr = warConfig.getString("defaultExtraLoadouts");
		String[] extraLoadoutsSplit = extraLoadoutStr.split(",");
		War.war.getDefaultExtraLoadouts().clear();
		for (String nameStr : extraLoadoutsSplit) {
			if (nameStr != null && !nameStr.equals("")) {
				War.war.getDefaultExtraLoadouts().put(nameStr, new HashMap<Integer, ItemStack>());
			}
		}
		
		for (String name : War.war.getDefaultExtraLoadouts().keySet()) {
			String loadoutStr = warConfig.getString(name + "Loadout");
			String[] loadoutSplit = loadoutStr.split(";");
			HashMap<Integer, ItemStack> loadout = War.war.getDefaultExtraLoadouts().get(name);
			loadout.clear();
			for (String str : loadoutSplit) {
				if (str != null && !str.equals("")) {
					String[] strSplit = str.split(",");
					ItemStack item = new ItemStack(Integer.parseInt(strSplit[0]), Integer.parseInt(strSplit[1]));
					loadout.put(Integer.parseInt(strSplit[2]), item);
				}
			}
		}
		
		// maxZones
		if (warConfig.contains("maxZones")) {
			War.war.setMaxZones(warConfig.getInt("maxZones"));
		}

		// defaultLifePool
		if (warConfig.contains("defaultLifePool")) {
			War.war.setDefaultLifepool(warConfig.getInt("defaultLifePool"));
		}

		// defaultMonumentHeal
		if (warConfig.contains("defaultMonumentHeal")) {
			War.war.setDefaultMonumentHeal(warConfig.getInt("defaultMonumentHeal"));
		}

		// defaultFriendlyFire
		if (warConfig.contains("defaultFriendlyFire")) {
			War.war.setDefaultFriendlyFire(warConfig.getBoolean("defaultFriendlyFire"));
		}

		// defaultAutoAssignOnly
		if (warConfig.contains("defaultAutoAssignOnly")) {
			War.war.setDefaultAutoAssignOnly(warConfig.getBoolean("defaultAutoAssignOnly"));
		}

		// defaultFlagPointsOnly
		if (warConfig.contains("defaultFlagPointsOnly")) {
			War.war.setDefaultFlagPointsOnly(warConfig.getBoolean("defaultFlagPointsOnly"));
		}

		// defaultTeamCap
		if (warConfig.contains("defaultTeamCap")) {
			War.war.setDefaultTeamCap(warConfig.getInt("defaultTeamCap"));
		}

		// defaultScoreCap
		if (warConfig.contains("defaultScoreCap")) {
			War.war.setDefaultScoreCap(warConfig.getInt("defaultScoreCap"));
		}

		// pvpInZonesOnly
		if (warConfig.contains("pvpInZonesOnly")) {
			War.war.setPvpInZonesOnly(warConfig.getBoolean("pvpInZonesOnly"));
		}

		// defaultBlockHeads
		if (warConfig.contains("defaultBlockHeads")) {
			War.war.setDefaultBlockHeads(warConfig.getBoolean("defaultBlockHeads"));
		}

		// buildInZonesOnly
		if (warConfig.contains("buildInZonesOnly")) {
			War.war.setBuildInZonesOnly(warConfig.getBoolean("buildInZonesOnly"));
		}

		// disablePVPMessage
		if (warConfig.contains("disablePvpMessage")) {
			War.war.setDisablePvpMessage(warConfig.getBoolean("disablePvpMessage"));
		}
		
		// tntInZonesOnly
		if (warConfig.contains("tntInZonesOnly")) {
			War.war.setTntInZonesOnly(warConfig.getBoolean("tntInZonesOnly"));
		}

		// defaultSpawnStyle
		String spawnStyle = warConfig.getString("defaultspawnStyle");
		if (spawnStyle != null && !spawnStyle.equals("")) {
			War.war.setDefaultSpawnStyle(TeamSpawnStyle.getStyleFromString(spawnStyle));
		}

		// defaultFlagReturn
		String flagReturn = warConfig.getString("defaultFlagReturn");
		if (flagReturn != null && !flagReturn.equals("")) {
			War.war.setDefaultFlagReturn(FlagReturn.getFromString(flagReturn));
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
		if (warConfig.contains("defaultUnbreakableZoneBlocks")) {
			War.war.setDefaultUnbreakableZoneBlocks(warConfig.getBoolean("defaultUnbreakableZoneBlocks"));
		}

		// defaultNoCreatures
		if (warConfig.contains("defaultNoCreatures")) {
			War.war.setDefaultNoCreatures(warConfig.getBoolean("defaultNoCreatures"));
		}
		
		// defaultGlassWalls
		if (warConfig.contains("defaultGlassWalls")) {
			War.war.setDefaultGlassWalls(warConfig.getBoolean("defaultGlassWalls"));
		}
		
		// defaultPvpInZone
		if (warConfig.contains("defaultPvpInZone")) {
			War.war.setDefaultPvpInZone(warConfig.getBoolean("defaultPvpInZone"));
		}
		
		// defaultInstaBreak
		if (warConfig.contains("defaultInstaBreak")) {
			War.war.setDefaultInstaBreak(warConfig.getBoolean("defaultInstaBreak"));
		}
		
		// defaultNoDrops
		if (warConfig.contains("defaultNoDrops")) {
			War.war.setDefaultNoDrops(warConfig.getBoolean("defaultNoDrops"));
		}
		
		// defaultNoHunger
		if (warConfig.contains("defaultNoHunger")) {
			War.war.setDefaultNoHunger(warConfig.getBoolean("defaultNoHunger"));
		}
		
		// defaultSaturation
		if (warConfig.contains("defaultSaturation")) {
			War.war.setDefaultSaturation(warConfig.getInt("defaultSaturation"));
		}
		
		// defaultMinPlayers
		if (warConfig.contains("defaultMinPlayers")) {
			War.war.setDefaultMinPlayers(warConfig.getInt("defaultMinPlayers"));
		}
		
		// defaultMinTeams
		if (warConfig.contains("defaultMinTeams")) {
			War.war.setDefaultMinTeams(warConfig.getInt("defaultMinTeams"));
		}

		// defaultResetOnEmpty
		if (warConfig.contains("defaultResetOnEmpty")) {
			War.war.setDefaultResetOnEmpty(warConfig.getBoolean("defaultResetOnEmpty"));
		}

		// defaultResetOnLoad
		if (warConfig.contains("defaultResetOnLoad")) {
			War.war.setDefaultResetOnLoad(warConfig.getBoolean("defaultResetOnLoad"));
		}

		// defaultResetOnUnload
		if (warConfig.contains("defaultResetOnUnload")) {
			War.war.setDefaultResetOnUnload(warConfig.getBoolean("defaultResetOnUnload"));
		}

		// warhub
		String hubStr = warConfig.getString("warhub");
		if (hubStr != null && !hubStr.equals("")) {
			RestoreWarhubJob restoreWarhub = new RestoreWarhubJob(hubStr);
			if (War.war.getServer().getScheduler().scheduleSyncDelayedTask(War.war, restoreWarhub) == -1) {
				War.war.log("Failed to schedule warhub-restore job. War hub was not loaded.", Level.WARNING);
			}
		}

	}

	public static void save() {
		YamlConfiguration warConfig = new YamlConfiguration();
		File config = new File(War.war.getDataFolder().getPath() + "/war.yml");
		String warzonesStr = "";

		// warzones
		for (Warzone zone : War.war.getWarzones()) {
			warzonesStr += zone.getName() + ",";
		}
		warConfig.set("warzones", warzonesStr);

		// zone makers: default is none and it means everyone can use /setzone
		String makersStr = ""; // everyone
		for (String name : War.war.getZoneMakerNames()) {
			makersStr += name + ",";
		}
		warConfig.set("zoneMakers", makersStr);

		// whitelisted commands during a game
		String commandWhitelistStr = ""; // everyone
		for (String command : War.war.getCommandWhitelist()) {
			commandWhitelistStr += command + ",";
		}
		warConfig.set("commandWhitelist", commandWhitelistStr);

		// defaultLoadout
		String defaultLoadoutStr = "";
		HashMap<Integer, ItemStack> items = War.war.getDefaultLoadout();
		for (Integer slot : items.keySet()) {
			ItemStack item = items.get(slot);
			if (item != null) {
				defaultLoadoutStr += item.getTypeId() + "," + item.getAmount() + "," + slot + ";";
			}
		}
		warConfig.set("defaultLoadout", defaultLoadoutStr);

		// defaultExtraLoadouts
		String defaultExtraLoadoutsStr = "";
		for (String name : War.war.getDefaultExtraLoadouts().keySet()) {
			defaultExtraLoadoutsStr += name + ",";
			
			String loadoutStr = "";
			HashMap<Integer, ItemStack> loadout = War.war.getDefaultExtraLoadouts().get(name);
			for (Integer slot : loadout.keySet()) {
				ItemStack item = loadout.get(slot);
				if (item != null) {
					loadoutStr += item.getTypeId() + "," + item.getAmount() + "," + slot + ";";
				}
			}
			warConfig.set(name + "Loadout", loadoutStr);
		}
		warConfig.set("defaultExtraLoadouts", defaultExtraLoadoutsStr);
		
		// maxZones
		warConfig.set("maxZones", War.war.getMaxZones());
		
		// defaultLifepool
		warConfig.set("defaultLifePool", War.war.getDefaultLifepool());

		// defaultMonumentHeal
		warConfig.set("defaultMonumentHeal", War.war.getDefaultMonumentHeal());

		// defaultFriendlyFire
		warConfig.set("defaultFriendlyFire", War.war.isDefaultFriendlyFire());

		// defaultAutoAssignOnly
		warConfig.set("defaultAutoAssignOnly", War.war.isDefaultAutoAssignOnly());

		// defaultFlagPointsOnly
		warConfig.set("defaultFlagPointsOnly", War.war.isDefaultFlagPointsOnly());

		// defaultTeamCap
		warConfig.set("defaultTeamCap", War.war.getDefaultTeamCap());

		// defaultScoreCap
		warConfig.set("defaultScoreCap", War.war.getDefaultScoreCap());

		// pvpInZonesOnly
		warConfig.set("pvpInZonesOnly", War.war.isPvpInZonesOnly());

		// defaultBlockHeads
		warConfig.set("defaultBlockHeads", War.war.isDefaultBlockHeads());

		// buildInZonesOnly
		warConfig.set("buildInZonesOnly", War.war.isBuildInZonesOnly());

		// disablePVPMessage
		warConfig.set("disablePvpMessage", War.war.isDisablePvpMessage());

		// tntInZonesOnly
		warConfig.set("tntInZonesOnly", War.war.isTntInZonesOnly());
		
		// spawnStyle
		warConfig.set("spawnStyle", War.war.getDefaultSpawnStyle().toString());

		// spawnStyle
		warConfig.set("flagReturn", War.war.getDefaultFlagReturn().toString());

		// defaultReward
		String defaultRewardStr = "";
		HashMap<Integer, ItemStack> rewardItems = War.war.getDefaultReward();
		for (Integer slot : rewardItems.keySet()) {
			ItemStack item = items.get(slot);
			if (item != null) {
				defaultRewardStr += item.getTypeId() + "," + item.getAmount() + "," + slot + ";";
			}
		}
		warConfig.set("defaultReward", defaultRewardStr);

		// defaultUnbreakableZoneBlocks
		warConfig.set("defaultUnbreakableZoneBlocks", War.war.isDefaultUnbreakableZoneBlocks());

		// defaultNoCreatures
		warConfig.set("defaultNoCreatures", War.war.isDefaultNoCreatures());

		// defaultGlassWalls
		warConfig.set("defaultGlassWalls", War.war.isDefaultGlassWalls());
		
		// defaultPvpInZone
		warConfig.set("defaultPvpInZone", War.war.isDefaultPvpInZone());

		// defaultInstaBreak
		warConfig.set("defaultInstaBreak", War.war.isDefaultInstaBreak());
		
		// defaultNoDrops
		warConfig.set("defaultNoDrops", War.war.isDefaultNoDrops());
		
		// defaultNoHunger
		warConfig.set("defaultNoHunger", War.war.isDefaultNoHunger());
				
		// defaultSaturation
		warConfig.set("defaultSaturation", War.war.getDefaultSaturation());
				
		// defaultMinPlayers
		warConfig.set("defaultMinPlayers", War.war.getDefaultMinPlayers());
		
		// defaultMinTeams
		warConfig.set("defaultMinTeams", War.war.getDefaultMinTeams());

		// defaultResetOnEmpty
		warConfig.set("defaultResetOnEmpty", War.war.isDefaultResetOnEmpty());

		// defaultResetOnLoad
		warConfig.set("defaultResetOnLoad", War.war.isDefaultResetOnLoad());

		// defaultResetOnUnload
		warConfig.set("defaultResetOnUnload", War.war.isDefaultResetOnUnload());

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
		warConfig.set("warhub", hubStr);

		try {
			warConfig.save(config);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
