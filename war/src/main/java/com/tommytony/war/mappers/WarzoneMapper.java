package com.tommytony.war.mappers;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;

import bukkit.tommytony.war.War;

import com.tommytony.war.Monument;
import com.tommytony.war.Team;
import com.tommytony.war.TeamKinds;
import com.tommytony.war.TeamSpawnStyles;
import com.tommytony.war.Warzone;
import com.tommytony.war.ZoneLobby;
import com.tommytony.war.volumes.Volume;
import com.tommytony.war.volumes.ZoneVolume;

/**
 * 
 * @author tommytony
 *
 */
public class WarzoneMapper {

	public static Warzone load(War war, String name, boolean loadBlocks) {
		//war.getLogger().info("Loading warzone " + name + " config and blocks...");
		PropertiesFile warzoneConfig = new PropertiesFile(war.getDataFolder().getPath() + "/warzone-" + name + ".txt");
		try {
			warzoneConfig.load();
		} catch (IOException e) {
			war.getLogger().info("Failed to load warzone-" + name + ".txt file.");
			e.printStackTrace();
		}
		
		// world
		String worldStr = warzoneConfig.getProperty("world");
		World world = null;
		if(worldStr == null || worldStr.equals("")){
			world = war.getServer().getWorlds().get(0); // default to first world
		} else {
			world = war.getServer().getWorld(worldStr);
		}
		
		
		if(world == null) {
			war.logWarn("Failed to restore warzone " + name + ". The specified world (name: " + worldStr + ") does not exist!");
		} else {
			// Create the zone	
			Warzone warzone = new Warzone(war, world, name);
			
			// Create file if needed 
			if(!warzoneConfig.containsKey("name")) {
				WarzoneMapper.save(war, warzone, false);
				war.getLogger().info("Warzone " + name + " config file created.");
				try {
					warzoneConfig.load();
				} catch (IOException e) {
					//war.getLogger().info("Failed to reload warzone-" + name + ".txt file after creating it.");
					e.printStackTrace();
				}
			}
			
			// teleport
			String teleportStr = warzoneConfig.getString("teleport");
			if(teleportStr != null && !teleportStr.equals("")) {
				String[] teleportSplit = teleportStr.split(",");
				int teleX = Integer.parseInt(teleportSplit[0]);
				int teleY = Integer.parseInt(teleportSplit[1]);
				int teleZ = Integer.parseInt(teleportSplit[2]);
				int yaw = Integer.parseInt(teleportSplit[3]);
				warzone.setTeleport(new Location(world, teleX, teleY, teleZ, yaw, 0));
			}
			
			// teams
			String teamsStr = warzoneConfig.getString("teams");
			if(teamsStr != null && !teamsStr.equals("")) {
				String[] teamsSplit = teamsStr.split(";");
				warzone.getTeams().clear();
				for(String teamStr : teamsSplit) {
					if(teamStr != null && !teamStr.equals("")){
						String[] teamStrSplit = teamStr.split(",");
						int teamX = Integer.parseInt(teamStrSplit[1]);
						int teamY = Integer.parseInt(teamStrSplit[2]);
						int teamZ = Integer.parseInt(teamStrSplit[3]);
						Location teamLocation = new Location(world, teamX, teamY, teamZ);
						if(teamStrSplit.length > 4) {
							int yaw = Integer.parseInt(teamStrSplit[4]);
							teamLocation.setYaw(yaw);
						}
						Team team = new Team(teamStrSplit[0], 
											TeamKinds.teamKindFromString(teamStrSplit[0]),
											teamLocation,
											war, warzone );
						team.setRemainingLives(warzone.getLifePool());
						warzone.getTeams().add(team);
					}
				}
			}
			
			// teamFlags
			String teamFlagsStr = warzoneConfig.getString("teamFlags");
			if(teamFlagsStr != null && !teamFlagsStr.equals("")) {
				String[] teamFlagsSplit = teamFlagsStr.split(";");
				for(String teamFlagStr : teamFlagsSplit) {
					if(teamFlagStr != null && !teamFlagStr.equals("")){
						String[] teamFlagStrSplit =teamFlagStr.split(",");
						Team team = warzone.getTeamByKind(TeamKinds.teamKindFromString(teamFlagStrSplit[0]));
						if(team != null) {
							int teamFlagX = Integer.parseInt(teamFlagStrSplit[1]);
							int teamFlagY = Integer.parseInt(teamFlagStrSplit[2]);
							int teamFlagZ = Integer.parseInt(teamFlagStrSplit[3]);
							Location teamFlagLocation = new Location(world, teamFlagX, teamFlagY, teamFlagZ);
							if(teamFlagStrSplit.length > 4) {
								int yaw = Integer.parseInt(teamFlagStrSplit[4]);
								teamFlagLocation.setYaw(yaw);
							}
							team.setTeamFlag(teamFlagLocation);	// this may screw things up
						}
					}
				}
			}
			
			// ff
			warzone.setFriendlyFire(warzoneConfig.getBoolean("friendlyFire"));
			
			// loadout
			String loadoutStr = warzoneConfig.getString("loadout");
			if(loadoutStr != null && !loadoutStr.equals("")) {
				String[] loadoutStrSplit = loadoutStr.split(";");
				warzone.getLoadout().clear();
				for(String itemStr : loadoutStrSplit) {
					if(itemStr != null && !itemStr.equals("")) {
						String[] itemStrSplit = itemStr.split(",");
						ItemStack item = new ItemStack(Integer.parseInt(itemStrSplit[0]),
								Integer.parseInt(itemStrSplit[1]));
						warzone.getLoadout().put(Integer.parseInt(itemStrSplit[2]), item);
					}
				}
			}
			
			// life pool
			warzone.setLifePool(warzoneConfig.getInt("lifePool"));
			
			// monument heal		//SY
			warzone.setMonumentHeal(warzoneConfig.getInt("monumentHeal"));
			
			// drawZoneOutline
			warzone.setDrawZoneOutline(warzoneConfig.getBoolean("drawZoneOutline"));
			
			// autoAssignOnly
			warzone.setAutoAssignOnly(warzoneConfig.getBoolean("autoAssignOnly"));
			
			// team cap
			warzone.setTeamCap(warzoneConfig.getInt("teamCap"));
			
			// score cap
			warzone.setScoreCap(warzoneConfig.getInt("scoreCap"));
	
			// blockHeads
			warzone.setBlockHeads(warzoneConfig.getBoolean("blockHeads"));
			
			// spawnStyle
			String spawnStyle = warzoneConfig.getString("spawnStyle");
			if(spawnStyle != null && !spawnStyle.equals("")){
				spawnStyle = spawnStyle.toLowerCase();
				if(spawnStyle.equals(TeamSpawnStyles.SMALL)) {
					warzone.setSpawnStyle(spawnStyle);
				} else if (spawnStyle.equals(TeamSpawnStyles.FLAT)){
					warzone.setSpawnStyle(spawnStyle);
				}
				// default is already initialized to BIG (see Warzone)				
			}
			
			// reward
			String rewardStr = warzoneConfig.getString("reward");
			if(rewardStr != null && !rewardStr.equals("")) {
				String[] rewardStrSplit = rewardStr.split(";");
				warzone.getReward().clear();
				for(String itemStr : rewardStrSplit) {
					if(itemStr != null && !itemStr.equals("")) {
						String[] itemStrSplit = itemStr.split(",");
						ItemStack item = new ItemStack(Integer.parseInt(itemStrSplit[0]),
								Integer.parseInt(itemStrSplit[1]));
						warzone.getReward().put(Integer.parseInt(itemStrSplit[2]), item);
					}
				}
			}
			
			// unbreakableZoneBlocks
			warzone.setUnbreakableZoneBlocks(warzoneConfig.getBoolean("unbreakableZoneBlocks"));
			
			// disabled
			warzone.setDisabled(warzoneConfig.getBoolean("disabled"));
			
			// defaultNoCreatures
			warzone.setNoCreatures(warzoneConfig.getBoolean("noCreatures"));
			
			// rallyPoint
			String rallyPointStr = warzoneConfig.getString("rallyPoint");
			if(rallyPointStr != null && !rallyPointStr.equals("")) {
				String[] rallyPointStrSplit = rallyPointStr.split(",");
				
				int rpX = Integer.parseInt(rallyPointStrSplit[0]);
				int rpY = Integer.parseInt(rallyPointStrSplit[1]);
				int rpZ = Integer.parseInt(rallyPointStrSplit[2]);
				Location rallyPoint = new Location(world, rpX, rpY, rpZ);
				warzone.setRallyPoint(rallyPoint);
			}
			
			// dropLootOnDeath
			//warzone.setDropLootOnDeath(warzoneConfig.getBoolean("dropLootOnDeath"));
			
			// monuments
			String monumentsStr = warzoneConfig.getString("monuments");
			if(monumentsStr != null && !monumentsStr.equals("")) {
				String[] monumentsSplit = monumentsStr.split(";");
				warzone.getMonuments().clear();
				for(String monumentStr  : monumentsSplit) {
					if(monumentStr != null && !monumentStr.equals("")){
						String[] monumentStrSplit = monumentStr.split(",");
						int monumentX = Integer.parseInt(monumentStrSplit[1]);
						int monumentY = Integer.parseInt(monumentStrSplit[2]);
						int monumentZ = Integer.parseInt(monumentStrSplit[3]);
						Monument monument = new Monument(monumentStrSplit[0], war, warzone, 
												new Location(world, monumentX, monumentY, monumentZ));
						warzone.getMonuments().add(monument);
					}
				}
			}
			
			// lobby
			String lobbyStr = warzoneConfig.getString("lobby");
			
			warzoneConfig.close();
			
			if(loadBlocks) {
				
				// zone blocks 
				ZoneVolume zoneVolume = VolumeMapper.loadZoneVolume(warzone.getName(), warzone.getName(), war, warzone.getWorld(), warzone);
				warzone.setVolume(zoneVolume);
			}
		
			// monument blocks
			for(Monument monument: warzone.getMonuments()) {
				monument.setVolume(VolumeMapper.loadVolume(monument.getName(),warzone.getName(), war, world));
			}
			
			// team spawn blocks
			for(Team team : warzone.getTeams()) {
				team.setSpawnVolume(VolumeMapper.loadVolume(team.getName(), warzone.getName(), war, world));
				if(team.getTeamFlag() != null) {
					team.setFlagVolume(VolumeMapper.loadVolume(team.getName()+"flag", warzone.getName(), war, world));
				}
			}
			
			// lobby
			BlockFace lobbyFace = null;
			if(lobbyStr != null && !lobbyStr.equals("")){
				if(lobbyStr.equals("south")) {
					lobbyFace = BlockFace.SOUTH;
				} else if(lobbyStr.equals("east")) {
					lobbyFace = BlockFace.EAST;
				} else if(lobbyStr.equals("north")) {
					lobbyFace = BlockFace.NORTH;
				} else if(lobbyStr.equals("west")) {
					lobbyFace = BlockFace.WEST;
				}
				Volume lobbyVolume = VolumeMapper.loadVolume("lobby", warzone.getName(), war, world);
				ZoneLobby lobby = new ZoneLobby(war, warzone, lobbyFace, lobbyVolume);
				warzone.setLobby(lobby);
			}
					
			return warzone;
		}
		return null;
	}
	
