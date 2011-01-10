package com.tommytony.war.mappers;
import java.io.File;
import java.io.IOException;

import com.tommytony.war.War;
import com.tommytony.war.Warzone;


public class WarMapper {
	
	public static void load(War war) {
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
				zone.resetState();			// is this wise?
			}
		}
		
		// defaultLoadout
//		String defaultLoadoutStr = warConfig.getString("defaultLoadout");
//		String[] defaultLoadoutSplit = defaultLoadoutStr.split(";");
//		war.getDefaultLoadout().clear();
//		for(String itemStr : defaultLoadoutSplit) {
//			if(itemStr != null && !itemStr.equals("")) {
//				String[] itemStrSplit = itemStr.split(",");
//				ItemStack item = new ItemStack(Integer.parseInt(itemStrSplit[0]),
//						Integer.parseInt(itemStrSplit[1]));
//				war.getDefaultLoadout().add(item);
//			}
//		}
		
		// defaultLifePool
		war.setDefaultLifepool(warConfig.getInt("defaultLifePool"));
		
		// defaultFriendlyFire
		war.setDefaultFriendlyFire(warConfig.getBoolean("defaultFriendlyFire"));
		
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
		
		// defaultLoadout
//		String defaultLoadoutStr = "";
//		List<Item> items = war.getDefaultLoadout();
//		for(Item item : items) {
//			defaultLoadoutStr += item.getItemId() + "," + item.getAmount() + "," + item.getSlot() + ";";
//		}
//		warConfig.setString("defaultLoadout", defaultLoadoutStr);
		
		// defaultLifepool
		warConfig.setInt("defaultLifePool", war.getDefaultLifepool());
		
		// defaultFriendlyFire
		warConfig.setBoolean("defaultFriendlyFire", war.getDefaultFriendlyFire());
		
		warConfig.save();
		warConfig.close();
		//war.getLogger().info("Saved war config.");
	}
}
