package com.tommytony.war.mappers;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import bukkit.tommytony.war.War;

import com.tommytony.war.TeamSpawnStyles;
import com.tommytony.war.WarHub;
import com.tommytony.war.Warzone;
import com.tommytony.war.volumes.Volume;

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
		String[] warzoneSplit = warzonesStr.split(",");
		war.getWarzones().clear();
		for(String warzoneName : warzoneSplit) {
			if(warzoneName != null && !warzoneName.equals("")){
				war.logInfo("Starting zone " + warzoneName + " restore...");
				Warzone zone = WarzoneMapper.load(war, warzoneName, !newWar);		// cascade load, only load blocks if warzone exists
				if(zone != null) { // could have failed, would've been logged already 
					war.getWarzones().add(zone);
					zone.getVolume().resetBlocksAsJob();
					if(zone.getLobby() != null) {
						zone.getLobby().getVolume().resetBlocksAsJob();
					}
					zone.initializeZoneAsJob();
				}
			}
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
		
		// defaultFriendlyFire
		war.setDefaultFriendlyFire(warConfig.getBoolean("defaultFriendlyFire"));
		
		// defaultDrawZoneOutline
		war.setDefaultDrawZoneOutline(warConfig.getBoolean("defaultDrawZoneOutline"));
		
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
		
		// defaultSpawnStyle
		String spawnStyle = warConfig.getString("defaultspawnStyle");
		if(spawnStyle != null && !spawnStyle.equals("")){
			spawnStyle = spawnStyle.toLowerCase();
			if(spawnStyle.equals(TeamSpawnStyles.SMALL)) {
				war.setDefaultSpawnStyle(spawnStyle);
			} else if (spawnStyle.equals(TeamSpawnStyles.FLAT)){
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
		
		// warhub
		String hubStr = warConfig.getString("warhub");
		if(hubStr != null && !hubStr.equals("")) {
			String[] hubStrSplit = hubStr.split(",");
			
			int hubX = Integer.parseInt(hubStrSplit[0]);
			int hubY = Integer.parseInt(hubStrSplit[1]);
			int hubZ = Integer.parseInt(hubStrSplit[2]);
			World world = null;
			if(hubStrSplit.length > 3) {
				String worldName = hubStrSplit[3];
				world = war.getServer().getWorld(worldName);
			} else {
				world = war.getServer().getWorlds().get(0);		// default to first world
			}
			Location hubLocation = new Location(world, hubX, hubY, hubZ);
			WarHub hub = new WarHub(war, hubLocation);
			war.setWarHub(hub);
			Volume vol = VolumeMapper.loadVolume("warhub", "", war, world);
			hub.setVolume(vol);
			hub.getVolume().resetBlocks();
			hub.initialize();
			
//			for(Warzone zone : war.getWarzones()) {
//				if(zone.getLobby() != null) {
//					zone.getLobby().getVolume().resetBlocksAsJob();
//					zone.getLobby().initialize();	// adds the warhub link gate
//				}
//					
//			}
		}
		
		warConfig.close();
		//war.getLogger().info("Loaded war config.");
	}
	
	public static void save(War war) {
		//war.getLogger().info("Saving war config...");
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
			defaultLoadoutStr += item.getTypeId() + "," + item.getAmount() + "," + slot + ";";
		}
		warConfig.setString("defaultLoadout", defaultLoadoutStr);
		
		// defaultLifepool
		warConfig.setInt("defaultLifePool", war.getDefaultLifepool());
		
		// defaultFriendlyFire
		warConfig.setBoolean("defaultFriendlyFire", war.getDefaultFriendlyFire());
		
		// defaultFriendlyFire
		warConfig.setBoolean("defaultDrawZoneOutline", war.getDefaultDrawZoneOutline());
		
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
		
		// spawnStyle
		warConfig.setString("spawnStyle", war.getDefaultSpawnStyle());
		
		// defaultReward
		String defaultRewardStr = "";
		HashMap<Integer, ItemStack> rewardItems = war.getDefaultReward();
		for(Integer slot : rewardItems.keySet()) {
			ItemStack item = items.get(slot);
			defaultRewardStr += item.getTypeId() + "," + item.getAmount() + "," + slot + ";";
		}
		warConfig.setString("defaultReward", defaultRewardStr);

		// defaultUnbreakableZoneBlocks
		warConfig.setBoolean("defaultUnbreakableZoneBlocks", war.isDefaultUnbreakableZoneBlocks());
		
		// defaultNoCreatures
		warConfig.setBoolean("defaultNoCreatures", war.isDefaultNoCreatures());
		
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
		//war.getLogger().info("Saved war config.");
	}
}