	public static void save(War war, Warzone warzone, boolean saveAllBlocks) {
		(new File(war.getDataFolder().getPath() +"/dat/warzone-"+warzone.getName())).mkdir();
		PropertiesFile warzoneConfig = new PropertiesFile(war.getDataFolder().getPath() + "/warzone-" + warzone.getName() + ".txt");
		//war.getLogger().info("Saving warzone " + warzone.getName() + "...");
		
		// name
		warzoneConfig.setString("name", warzone.getName());
		
		// world
		warzoneConfig.setString("world", warzone.getWorld().getName());	// default for now
		
		// teleport
		String teleportStr = "";
		Location tele = warzone.getTeleport();
		if(tele != null) {
			int intYaw = 0;
			if(tele.getYaw() >= 0){
				intYaw = (int)(tele.getYaw() % 360);
			} else {
				intYaw = (int)(360 + (tele.getYaw() % 360));
			}
			teleportStr = tele.getBlockX() + "," + tele.getBlockY() + "," + tele.getBlockZ() + "," + intYaw;
		}
		warzoneConfig.setString("teleport", teleportStr);
		
		// teams
		String teamsStr = "";
		List<Team> teams = warzone.getTeams();
		for(Team team : teams) {
			Location spawn = team.getTeamSpawn();
			int intYaw = 0;
			if(spawn.getYaw() >= 0){
				intYaw = (int)(spawn.getYaw() % 360);
			} else {
				intYaw = (int)(360 + (spawn.getYaw() % 360));
			}
			teamsStr += team.getName() + "," + spawn.getBlockX() + "," + spawn.getBlockY() + "," + spawn.getBlockZ() + "," + intYaw + ";";
		}
		warzoneConfig.setString("teams", teamsStr);
		
		// team flags
		String teamFlagsStr = "";;
		for(Team team : teams) {
			if(team.getFlagVolume() != null) {
				Location flag = team.getTeamFlag();
				int intYaw = 0;
				if(flag.getYaw() >= 0){
					intYaw = (int)(flag.getYaw() % 360);
				} else {
					intYaw = (int)(360 + (flag.getYaw() % 360));
				}
				teamFlagsStr += team.getName() + "," + flag.getBlockX() + "," + flag.getBlockY() + "," + flag.getBlockZ() + "," + intYaw + ";";
			}
		}
		warzoneConfig.setString("teamFlags", teamFlagsStr);
		
		// ff
		warzoneConfig.setBoolean("friendlyFire", warzone.getFriendlyFire());
		
		// loadout
		String loadoutStr = "";
		HashMap<Integer, ItemStack> items = warzone.getLoadout();
		for(Integer slot : items.keySet()) {
			ItemStack item = items.get(slot);
			if(item != null) {
				loadoutStr += item.getTypeId() + "," + item.getAmount() + "," + slot + ";";
			}
		}
		warzoneConfig.setString("loadout", loadoutStr);
		
		// life pool
		warzoneConfig.setInt("lifePool", warzone.getLifePool());
		
		// monument heal 		//SY
		warzoneConfig.setInt("monumentHeal", warzone.getMonumentHeal());
		
		// drawZoneOutline
		warzoneConfig.setBoolean("drawZoneOutline", warzone.isDrawZoneOutline());
		
		// autoAssignOnly
		warzoneConfig.setBoolean("autoAssignOnly", warzone.isAutoAssignOnly());
		
		// team cap
		warzoneConfig.setInt("teamCap", warzone.getTeamCap());
		
		// score cap
		warzoneConfig.setInt("scoreCap", warzone.getScoreCap());
		
		// blockHeads
		warzoneConfig.setBoolean("blockHeads", warzone.isBlockHeads());
		
		// spawnStyle
		warzoneConfig.setString("spawnStyle", warzone.getSpawnStyle());
		
		// reward
		String rewardStr = "";
		HashMap<Integer, ItemStack> rewardItems = warzone.getReward();
		for(Integer slot : rewardItems.keySet()) {
			ItemStack item = rewardItems.get(slot);
			if(item != null) {
				rewardStr += item.getTypeId() + "," + item.getAmount() + "," + slot + ";";
			}
		}
		warzoneConfig.setString("reward", rewardStr);
		
		// unbreakableZoneBlocks
		warzoneConfig.setBoolean("unbreakableZoneBlocks", warzone.isUnbreakableZoneBlocks());
		
		// disabled
		warzoneConfig.setBoolean("disabled", warzone.isDisabled());
		
		// noCreatures
		warzoneConfig.setBoolean("noCreatures", warzone.isNoCreatures());
		
		// rallyPoint
		String rpStr = "";
		Location rp = warzone.getRallyPoint();
		if(rp != null) {
			rpStr = rp.getBlockX() + "," + rp.getBlockY() + "," + rp.getBlockZ();
		}
		warzoneConfig.setString("rallyPoint", rpStr);
		
		// defaultDropLootOnDeath
		//warzoneConfig.setBoolean("dropLootOnDeath", warzone.isDropLootOnDeath());
		
		// monuments
		String monumentsStr = "";
		List<Monument> monuments = warzone.getMonuments();
		for(Monument monument : monuments) {
			Location monumentLoc = monument.getLocation();
			monumentsStr += monument.getName() + "," + monumentLoc.getBlockX() + "," + monumentLoc.getBlockY() + "," + monumentLoc.getBlockZ() + ";";
		}
		warzoneConfig.setString("monuments", monumentsStr);
		
		// lobby
		String lobbyStr = "";
		if(warzone.getLobby() != null) {
			if(BlockFace.SOUTH == warzone.getLobby().getWall()) {
				lobbyStr = "south";
			} else if(BlockFace.EAST == warzone.getLobby().getWall()) {
				lobbyStr = "east";
			} else if(BlockFace.NORTH == warzone.getLobby().getWall()) {
				lobbyStr = "north";
			} else if(BlockFace.WEST == warzone.getLobby().getWall()) {
				lobbyStr = "west";
			} 
		}
		warzoneConfig.setString("lobby", lobbyStr);
		
		warzoneConfig.save();
		warzoneConfig.close();
		
		if(saveAllBlocks) {
			// zone blocks
			VolumeMapper.save(warzone.getVolume(), warzone.getName(), war);
		}
			
		// monument blocks
		for(Monument monument: monuments) {
			VolumeMapper.save(monument.getVolume(), warzone.getName(), war);
		}
		
		// team spawn & flag blocks
		for(Team team : teams) {
			VolumeMapper.save(team.getSpawnVolume(), warzone.getName(), war);
			if(team.getFlagVolume() != null) {
				VolumeMapper.save(team.getFlagVolume(),	warzone.getName(), war);
			}
		}	
		
		if(warzone.getLobby() != null) {
			VolumeMapper.save(warzone.getLobby().getVolume(), warzone.getName(), war);
		}
		
//		if(saveBlocks) {
//			war.getLogger().info("Saved warzone " + warzone.getName() + " config and blocks.");
//		} else {
//			war.getLogger().info("Saved warzone " + warzone.getName() + " config.");
//		}
	}
	
	public static void delete(War war, String name) {
		File zoneFolder = new File(war.getDataFolder().getPath() + "/dat/warzone-" + name);
		File[] files = zoneFolder.listFiles();
		for(File file : files) {
			boolean deletedData = file.delete();
			if(!deletedData) {
				war.logWarn("Failed to delete file " + file.getName());
			}
		}
		boolean deletedData = zoneFolder.delete();
		if(!deletedData) {
			war.logWarn("Failed to delete folder " + zoneFolder.getName());
		}
		File zoneFile = new File(war.getDataFolder().getPath() + "/warzone-" + name + ".txt");
		deletedData = zoneFile.delete();
		if(!deletedData) {
			war.logWarn("Failed to delete file " + zoneFile.getName());
		}
	}

}
