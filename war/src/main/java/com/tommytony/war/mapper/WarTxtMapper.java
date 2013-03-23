package com.tommytony.war.mapper;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;

import org.bukkit.inventory.ItemStack;


import com.tommytony.war.War;
import com.tommytony.war.config.FlagReturn;
import com.tommytony.war.config.TeamConfig;
import com.tommytony.war.config.TeamSpawnStyle;
import com.tommytony.war.config.WarConfig;
import com.tommytony.war.config.WarzoneConfig;
import com.tommytony.war.job.RestoreWarhubJob;
import com.tommytony.war.job.RestoreWarzonesJob;

/**
 *
 * @author tommytony
 *
 */
public class WarTxtMapper {

	public static void load() {
		load(false);
	}
	
	public static void load(boolean convertingToYml) {
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
			WarTxtMapper.save();
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
		RestoreWarzonesJob restoreWarzones = new RestoreWarzonesJob(warzonesStr, newWar, convertingToYml);
		// make sure warhub job is over before this one ends, because this job will launch conversion (which needs the warhub)
		if (War.war.getServer().getScheduler().scheduleSyncDelayedTask(War.war, restoreWarzones, 20) == -1) {
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
		War.war.getDefaultInventories().clearLoadouts();
		
		String loadoutStr = warConfig.getString("defaultLoadout");
		if (loadoutStr != null && !loadoutStr.equals("")) {
			War.war.getDefaultInventories().addLoadout("default", new HashMap<Integer, ItemStack>());
			LoadoutTxtMapper.fromStringToLoadout(loadoutStr, War.war.getDefaultInventories().getLoadout("default"));
		}
		
		// defaultExtraLoadouts
		String extraLoadoutStr = warConfig.getString("defaultExtraLoadouts");
		String[] extraLoadoutsSplit = extraLoadoutStr.split(",");
		
		for (String nameStr : extraLoadoutsSplit) {
			if (nameStr != null && !nameStr.equals("")) {
				War.war.getDefaultInventories().addLoadout(nameStr, new HashMap<Integer, ItemStack>());
			}
		}
		
		for (String extraName : extraLoadoutsSplit) {
			if (extraName != null && !extraName.equals("")) {
				String loadoutString = warConfig.getString(extraName + "Loadout");
				HashMap<Integer, ItemStack> loadout = War.war.getDefaultInventories().getLoadout(extraName);
				LoadoutTxtMapper.fromStringToLoadout(loadoutString, loadout);
			}
		}
		
		// maxZones
		if (warConfig.keyExists("maxZones")) {
			War.war.getWarConfig().put(WarConfig.MAXZONES, warConfig.getInt("maxZones"));
		}

		// defaultLifePool
		if (warConfig.keyExists("defaultLifePool")) {
			War.war.getTeamDefaultConfig().put(TeamConfig.LIFEPOOL, warConfig.getInt("defaultLifePool"));
		}

		// defaultMonumentHeal
		if (warConfig.keyExists("defaultMonumentHeal")) {
			War.war.getWarzoneDefaultConfig().put(WarzoneConfig.MONUMENTHEAL, warConfig.getInt("defaultMonumentHeal"));
		}

		// defaultFriendlyFire
		if (warConfig.keyExists("defaultFriendlyFire")) {
			War.war.getWarzoneDefaultConfig().put(WarzoneConfig.FRIENDLYFIRE, warConfig.getBoolean("defaultFriendlyFire"));
		}

		// defaultAutoAssignOnly
		if (warConfig.keyExists("defaultAutoAssignOnly")) {
			War.war.getWarzoneDefaultConfig().put(WarzoneConfig.AUTOASSIGN, warConfig.getBoolean("defaultAutoAssignOnly"));
		}

		// defaultFlagPointsOnly
		if (warConfig.keyExists("defaultFlagPointsOnly")) {
			War.war.getTeamDefaultConfig().put(TeamConfig.FLAGPOINTSONLY, warConfig.getBoolean("defaultFlagPointsOnly"));
		}
		
		// defaultFlagMustBeHome
		if (warConfig.keyExists("defaultFlagMustBeHome")) {
			War.war.getTeamDefaultConfig().put(TeamConfig.FLAGMUSTBEHOME, warConfig.getBoolean("defaultFlagMustBeHome"));
		}

		// defaultTeamCap
		if (warConfig.keyExists("defaultTeamCap")) {
			War.war.getTeamDefaultConfig().put(TeamConfig.TEAMSIZE, warConfig.getInt("defaultTeamCap"));
		}

		// defaultScoreCap
		if (warConfig.keyExists("defaultScoreCap")) {
			War.war.getTeamDefaultConfig().put(TeamConfig.MAXSCORE, warConfig.getInt("defaultScoreCap"));
		}
		
		// defaultRespawnTimer
		if (warConfig.keyExists("defaultRespawnTimer")) {
			War.war.getTeamDefaultConfig().put(TeamConfig.RESPAWNTIMER, warConfig.getInt("defaultRespawnTimer"));
		}

		// pvpInZonesOnly
		if (warConfig.keyExists("pvpInZonesOnly")) {
			War.war.getWarConfig().put(WarConfig.PVPINZONESONLY, warConfig.getBoolean("pvpInZonesOnly"));
		}

		// defaultBlockHeads
		if (warConfig.keyExists("defaultBlockHeads")) {
			War.war.getWarzoneDefaultConfig().put(WarzoneConfig.BLOCKHEADS, warConfig.getBoolean("defaultBlockHeads"));
		}

		// buildInZonesOnly
		if (warConfig.keyExists("buildInZonesOnly")) {
			War.war.getWarConfig().put(WarConfig.BUILDINZONESONLY, warConfig.getBoolean("buildInZonesOnly"));
		}

		// disablePVPMessage
		if (warConfig.keyExists("disablePvpMessage")) {
			War.war.getWarConfig().put(WarConfig.DISABLEPVPMESSAGE, warConfig.getBoolean("disablePvpMessage"));
		}
		
		// disableBuildMessage
		if (warConfig.keyExists("disableBuildMessage")) {
			War.war.getWarConfig().put(WarConfig.DISABLEBUILDMESSAGE, warConfig.getBoolean("disableBuildMessage"));
		}
		
		// tntInZonesOnly
		if (warConfig.keyExists("tntInZonesOnly")) {
			War.war.getWarConfig().put(WarConfig.TNTINZONESONLY, warConfig.getBoolean("tntInZonesOnly"));
		}

		// defaultSpawnStyle
		String spawnStyle = warConfig.getString("defaultspawnStyle");
		if (spawnStyle != null && !spawnStyle.equals("")) {
			War.war.getTeamDefaultConfig().put(TeamConfig.SPAWNSTYLE, TeamSpawnStyle.getStyleFromString(spawnStyle));
		}

		// defaultFlagReturn
		String flagReturn = warConfig.getString("defaultFlagReturn");
		if (flagReturn != null && !flagReturn.equals("")) {
			War.war.getTeamDefaultConfig().put(TeamConfig.FLAGRETURN, FlagReturn.getFromString(flagReturn));
		}

		// defaultReward
		String defaultRewardStr = warConfig.getString("defaultReward");
		if (defaultRewardStr != null && !defaultRewardStr.equals("")) {
			LoadoutTxtMapper.fromStringToLoadout(defaultRewardStr, War.war.getDefaultInventories().getReward());
		}

		// defaultUnbreakableZoneBlocks
		if (warConfig.keyExists("defaultUnbreakableZoneBlocks")) {
			War.war.getWarzoneDefaultConfig().put(WarzoneConfig.UNBREAKABLE, warConfig.getBoolean("defaultUnbreakableZoneBlocks"));
		}

		// defaultNoCreatures
		if (warConfig.keyExists("defaultNoCreatures")) {
			War.war.getWarzoneDefaultConfig().put(WarzoneConfig.NOCREATURES, warConfig.getBoolean("defaultNoCreatures"));
		}
		
		// defaultGlassWalls
		if (warConfig.keyExists("defaultGlassWalls")) {
			War.war.getWarzoneDefaultConfig().put(WarzoneConfig.GLASSWALLS, warConfig.getBoolean("defaultGlassWalls"));
		}
		
		// defaultPvpInZone
		if (warConfig.keyExists("defaultPvpInZone")) {
			War.war.getWarzoneDefaultConfig().put(WarzoneConfig.PVPINZONE, warConfig.getBoolean("defaultPvpInZone"));
		}
		
		// defaultInstaBreak
		if (warConfig.keyExists("defaultInstaBreak")) {
			War.war.getWarzoneDefaultConfig().put(WarzoneConfig.INSTABREAK, warConfig.getBoolean("defaultInstaBreak"));
		}
		
		// defaultNoDrops
		if (warConfig.keyExists("defaultNoDrops")) {
			War.war.getWarzoneDefaultConfig().put(WarzoneConfig.NODROPS, warConfig.getBoolean("defaultNoDrops"));
		}
		
		// defaultNoHunger
		if (warConfig.keyExists("defaultNoHunger")) {
			War.war.getTeamDefaultConfig().put(TeamConfig.NOHUNGER, warConfig.getBoolean("defaultNoHunger"));
		}
		
		// defaultSaturation
		if (warConfig.keyExists("defaultSaturation")) {
			War.war.getTeamDefaultConfig().put(TeamConfig.SATURATION, warConfig.getInt("defaultSaturation"));
		}
		
		// defaultMinPlayers
		if (warConfig.keyExists("defaultMinPlayers")) {
			War.war.getWarzoneDefaultConfig().put(WarzoneConfig.MINPLAYERS, warConfig.getInt("defaultMinPlayers"));
		}
		
		// defaultMinTeams
		if (warConfig.keyExists("defaultMinTeams")) {
			War.war.getWarzoneDefaultConfig().put(WarzoneConfig.MINTEAMS, warConfig.getInt("defaultMinTeams"));
		}

		// defaultResetOnEmpty
		if (warConfig.keyExists("defaultResetOnEmpty")) {
			War.war.getWarzoneDefaultConfig().put(WarzoneConfig.RESETONEMPTY, warConfig.getBoolean("defaultResetOnEmpty"));
		}

		// defaultResetOnLoad
		if (warConfig.keyExists("defaultResetOnLoad")) {
			War.war.getWarzoneDefaultConfig().put(WarzoneConfig.RESETONLOAD, warConfig.getBoolean("defaultResetOnLoad"));
		}

		// defaultResetOnUnload
		if (warConfig.keyExists("defaultResetOnUnload")) {
			War.war.getWarzoneDefaultConfig().put(WarzoneConfig.RESETONUNLOAD, warConfig.getBoolean("defaultResetOnUnload"));
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
//		PropertiesFile warConfig = new PropertiesFile(War.war.getDataFolder().getPath() + "/war.txt");
//		String warzonesStr = "";
		
		War.war.log("Saving War with WarTxtMapper", Level.SEVERE);

//		// warzones
//		for (Warzone zone : War.war.getWarzones()) {
//			warzonesStr += zone.getName() + ",";
//		}
//		warConfig.setString("warzones", warzonesStr);
//
//		// zone makers: default is none and it means everyone can use /setzone
//		String makersStr = ""; // everyone
//		for (String name : War.war.getZoneMakerNames()) {
//			makersStr += name + ",";
//		}
//		warConfig.setString("zoneMakers", makersStr);
//
//		// whitelisted commands during a game
//		String commandWhitelistStr = ""; // everyone
//		for (String command : War.war.getCommandWhitelist()) {
//			commandWhitelistStr += command + ",";
//		}
//		warConfig.setString("commandWhitelist", commandWhitelistStr);
//
//		// defaultLoadout
//		HashMap<Integer, ItemStack> items = War.war.getDefaultInventories().getLoadouts().get("default");
//		warConfig.setString("defaultLoadout", LoadoutTxtMapper.fromLoadoutToString(items));
//		
//		// defaultExtraLoadouts
//		String extraLoadoutsStr = "";
//		for (String name : War.war.getDefaultInventories().getLoadouts().keySet()) {
//			if (!name.equals("default")) {
//				extraLoadoutsStr += name + ",";
//				
//				HashMap<Integer, ItemStack> loadout = War.war.getDefaultInventories().getLoadouts().get(name);
//				warConfig.setString(name + "Loadout", LoadoutTxtMapper.fromLoadoutToString(loadout));
//			}
//		}
//		warConfig.setString("defaultExtraLoadouts", extraLoadoutsStr);
//		
//		// maxZones
//		warConfig.setInt("maxZones", War.war.getWarConfig().getInt(WarConfig.MAXZONES));
//		
//		// defaultLifepool
//		warConfig.setInt("defaultLifePool", War.war.getDefaultLifepool());
//
//		// defaultMonumentHeal
//		warConfig.setInt("defaultMonumentHeal", War.war.getDefaultMonumentHeal());
//
//		// defaultFriendlyFire
//		warConfig.setBoolean("defaultFriendlyFire", War.war.isDefaultFriendlyFire());
//
//		// defaultAutoAssignOnly
//		warConfig.setBoolean("defaultAutoAssignOnly", War.war.isDefaultAutoAssignOnly());
//
//		// defaultFlagPointsOnly
//		warConfig.setBoolean("defaultFlagPointsOnly", War.war.isDefaultFlagPointsOnly());
//		
//		// defaultFlagMustBeHome
//		warConfig.setBoolean("defaultFlagMustBeHome", War.war.isDefaultFlagMustBeHome());
//
//		// defaultTeamCap
//		warConfig.setInt("defaultTeamCap", War.war.getDefaultTeamCap());
//
//		// defaultScoreCap
//		warConfig.setInt("defaultScoreCap", War.war.getDefaultScoreCap());
//		
//		// defaultRespawnTimer
//		warConfig.setInt("defaultRespawnTimer", War.war.getDefaultRespawnTimer());
//
//		// pvpInZonesOnly
//		warConfig.setBoolean("pvpInZonesOnly", War.war.getWarConfig().getBoolean(WarConfig.PVPINZONESONLY));
//
//		// defaultBlockHeads
//		warConfig.setBoolean("defaultBlockHeads", War.war.isDefaultBlockHeads());
//
//		// buildInZonesOnly
//		warConfig.setBoolean("buildInZonesOnly", War.war.getWarConfig().getBoolean(WarConfig.BUILDINZONESONLY));
//
//		// disablePVPMessage
//		warConfig.setBoolean("disablePvpMessage", War.war.getWarConfig().getBoolean(WarConfig.DISABLEPVPMESSAGE));
//		
//		// disableBuildMessage
//		warConfig.setBoolean("disableBuildMessage", War.war.getWarConfig().getBoolean(WarConfig.DISABLEBUILDMESSAGE));
//
//		// tntInZonesOnly
//		warConfig.setBoolean("tntInZonesOnly", War.war.getWarConfig().getBoolean(WarConfig.TNTINZONESONLY));
//		
//		// spawnStyle
//		warConfig.setString("spawnStyle", War.war.getDefaultSpawnStyle().toString());
//
//		// flagReturn
//		warConfig.setString("flagReturn", War.war.getDefaultFlagReturn().toString());
//
//		// defaultReward
//		String defaultRewardStr = "";
//		HashMap<Integer, ItemStack> rewardItems = War.war.getDefaultInventories().getReward();
//		for (Integer slot : rewardItems.keySet()) {
//			ItemStack item = items.get(slot);
//			if (item != null) {
//				defaultRewardStr += item.getTypeId() + "," + item.getAmount() + "," + slot + ";";
//			}
//		}
//		warConfig.setString("defaultReward", defaultRewardStr);
//
//		// defaultUnbreakableZoneBlocks
//		warConfig.setBoolean("defaultUnbreakableZoneBlocks", War.war.isDefaultUnbreakableZoneBlocks());
//
//		// defaultNoCreatures
//		warConfig.setBoolean("defaultNoCreatures", War.war.isDefaultNoCreatures());
//
//		// defaultGlassWalls
//		warConfig.setBoolean("defaultGlassWalls", War.war.isDefaultGlassWalls());
//		
//		// defaultPvpInZone
//		warConfig.setBoolean("defaultPvpInZone", War.war.isDefaultPvpInZone());
//
//		// defaultInstaBreak
//		warConfig.setBoolean("defaultInstaBreak", War.war.isDefaultInstaBreak());
//		
//		// defaultNoDrops
//		warConfig.setBoolean("defaultNoDrops", War.war.isDefaultNoDrops());
//		
//		// defaultNoHunger
//		warConfig.setBoolean("defaultNoHunger", War.war.isDefaultNoHunger());
//				
//		// defaultSaturation
//		warConfig.setInt("defaultSaturation", War.war.getDefaultSaturation());
//				
//		// defaultMinPlayers
//		warConfig.setInt("defaultMinPlayers", War.war.getDefaultMinPlayers());
//		
//		// defaultMinTeams
//		warConfig.setInt("defaultMinTeams", War.war.getDefaultMinTeams());
//
//		// defaultResetOnEmpty
//		warConfig.setBoolean("defaultResetOnEmpty", War.war.isDefaultResetOnEmpty());
//
//		// defaultResetOnLoad
//		warConfig.setBoolean("defaultResetOnLoad", War.war.isDefaultResetOnLoad());
//
//		// defaultResetOnUnload
//		warConfig.setBoolean("defaultResetOnUnload", War.war.isDefaultResetOnUnload());
//
//		// warhub
//		String hubStr = "";
//		WarHub hub = War.war.getWarHub();
//		if (hub != null) {
//			String orientationStr = "";
//			switch (hub.getOrientation()) {
//				case SOUTH:
//					orientationStr = "south";
//					break;
//				case EAST:
//					orientationStr = "east";
//					break;
//				case NORTH:
//					orientationStr = "north";
//					break;
//				case WEST:
//				default:
//					orientationStr = "west";
//					break;
//			}
//			hubStr = hub.getLocation().getBlockX() + "," + hub.getLocation().getBlockY() + "," + hub.getLocation().getBlockZ() + ","
//					+ hub.getLocation().getWorld().getName() + "," + orientationStr;
//			VolumeMapper.save(hub.getVolume(), "");
//		}
//		warConfig.setString("warhub", hubStr);
//
//		warConfig.save();
//		warConfig.close();
	}
}
