package com.tommytony.war.mappers;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;

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
		if (warConfig.keyExists("maxZones")) {
			War.war.setMaxZones(warConfig.getInt("maxZones"));
		}

		// defaultLifePool
		if (warConfig.keyExists("defaultLifePool")) {
			War.war.setDefaultLifepool(warConfig.getInt("defaultLifePool"));
		}

		// defaultMonumentHeal
		if (warConfig.keyExists("defaultMonumentHeal")) {
			War.war.setDefaultMonumentHeal(warConfig.getInt("defaultMonumentHeal"));
		}

		// defaultFriendlyFire
		if (warConfig.keyExists("defaultFriendlyFire")) {
			War.war.setDefaultFriendlyFire(warConfig.getBoolean("defaultFriendlyFire"));
		}

		// defaultAutoAssignOnly
		if (warConfig.keyExists("defaultAutoAssignOnly")) {
			War.war.setDefaultAutoAssignOnly(warConfig.getBoolean("defaultAutoAssignOnly"));
		}

		// defaultFlagPointsOnly
		if (warConfig.keyExists("defaultFlagPointsOnly")) {
			War.war.setDefaultFlagPointsOnly(warConfig.getBoolean("defaultFlagPointsOnly"));
		}

		// defaultTeamCap
		if (warConfig.keyExists("defaultTeamCap")) {
			War.war.setDefaultTeamCap(warConfig.getInt("defaultTeamCap"));
		}

		// defaultScoreCap
		if (warConfig.keyExists("defaultScoreCap")) {
			War.war.setDefaultScoreCap(warConfig.getInt("defaultScoreCap"));
		}

		// pvpInZonesOnly
		if (warConfig.keyExists("pvpInZonesOnly")) {
			War.war.setPvpInZonesOnly(warConfig.getBoolean("pvpInZonesOnly"));
		}

		// defaultBlockHeads
		if (warConfig.keyExists("defaultBlockHeads")) {
			War.war.setDefaultBlockHeads(warConfig.getBoolean("defaultBlockHeads"));
		}

		// buildInZonesOnly
		if (warConfig.keyExists("buildInZonesOnly")) {
			War.war.setBuildInZonesOnly(warConfig.getBoolean("buildInZonesOnly"));
		}

		// disablePVPMessage
		if (warConfig.keyExists("disablePvpMessage")) {
			War.war.setDisablePvpMessage(warConfig.getBoolean("disablePvpMessage"));
		}
		
		// tntInZonesOnly
		if (warConfig.keyExists("tntInZonesOnly")) {
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
		if (warConfig.keyExists("defaultUnbreakableZoneBlocks")) {
			War.war.setDefaultUnbreakableZoneBlocks(warConfig.getBoolean("defaultUnbreakableZoneBlocks"));
		}

		// defaultNoCreatures
		if (warConfig.keyExists("defaultNoCreatures")) {
			War.war.setDefaultNoCreatures(warConfig.getBoolean("defaultNoCreatures"));
		}
		
		// defaultGlassWalls
		if (warConfig.keyExists("defaultGlassWalls")) {
			War.war.setDefaultGlassWalls(warConfig.getBoolean("defaultGlassWalls"));
		}
		
		// defaultPvpInZone
		if (warConfig.keyExists("defaultPvpInZone")) {
			War.war.setDefaultPvpInZone(warConfig.getBoolean("defaultPvpInZone"));
		}
		
		// defaultInstaBreak
		if (warConfig.keyExists("defaultInstaBreak")) {
			War.war.setDefaultInstaBreak(warConfig.getBoolean("defaultInstaBreak"));
		}
		
		// defaultNoDrops
		if (warConfig.keyExists("defaultNoDrops")) {
			War.war.setDefaultNoDrops(warConfig.getBoolean("defaultNoDrops"));
		}
		
		// defaultNoHunger
		if (warConfig.keyExists("defaultNoHunger")) {
			War.war.setDefaultNoHunger(warConfig.getBoolean("defaultNoHunger"));
		}
		
		// defaultSaturation
		if (warConfig.keyExists("defaultSaturation")) {
			War.war.setDefaultSaturation(warConfig.getInt("defaultSaturation"));
		}
		
		// defaultMinPlayers
		if (warConfig.keyExists("defaultMinPlayers")) {
			War.war.setDefaultMinPlayers(warConfig.getInt("defaultMinPlayers"));
		}
		
		// defaultMinTeams
		if (warConfig.keyExists("defaultMinTeams")) {
			War.war.setDefaultMinTeams(warConfig.getInt("defaultMinTeams"));
		}

		// defaultResetOnEmpty
		if (warConfig.keyExists("defaultResetOnEmpty")) {
			War.war.setDefaultResetOnEmpty(warConfig.getBoolean("defaultResetOnEmpty"));
		}

		// defaultResetOnLoad
		if (warConfig.keyExists("defaultResetOnLoad")) {
			War.war.setDefaultResetOnLoad(warConfig.getBoolean("defaultResetOnLoad"));
		}

		// defaultResetOnUnload
		if (warConfig.keyExists("defaultResetOnUnload")) {
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
			warConfig.setString(name + "Loadout", loadoutStr);
		}
		warConfig.setString("defaultExtraLoadouts", defaultExtraLoadoutsStr);
		
		// maxZones
		warConfig.setInt("maxZones", War.war.getMaxZones());
		
		// defaultLifepool
		warConfig.setInt("defaultLifePool", War.war.getDefaultLifepool());

		// defaultMonumentHeal
		warConfig.setInt("defaultMonumentHeal", War.war.getDefaultMonumentHeal());

		// defaultFriendlyFire
		warConfig.setBoolean("defaultFriendlyFire", War.war.isDefaultFriendlyFire());

		// defaultAutoAssignOnly
		warConfig.setBoolean("defaultAutoAssignOnly", War.war.isDefaultAutoAssignOnly());

		// defaultFlagPointsOnly
		warConfig.setBoolean("defaultFlagPointsOnly", War.war.isDefaultFlagPointsOnly());

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

		// tntInZonesOnly
		warConfig.setBoolean("tntInZonesOnly", War.war.isTntInZonesOnly());
		
		// spawnStyle
		warConfig.setString("spawnStyle", War.war.getDefaultSpawnStyle().toString());

		// spawnStyle
		warConfig.setString("flagReturn", War.war.getDefaultFlagReturn().toString());

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

		// defaultGlassWalls
		warConfig.setBoolean("defaultGlassWalls", War.war.isDefaultGlassWalls());
		
		// defaultPvpInZone
		warConfig.setBoolean("defaultPvpInZone", War.war.isDefaultPvpInZone());

		// defaultInstaBreak
		warConfig.setBoolean("defaultInstaBreak", War.war.isDefaultInstaBreak());
		
		// defaultNoDrops
		warConfig.setBoolean("defaultNoDrops", War.war.isDefaultNoDrops());
		
		// defaultNoHunger
		warConfig.setBoolean("defaultNoHunger", War.war.isDefaultNoHunger());
				
		// defaultSaturation
		warConfig.setInt("defaultSaturation", War.war.getDefaultSaturation());
				
		// defaultMinPlayers
		warConfig.setInt("defaultMinPlayers", War.war.getDefaultMinPlayers());
		
		// defaultMinTeams
		warConfig.setInt("defaultMinTeams", War.war.getDefaultMinTeams());

		// defaultResetOnEmpty
		warConfig.setBoolean("defaultResetOnEmpty", War.war.isDefaultResetOnEmpty());

		// defaultResetOnLoad
		warConfig.setBoolean("defaultResetOnLoad", War.war.isDefaultResetOnLoad());

		// defaultResetOnUnload
		warConfig.setBoolean("defaultResetOnUnload", War.war.isDefaultResetOnUnload());

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
