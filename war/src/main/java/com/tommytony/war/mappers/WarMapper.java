package com.tommytony.war.mappers;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.bukkit.inventory.ItemStack;

import bukkit.tommytony.war.War;

import com.tommytony.war.TeamSpawnStyles;
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
	
	public static void load(War war) {
		//war.getLogger().info("Loading war config...");
		(war.getDataFolder()).mkdir();
		(new File(war.getDataFolder().getPath() + "/dat")).mkdir();
		PropertiesFile warConfig = new PropertiesFile(war.getDataFolder().getPath() + "/war.txt");
		try {
			warConfig.load();
		} catch (IOException e) {
			war.logWarn("Failed to load war.txt file.");
			e.printStackTrace();
		}
		
		// Create file if need be
		boolean newWar = false;
		if(!warConfig.containsKey("warzones")) {
			newWar = true;
			WarMapper.save(war);
			war.logInfo("war.txt settings file created.");
			try {
				warConfig.load();
			} catch (IOException e) {
				war.logWarn("Failed to reload war.txt file after creating it.");
				e.printStackTrace();
			}
		}
		
		// warzones
		String warzonesStr = warConfig.getString("warzones");
		RestoreWarzonesJob restoreWarzones = new RestoreWarzonesJob(war, warzonesStr, newWar);
		if(war.getServer().getScheduler().scheduleSyncDelayedTask(war, restoreWarzones) == -1) {
			war.logWarn("Failed to schedule warzone-restore job. No warzone was loaded.");
		}
		
		// zone makers
		String makersStr = warConfig.getString("zoneMakers");
		String[] makersSplit = makersStr.split(",");
		war.getZoneMakerNames().clear();
		for(String makerName : makersSplit) {
			if(makerName != null && !makerName.equals("")){
				war.getZoneMakerNames().add(makerName);
			}
		}
		
		// defaultLoadout
		String defaultLoadoutStr = warConfig.getString("defaultLoadout");
		String[] defaultLoadoutSplit = defaultLoadoutStr.split(";");
		war.getDefaultLoadout().clear();
		for(String itemStr : defaultLoadoutSplit) {
			if(itemStr != null && !itemStr.equals("")) {
				String[] itemStrSplit = itemStr.split(",");
				ItemStack item = new ItemStack(Integer.parseInt(itemStrSplit[0]),
						Integer.parseInt(itemStrSplit[1]));
				war.getDefaultLoadout().put(Integer.parseInt(itemStrSplit[2]), item);
			}
		}
		
		// defaultLifePool
		war.setDefaultLifepool(warConfig.getInt("defaultLifePool"));
		
		// defaultMonumentHeal
		war.setDefaultMonumentHeal(warConfig.getInt("defaultMonumentHeal"));
		
		// defaultFriendlyFire
		war.setDefaultFriendlyFire(warConfig.getBoolean("defaultFriendlyFire"));
		
		// defaultAutoAssignOnly
		war.setDefaultAutoAssignOnly(warConfig.getBoolean("defaultAutoAssignOnly"));
		
		// defaultTeamCap
		war.setDefaultTeamCap(warConfig.getInt("defaultTeamCap"));
		
		// defaultScoreCap
		war.setDefaultScoreCap(warConfig.getInt("defaultScoreCap"));

		// pvpInZonesOnly
		war.setPvpInZonesOnly(warConfig.getBoolean("pvpInZonesOnly"));
		
		// defaultBlockHeads
		war.setDefaultBlockHeads(warConfig.getBoolean("defaultBlockHeads"));
		
		// buildInZonesOnly
		war.setBuildInZonesOnly(warConfig.getBoolean("buildInZonesOnly"));
		
		// disablePVPMessage
		war.setDisablePvpMessage(warConfig.getBoolean("disablePvpMessage"));
		
		// defaultSpawnStyle
		String spawnStyle = warConfig.getString("defaultspawnStyle");
		if(spawnStyle != null && !spawnStyle.equals("")){
			spawnStyle = spawnStyle.toLowerCase();
			if(spawnStyle.equals(TeamSpawnStyles.SMALL)) {
				war.setDefaultSpawnStyle(spawnStyle);
			} else if (spawnStyle.equals(TeamSpawnStyles.FLAT)){
				war.setDefaultSpawnStyle(spawnStyle);
			} else if (spawnStyle.equals(TeamSpawnStyles.INVISIBLE)){
				war.setDefaultSpawnStyle(spawnStyle);
			}
			// default is already initialized to BIG (see Warzone)				
		}
		
		// defaultReward
		String defaultRewardStr = warConfig.getString("defaultReward");
		if(defaultRewardStr != null && !defaultRewardStr.equals("")) {
			String[] defaultRewardStrSplit = defaultRewardStr.split(";");
			war.getDefaultReward().clear();
			for(String itemStr : defaultRewardStrSplit) {
				if(itemStr != null && !itemStr.equals("")) {
					String[] itemStrSplit = itemStr.split(",");
					ItemStack item = new ItemStack(Integer.parseInt(itemStrSplit[0]),
							Integer.parseInt(itemStrSplit[1]));
					war.getDefaultReward().put(Integer.parseInt(itemStrSplit[2]), item);
				}
			}
		}
		
		// defaultUnbreakableZoneBlocks
		war.setDefaultUnbreakableZoneBlocks(warConfig.getBoolean("defaultUnbreakableZoneBlocks"));
		
		// defaultNoCreatures
		war.setDefaultNoCreatures(warConfig.getBoolean("defaultNoCreatures"));
		
		// defaultDropLootOnDeath
		//war.setDefaultDropLootOnDeath(warConfig.getBoolean("defaultDropLootOnDeath"));
		
		// defaultResetOnEmpty
		war.setDefaultResetOnEmpty(warConfig.getBoolean("defaultResetOnEmpty"));

		// defaultResetOnLoad
		war.setDefaultResetOnLoad(warConfig.getBoolean("defaultResetOnLoad"));
		
		// defaultResetOnUnload
		war.setDefaultResetOnUnload(warConfig.getBoolean("defaultResetOnUnload"));
		
		// warhub
		String hubStr = warConfig.getString("warhub");
		if(hubStr != null && !hubStr.equals("")) {
			RestoreWarhubJob restoreWarhub = new RestoreWarhubJob(war, hubStr);
			if(war.getServer().getScheduler().scheduleSyncDelayedTask(war, restoreWarhub) == -1) {
				war.logWarn("Failed to schedule warhub-restore job. War hub was not loaded.");
			}
		}
		
		warConfig.close();
	}
	
	public static void save(War war) {
		PropertiesFile warConfig = new PropertiesFile(war.getDataFolder().getPath() + "/war.txt");
		String warzonesStr = "";
		
		// warzones
		for(Warzone zone : war.getWarzones()) {
			warzonesStr += zone.getName() + ",";
		}
		warConfig.setString("warzones", warzonesStr);
		
		// zone makers: default is none and it means everyone can use /setzone
		String makersStr = "";	// everyone
		for(String name : war.getZoneMakerNames()) {
			makersStr += name + ",";
		}
		warConfig.setString("zoneMakers", makersStr);
		
		// defaultLoadout
		String defaultLoadoutStr = "";
		HashMap<Integer, ItemStack> items = war.getDefaultLoadout();
		for(Integer slot : items.keySet()) {
			ItemStack item = items.get(slot);
			if(item != null) {
				defaultLoadoutStr += item.getTypeId() + "," + item.getAmount() + "," + slot + ";";
			}
		}
		warConfig.setString("defaultLoadout", defaultLoadoutStr);
		
		// defaultLifepool
		warConfig.setInt("defaultLifePool", war.getDefaultLifepool());
		
		// defaultMonumentHeal
		warConfig.setInt("defaultMonumentHeal", war.getDefaultMonumentHeal());
		
		// defaultFriendlyFire
		warConfig.setBoolean("defaultFriendlyFire", war.getDefaultFriendlyFire());
	
		// defaultAutoAssignOnly
		warConfig.setBoolean("defaultAutoAssignOnly", war.getDefaultAutoAssignOnly());
		
		// defaultTeamCap
		warConfig.setInt("defaultTeamCap", war.getDefaultTeamCap());
		
		// defaultScoreCap
		warConfig.setInt("defaultScoreCap", war.getDefaultScoreCap());

		// pvpInZonesOnly
		warConfig.setBoolean("pvpInZonesOnly", war.isPvpInZonesOnly());
		
		// defaultBlockHeads
		warConfig.setBoolean("defaultBlockHeads", war.isDefaultBlockHeads());
		
		// buildInZonesOnly
		warConfig.setBoolean("buildInZonesOnly", war.isBuildInZonesOnly());
		
		// disablePVPMessage
		warConfig.setBoolean("disablePvpMessage", war.isDisablePvpMessage());
		
		// spawnStyle
		warConfig.setString("spawnStyle", war.getDefaultSpawnStyle());
		
		// defaultReward
		String defaultRewardStr = "";
		HashMap<Integer, ItemStack> rewardItems = war.getDefaultReward();
		for(Integer slot : rewardItems.keySet()) {
			ItemStack item = items.get(slot);
			if(item != null) {
				defaultRewardStr += item.getTypeId() + "," + item.getAmount() + "," + slot + ";";
			}
		}
		warConfig.setString("defaultReward", defaultRewardStr);

		// defaultUnbreakableZoneBlocks
		warConfig.setBoolean("defaultUnbreakableZoneBlocks", war.isDefaultUnbreakableZoneBlocks());
		
		// defaultNoCreatures
		warConfig.setBoolean("defaultNoCreatures", war.isDefaultNoCreatures());
		
		// defaultResetOnEmpty
		warConfig.setBoolean("defaultResetOnEmpty", war.isDefaultResetOnEmpty());
		
		// defaultResetOnLoad
		warConfig.setBoolean("defaultResetOnLoad", war.isDefaultResetOnLoad());
		
		// defaultResetOnUnload
		warConfig.setBoolean("defaultResetOnUnload", war.isDefaultResetOnUnload());
		
		// defaultDropLootOnDeath
		//warConfig.setBoolean("defaultDropLootOnDeath", war.isDefaultDropLootOnDeath());
		
		// warhub
		String hubStr = "";
		WarHub hub = war.getWarHub();
		if(hub != null) {
			hubStr = hub.getLocation().getBlockX() + "," + hub.getLocation().getBlockY() + "," + hub.getLocation().getBlockZ() + "," 
						+ hub.getLocation().getWorld().getName();
			VolumeMapper.save(hub.getVolume(), "", war);
		}
		warConfig.setString("warhub", hubStr);
		
		warConfig.save();
		warConfig.close();
	}
}
