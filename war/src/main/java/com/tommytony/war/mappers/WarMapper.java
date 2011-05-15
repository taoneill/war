package com.tommytony.war.mappers;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

import bukkit.tommytony.war.War;

import com.tommytony.war.TeamSpawnStyles;
import com.tommytony.war.WarHub;
import com.tommytony.war.Warzone;
import com.tommytony.war.jobs.RestoreWarhubJob;
import com.tommytony.war.jobs.RestoreWarzonesJob;
import com.tommytony.war.utils.InventoryStash;

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
		
		// defaultMonumentHeal		//SY
		war.setDefaultLifepool(warConfig.getInt("defaultMonumentHeal"));
		
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
			RestoreWarhubJob restoreWarhub = new RestoreWarhubJob(war, hubStr);
			if(war.getServer().getScheduler().scheduleSyncDelayedTask(war, restoreWarhub) == -1) {
				war.logWarn("Failed to schedule warhub-restore job. War hub was not loaded.");
			}
		}
		
		warConfig.close();
		
		//loadDisconnected(war);
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
		
		// defaultMonumentHeal		//SY
		warConfig.setInt("defaultMonumentHeal", war.getDefaultMonumentHeal());
		
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
			if(item != null) {
				defaultRewardStr += item.getTypeId() + "," + item.getAmount() + "," + slot + ";";
			}
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
	}

//	private static void loadDisconnected(War war) {
//		BufferedReader in = null;
//		try {
//			in = new BufferedReader(new FileReader(new File(war.getDataFolder().getPath() + 
//													"/dat/disconnected.dat")));
//			String blockLine = in.readLine();
//			while(blockLine != null && !blockLine.equals("")) {
//				String[] blockSplit = blockLine.split(",");
//				if(blockLine != null && !blockLine.equals("") && blockSplit.length > 1) {
//					String playerName = blockSplit[0];
//					List<ItemStack> items = new ArrayList<ItemStack>();
//					if(blockSplit.length > 1) {
//						String itemsStr = blockSplit[1];
//						String[] itemsStrSplit = itemsStr.split(";;");
//						for(String itemStr : itemsStrSplit) {
//							String[] itemStrSplit = itemStr.split(";");
//							if(itemStrSplit.length == 4) {
//								ItemStack stack = new ItemStack(Integer.parseInt(itemStrSplit[0]),
//										Integer.parseInt(itemStrSplit[1]));
//								stack.setData(new MaterialData(stack.getTypeId(),Byte.parseByte(itemStrSplit[3])));
//								short durability = (short)Integer.parseInt(itemStrSplit[2]);
//								stack.setDurability(durability);
//								items.add(stack);
//							} else if(itemStrSplit.length == 3) {
//								ItemStack stack = new ItemStack(Integer.parseInt(itemStrSplit[0]),
//										Integer.parseInt(itemStrSplit[1]));
//								short durability = (short)Integer.parseInt(itemStrSplit[2]);
//								stack.setDurability(durability);
//								items.add(stack);
//							} else {
//								items.add(new ItemStack(Integer.parseInt(itemStrSplit[0]),
//										Integer.parseInt(itemStrSplit[1])));
//							}
//						}
//					}
//					war.getDisconnected().put(playerName, new InventoryStash(items));
//				}
//				
//			}
//		} catch (IOException e) {
//			war.logWarn("Failed to read volume file " + volume.getName() + 
//					" for warzone " + zoneName + ". " + e.getClass().getName() + " " + e.getMessage());
//			e.printStackTrace();
//		} catch (Exception e) {
//			war.logWarn("Unexpected error caused failure to read volume file " + zoneName + 
//					" for warzone " + volume.getName() + ". " + e.getClass().getName() + " " + e.getMessage());
//			e.printStackTrace();
//		}  finally {
//			if(in != null)
//				try {
//					in.close();
//				} catch (IOException e) {
//					war.logWarn("Failed to close file reader for volume " + volume.getName() +
//							" for warzone " + zoneName + ". " + e.getClass().getName() + " " + e.getMessage());
//					e.printStackTrace();
//				}
//		}
//	}
//
//	public static void saveDisconnected(War war) {
//		BufferedWriter out = null;
//		try {
//			out = new BufferedWriter(new FileWriter(new File(war.getDataFolder().getPath() + "/dat/disconnected.dat")));
//			HashMap<String,InventoryStash> disconnected = war.getDisconnected();
//			for(String key : disconnected.keySet()){
//				String userString = key + ",";
//				InventoryStash userInv = disconnected.get(userString);
//				for(ItemStack item : userInv.getContents()) {
//					String extra = "";
//					if(item != null) {
//						extra += item.getTypeId() + ";" 
//						+ item.getAmount() + ";" 
//						+ item.getDurability(); 
//						if(item.getData() != null)
//							extra += ";" + item.getData().getData() ;
//						extra += ";;";
//					}
//					userString += extra;
//				}
//				if(userInv.getHelmet() != null) {
//					
//				}
//				
//				out.write(userString);
//				out.newLine();
//			}
//		} catch (IOException e) {
//			war.logWarn("Failed while writing disconnected user inventories. " + e.getMessage());
//			e.printStackTrace();
//		} catch (Exception e) {
//			war.logWarn("Unexpected error caused failure while writing disconnected user inventories. " + e.getClass().getName() + " " + e.getMessage());
//			e.printStackTrace();
//		}  
//		finally {
//			if(out != null)
//				try {
//					out.close();
//				} catch (IOException e) {
//					war.logWarn("Failed to close file writer for disconnected user inventories. " + e.getMessage());
//					e.printStackTrace();
//				}	
//		}
//	}
}
