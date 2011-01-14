package com.tommytony.war.mappers;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.bukkit.ItemStack;
import org.bukkit.Location;
import org.bukkit.World;

import bukkit.tommytony.war.War;

import com.tommytony.war.WarHub;
import com.tommytony.war.Warzone;
import com.tommytony.war.volumes.Volume;

/**
 * 
 * @author tommytony
 *
 */
public class WarMapper {
	
	public static void load(War war, World world) {
		//war.getLogger().info("Loading war config...");
		(new File(war.getName())).mkdir();
		PropertiesFile warConfig = new PropertiesFile(war.getName() + "/war.txt");
		try {
			warConfig.load();
		} catch (IOException e) {
			war.getLogger().info("Failed to load war.txt file.");
			e.printStackTrace();
		}
		
		// Create file if need be
		boolean newWar = false;
		if(!warConfig.containsKey("warzones")) {
			newWar = true;
			WarMapper.save(war);
			war.getLogger().info("War config file created.");
			try {
				warConfig.load();
			} catch (IOException e) {
				war.getLogger().info("Failed to reload war.txt file after creating it.");
				e.printStackTrace();
			}
		}
		
		// warzones
		String warzonesStr = warConfig.getString("warzones");
		String[] warzoneSplit = warzonesStr.split(",");
		war.getWarzones().clear();
		for(String warzoneName : warzoneSplit) {
			if(warzoneName != null && !warzoneName.equals("")){
				Warzone zone = WarzoneMapper.load(war, warzoneName, !newWar);		// cascade load, only load blocks if warzone exists
				war.getWarzones().add(zone);
				zone.getVolume().resetBlocks();
				zone.initializeZone();			// is this wise?
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
		war.setDefaultFriendlyFire(warConfig.getBoolean("defaultDrawZoneOutline"));
		
		// defaultAutoAssignOnly
		war.setDefaultAutoAssignOnly(warConfig.getBoolean("defaultAutoAssignOnly"));
		
		// warhub
		String hubStr = warConfig.getString("warhub");
		if(hubStr != null && !hubStr.equals("")) {
			String[] nwStrSplit = hubStr.split(",");
			
			int hubX = Integer.parseInt(nwStrSplit[0]);
			int hubY = Integer.parseInt(nwStrSplit[1]);
			int hubZ = Integer.parseInt(nwStrSplit[2]);
			Location hubLocation = new Location(world, hubX, hubY, hubZ);
			WarHub hub = new WarHub(war, hubLocation);
			war.setWarHub(hub);
			Volume vol = VolumeMapper.loadVolume("warhub", "", war, world);
			hub.setVolume(vol);
			hub.getVolume().resetBlocks();
			hub.initialize();
		}
		
		warConfig.close();
		war.getLogger().info("Loaded war config.");
	}
	
	public static void save(War war) {
		//war.getLogger().info("Saving war config...");
		PropertiesFile warConfig = new PropertiesFile(war.getName() + "/war.txt");
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
			defaultLoadoutStr += item.getTypeID() + "," + item.getAmount() + "," + slot + ";";
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
		
		// warhub
		String hubStr = "";
		WarHub hub = war.getWarHub();
		if(hub != null) {
			hubStr = hub.getLocation().getBlockX() + "," + hub.getLocation().getBlockY() + "," + hub.getLocation().getBlockZ();
		}
		warConfig.setString("warhub", hubStr);
		
		warConfig.save();
		warConfig.close();
		//war.getLogger().info("Saved war config.");
	}
}
