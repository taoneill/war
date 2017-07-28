package com.tommytony.war.mapper;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.tommytony.war.config.WarzoneConfig;
import com.tommytony.war.structure.*;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import com.tommytony.war.Team;
import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import com.tommytony.war.config.TeamConfig;
import com.tommytony.war.config.TeamKind;
import com.tommytony.war.utility.Direction;
import com.tommytony.war.volume.Volume;
import com.tommytony.war.volume.ZoneVolume;

public class WarzoneYmlMapper {

	@SuppressWarnings("deprecation")
	public static Warzone load(String name) { // removed createNewVolume, as it did nothing
		File warzoneTxtFile = new File(War.war.getDataFolder().getPath() + "/warzone-" + name + ".txt");
		File warzoneYmlFile = new File(War.war.getDataFolder().getPath() + "/warzone-" + name + ".yml");
		
		// Convert from TXT to YML if needed
		if (warzoneTxtFile.exists() && !warzoneYmlFile.exists()) {
			// Since we're converting, WarTxtMapper didn't load the warzones. 
			// We need to load the old-text-format-Warzone into memory.
			Warzone zoneToConvert = WarzoneTxtMapper.load(name, false);
			WarzoneYmlMapper.save(zoneToConvert);
			War.war.log("Converted warzone-" + name + ".txt to warzone-" + name + ".yml", Level.INFO);
		}
		
		if (!warzoneYmlFile.exists()) {
			War.war.log("File warzone-" + name + ".yml not found", Level.WARNING);
		} else {
			YamlConfiguration warzoneYmlConfig = YamlConfiguration.loadConfiguration(warzoneYmlFile);
			ConfigurationSection warzoneRootSection = warzoneYmlConfig.getConfigurationSection("set");
			
			// Bukkit config API forces all Yml nodes to lowercase, now, it seems, sigh...
			// We need to keep this original (non-lowercase) implementation because old warzone.yml
			// files are not lowercased yet if they haven't been saved since the API change.
			String zoneInfoPrefix = "warzone." + name + ".info.";
			
			// world of the warzone
			String worldStr = warzoneRootSection.getString(zoneInfoPrefix + "world");
			if (worldStr == null) {
				// Ah! Seems that the new (post 1.2.3-ish) Bukkit config API has lowercased our map name on the previous save.
				// Retry with lowercase warzone name.
				zoneInfoPrefix = "warzone." + name.toLowerCase() + ".info.";
				worldStr = warzoneRootSection.getString(zoneInfoPrefix + "world");
			}
			World world = War.war.getServer().getWorld(worldStr);
			
			// Create the zone
			Warzone warzone = new Warzone(world, name);

			// teleport
			int teleX = warzoneRootSection.getInt(zoneInfoPrefix + "teleport.x");
			int teleY = warzoneRootSection.getInt(zoneInfoPrefix + "teleport.y");
			int teleZ = warzoneRootSection.getInt(zoneInfoPrefix + "teleport.z");
			int teleYaw = warzoneRootSection.getInt(zoneInfoPrefix + "teleport.yaw");
			warzone.setTeleport(new Location(world, teleX, teleY, teleZ, teleYaw, 0));
			
			// defaultLoadouts
			if (warzoneRootSection.contains("team.default.loadout")) {
				ConfigurationSection loadoutsSection = warzoneRootSection.getConfigurationSection("team.default.loadout");
				warzone.getDefaultInventories().setLoadouts(LoadoutYmlMapper.fromConfigToLoadouts(loadoutsSection, new HashMap<String, HashMap<Integer, ItemStack>>()));
			}

			// defaultReward
			if (warzoneRootSection.contains("team.default.reward")) {
				ConfigurationSection rewardsSection = warzoneRootSection.getConfigurationSection("team.default.reward");
				HashMap<Integer, ItemStack> reward = new HashMap<Integer, ItemStack>();
				LoadoutYmlMapper.fromConfigToLoadout(rewardsSection, reward, "default");
				warzone.getDefaultInventories().setReward(reward);
			}
			
			// Team default settings
			if (warzoneRootSection.contains("team.default.config")) {
				ConfigurationSection teamConfigSection = warzoneRootSection.getConfigurationSection("team.default.config");
				warzone.getTeamDefaultConfig().loadFrom(teamConfigSection);
			}
			
			// Warzone settings
			if (warzoneRootSection.contains("warzone." + warzone.getName() + ".config")) {
				ConfigurationSection warzoneConfigSection = warzoneRootSection.getConfigurationSection("warzone." + warzone.getName() + ".config");
				warzone.getWarzoneConfig().loadFrom(warzoneConfigSection);
			} else if (warzoneRootSection.contains("warzone." + warzone.getName().toLowerCase() + ".config")) {
				// Workaround for broken Bukkit backward-compatibility for non-lowercase Yml nodes
				ConfigurationSection warzoneConfigSection = warzoneRootSection.getConfigurationSection("warzone." + warzone.getName().toLowerCase() + ".config");
				warzone.getWarzoneConfig().loadFrom(warzoneConfigSection);
			}

			// authors
			if (warzoneRootSection.contains(zoneInfoPrefix + "authors")) {
				for(String authorStr : warzoneRootSection.getStringList(zoneInfoPrefix + "authors")) {
					if (!authorStr.equals("")) {
						warzone.addAuthor(authorStr);
					}
				}
			}

			// rallyPoint
			if (warzoneRootSection.contains(zoneInfoPrefix + "rallypoint")) {
				int rpX = warzoneRootSection.getInt(zoneInfoPrefix + "rallypoint.x");
				int rpY = warzoneRootSection.getInt(zoneInfoPrefix + "rallypoint.y");
				int rpZ = warzoneRootSection.getInt(zoneInfoPrefix + "rallypoint.z");
				int rpYaw = warzoneRootSection.getInt(zoneInfoPrefix + "rallypoint.yaw");
				Location rallyPoint = new Location(world, rpX, rpY, rpZ, rpYaw, 0);
				warzone.setRallyPoint(rallyPoint);
			}

			// monuments
			if (warzoneRootSection.contains(zoneInfoPrefix + "monument")) {
				List<String> monunmentNames = warzoneRootSection.getStringList(zoneInfoPrefix + "monument.names");
				for (String monumentName : monunmentNames) {
					if (monumentName != null && !monumentName.equals("")) {
						String monumentPrefix = zoneInfoPrefix + "monument." + monumentName + ".";
						if (!warzoneRootSection.contains(monumentPrefix + "x")) {
							// try lowercase instead
							monumentPrefix = zoneInfoPrefix + "monument." + monumentName.toLowerCase() + ".";
						}
						int monumentX = warzoneRootSection.getInt(monumentPrefix + "x");
						int monumentY = warzoneRootSection.getInt(monumentPrefix + "y");
						int monumentZ = warzoneRootSection.getInt(monumentPrefix + "z");
						int monumentYaw = warzoneRootSection.getInt(monumentPrefix + "yaw");
						Monument monument = new Monument(monumentName, warzone, new Location(world, monumentX, monumentY, monumentZ, monumentYaw, 0));
						warzone.getMonuments().add(monument);
					}
				}
			}

			// capture points
			if (warzoneRootSection.contains(zoneInfoPrefix + "capturepoint")) {
				List<String> cpNames = warzoneRootSection.getStringList(zoneInfoPrefix + "capturepoint.names");
				for (String cpName : cpNames) {
					if (cpName != null && !cpName.equals("")) {
						String cpPrefix = zoneInfoPrefix + "capturepoint." + cpName + ".";
						if (!warzoneRootSection.contains(cpPrefix + "x")) {
							// try lowercase instead
							cpPrefix = zoneInfoPrefix + "capturepoint." + cpName.toLowerCase() + ".";
						}
						int cpX = warzoneRootSection.getInt(cpPrefix + "x");
						int cpY = warzoneRootSection.getInt(cpPrefix + "y");
						int cpZ = warzoneRootSection.getInt(cpPrefix + "z");
						float cpYaw = (float) warzoneRootSection.getDouble(cpPrefix + "yaw");
						TeamKind controller = null;
						int strength = 0;
						if (warzoneRootSection.contains(cpPrefix + "controller")) {
							controller = TeamKind.teamKindFromString(warzoneRootSection.getString(cpPrefix + "controller"));
							strength = warzone.getWarzoneConfig().getInt(WarzoneConfig.CAPTUREPOINTTIME);
						}
						CapturePoint cp = new CapturePoint(cpName, new Location(world, cpX, cpY, cpZ, cpYaw, 0), controller, strength, warzone);
						warzone.getCapturePoints().add(cp);
					}
				}
			}

			// bombs
			if (warzoneRootSection.contains(zoneInfoPrefix + "bomb")) {
				List<String> bombNames = warzoneRootSection.getStringList(zoneInfoPrefix + "bomb.names");
				for (String bombName : bombNames) {
					if (bombName != null && !bombName.equals("")) {
						String bombPrefix = zoneInfoPrefix + "bomb." + bombName + ".";
						if (!warzoneRootSection.contains(bombPrefix + "x")) {
							// try lowercase instead
							bombPrefix = zoneInfoPrefix + "bomb." + bombName.toLowerCase() + ".";
						}
						int bombX = warzoneRootSection.getInt(bombPrefix + "x");
						int bombY = warzoneRootSection.getInt(bombPrefix + "y");
						int bombZ = warzoneRootSection.getInt(bombPrefix + "z");
						int bombYaw = warzoneRootSection.getInt(bombPrefix + "yaw");
						Bomb bomb = new Bomb(bombName, warzone, new Location(world, bombX, bombY, bombZ, bombYaw, 0));
						warzone.getBombs().add(bomb);
					}
				}
			}
			
			// cakes
			if (warzoneRootSection.contains(zoneInfoPrefix + "cake")) {
				List<String> cakeNames = warzoneRootSection.getStringList(zoneInfoPrefix + "cake.names");
				for (String cakeName : cakeNames) {
					if (cakeName != null && !cakeName.equals("")) {
						String cakePrefix = zoneInfoPrefix + "cake." + cakeName + ".";
						if (!warzoneRootSection.contains(cakePrefix + "x")) {
							// try lowercase instead
							cakePrefix = zoneInfoPrefix + "cake." + cakeName + ".";
						}
						int cakeX = warzoneRootSection.getInt(cakePrefix + "x");
						int cakeY = warzoneRootSection.getInt(cakePrefix + "y");
						int cakeZ = warzoneRootSection.getInt(cakePrefix + "z");
						int cakeYaw = warzoneRootSection.getInt(cakePrefix + "yaw");
						Cake cake = new Cake(cakeName, warzone, new Location(world, cakeX, cakeY, cakeZ, cakeYaw, 0));
						warzone.getCakes().add(cake);
					}
				}
			}
			
			// teams (maybe no teams)
			if (warzoneRootSection.contains("team.names")) {
				List<String> teamsNames = warzoneRootSection.getStringList("team.names");
				for (String teamName : teamsNames) {
					// team info
					String teamInfoPrefix = "team." + teamName + ".info.";
					if (!warzoneRootSection.contains(teamInfoPrefix + "spawn.x")) {
						// try lowercase instead - supports custom team names
						teamInfoPrefix = "team." + teamName.toLowerCase() + ".info.";
					}
					List<Location> teamSpawns = new ArrayList<Location>();
					if (warzoneRootSection.contains(teamInfoPrefix + "spawn")) {
						int teamX = warzoneRootSection.getInt(teamInfoPrefix + "spawn.x");
						int teamY = warzoneRootSection.getInt(teamInfoPrefix + "spawn.y");
						int teamZ = warzoneRootSection.getInt(teamInfoPrefix + "spawn.z");
						int teamYaw = warzoneRootSection.getInt(teamInfoPrefix + "spawn.yaw");
						Location teamLocation = new Location(world, teamX, teamY, teamZ, teamYaw, 0);
						teamSpawns.add(teamLocation);
						File original = new File(War.war.getDataFolder().getPath() + "/dat/warzone-" + name + "/volume-" + teamName + ".dat");
						File modified = new File(War.war.getDataFolder().getPath() + "/dat/warzone-" + name + "/volume-" + teamName + teamSpawns.indexOf(teamLocation) + ".dat");
						File originalSql = new File(War.war.getDataFolder().getPath() + "/dat/warzone-" + name + "/volume-" + teamName + ".sl3");
						File modifiedSql = new File(War.war.getDataFolder().getPath() + "/dat/warzone-" + name + "/volume-" + teamName + teamSpawns.indexOf(teamLocation) + ".sl3");
						try {
							original.renameTo(modified);
						} catch (Exception ignored) {
						}
						try {
							originalSql.renameTo(modifiedSql);
						} catch (Exception ignored) {
						}
					}
					if (warzoneRootSection.contains(teamInfoPrefix + "spawns")) {
						for (Map<?, ?> map : warzoneRootSection.getMapList(teamInfoPrefix + "spawns")) {
							int teamX = (Integer) map.get("x");
							int teamY = (Integer) map.get("y");
							int teamZ = (Integer) map.get("z");
							int teamYaw = (Integer) map.get("yaw");
							Location teamLocation = new Location(world, teamX, teamY, teamZ, teamYaw, 0);
							teamSpawns.add(teamLocation);
						}
					}
	
					Team team = new Team(teamName, TeamKind.teamKindFromString(teamName), teamSpawns, warzone);
					warzone.getTeams().add(team);
					
					if (warzoneRootSection.contains(teamInfoPrefix + "flag")) {
						int flagX = warzoneRootSection.getInt(teamInfoPrefix + "flag.x");
						int flagY = warzoneRootSection.getInt(teamInfoPrefix + "flag.y");
						int flagZ = warzoneRootSection.getInt(teamInfoPrefix + "flag.z");
						int flagYaw = warzoneRootSection.getInt(teamInfoPrefix + "flag.yaw");
						Location flagLocation = new Location(world, flagX, flagY, flagZ, flagYaw, 0);
						team.setTeamFlag(flagLocation);
					}
					
					String teamConfigPrefix = "team." + teamName + ".config";
					if (warzoneRootSection.contains(teamConfigPrefix)) {
						// team specific config
						ConfigurationSection teamConfigSection = warzoneRootSection.getConfigurationSection(teamConfigPrefix);
						team.getTeamConfig().loadFrom(teamConfigSection);
					} else if (warzoneRootSection.contains(teamConfigPrefix.toLowerCase())) {
						// try lowercase instead
						ConfigurationSection teamConfigSection = warzoneRootSection.getConfigurationSection(teamConfigPrefix.toLowerCase());
						team.getTeamConfig().loadFrom(teamConfigSection);
					}
					
					// LIFEPOOL INITIALIZATION HERE
					team.setRemainingLives(team.getTeamConfig().resolveInt(TeamConfig.LIFEPOOL));
					
					String teamLoadoutPrefix = "team." + teamName + ".loadout";
					if (warzoneRootSection.contains(teamLoadoutPrefix)) {
						// team specific loadouts
						ConfigurationSection loadoutsSection = warzoneRootSection.getConfigurationSection(teamLoadoutPrefix);
						team.getInventories().setLoadouts(LoadoutYmlMapper.fromConfigToLoadouts(loadoutsSection, new HashMap<String, HashMap<Integer, ItemStack>>()));
					} else if (warzoneRootSection.contains(teamLoadoutPrefix.toLowerCase())) {
						// try lowercase instead
						ConfigurationSection loadoutsSection = warzoneRootSection.getConfigurationSection(teamLoadoutPrefix.toLowerCase());
						team.getInventories().setLoadouts(LoadoutYmlMapper.fromConfigToLoadouts(loadoutsSection, new HashMap<String, HashMap<Integer, ItemStack>>()));
					} 
					
					String teamRewardPrefix = "team." + teamName + ".reward";
					if (warzoneRootSection.contains(teamRewardPrefix)) {
						// team specific reward
						ConfigurationSection rewardsSection = warzoneRootSection.getConfigurationSection(teamRewardPrefix);
						HashMap<Integer, ItemStack> reward = new HashMap<Integer, ItemStack>();
						LoadoutYmlMapper.fromConfigToLoadout(rewardsSection, reward, "default");
						warzone.getDefaultInventories().setReward(reward);
					} else if (warzoneRootSection.contains(teamRewardPrefix.toLowerCase())) {
						// try lowercase instead
						ConfigurationSection rewardsSection = warzoneRootSection.getConfigurationSection(teamRewardPrefix.toLowerCase());
						HashMap<Integer, ItemStack> reward = new HashMap<Integer, ItemStack>();
						LoadoutYmlMapper.fromConfigToLoadout(rewardsSection, reward, "default");
						warzone.getDefaultInventories().setReward(reward);
					} 
				}
			}
			Connection connection = null;
			try {
				connection = ZoneVolumeMapper.getZoneConnection(warzone.getVolume(), warzone.getName(), warzone.getWorld());
			} catch (SQLException e) {
				War.war.getLogger().log(Level.WARNING, "Failed to load warzone structures volume", e);
			}
			// monument blocks
			for (Monument monument : warzone.getMonuments()) {
				try {
					monument.setVolume(warzone.loadStructure(monument.getName(), connection));
				} catch (SQLException e) {
					War.war.getLogger().log(Level.WARNING, "Failed to load warzone structures volume", e);
				}
			}

			// capture point blocks
			for (CapturePoint cp : warzone.getCapturePoints()) {
				try {
					cp.setVolume(warzone.loadStructure("cp-" + cp.getName(), connection));
				} catch (SQLException e) {
					War.war.getLogger().log(Level.WARNING, "Failed to load warzone structures volume", e);
				}
			}

			// bomb blocks
			for (Bomb bomb : warzone.getBombs()) {
				try {
					bomb.setVolume(warzone.loadStructure("bomb-" + bomb.getName(), connection));
				} catch (SQLException e) {
					War.war.getLogger().log(Level.WARNING, "Failed to load warzone structures volume", e);
				}
			}
			
			// cake blocks
			for (Cake cake : warzone.getCakes()) {
				try {
					cake.setVolume(warzone.loadStructure("cake-" + cake.getName(), connection));
				} catch (SQLException e) {
					War.war.getLogger().log(Level.WARNING, "Failed to load warzone structures volume", e);
				}
			}
			
			// team spawn blocks
			for (Team team : warzone.getTeams()) {
				for (Location teamSpawn : team.getTeamSpawns()) {
					try {
						team.setSpawnVolume(teamSpawn, warzone.loadStructure(team.getName() + team.getTeamSpawns().indexOf(teamSpawn), connection));
					} catch (SQLException e) {
						War.war.getLogger().log(Level.WARNING, "Failed to load warzone structures volume", e);
					}
				}
				if (team.getTeamFlag() != null) {
					try {
						team.setFlagVolume(warzone.loadStructure(team.getName() + "flag", connection));
					} catch (SQLException e) {
						War.war.getLogger().log(Level.WARNING, "Failed to load warzone structures volume", e);
					}
				}
			}

			// lobby
			String lobbyPrefix = zoneInfoPrefix + "lobby.";
			
			// lobby orientation
			String lobbyOrientation = warzoneRootSection.getString(lobbyPrefix + "orientation");
			BlockFace lobbyFace = null;
			if (lobbyOrientation.equals("south")) {
				lobbyFace = Direction.SOUTH();
			} else if (lobbyOrientation.equals("east")) {
				lobbyFace = Direction.EAST();
			} else if (lobbyOrientation.equals("north")) {
				lobbyFace = Direction.NORTH();
			} else if (lobbyOrientation.equals("west")) {
				lobbyFace = Direction.WEST();
			}
			
			// lobby materials
			if (warzoneRootSection.isItemStack(lobbyPrefix + "materials.floor")) {
				warzone.getLobbyMaterials().setFloorBlock(
						warzoneRootSection.getItemStack(lobbyPrefix + "materials.floor"));
			} else {
				ConfigurationSection floorMaterialSection = warzoneRootSection
						.getConfigurationSection(lobbyPrefix + "materials.floor");
				if (floorMaterialSection != null) {
					warzone.getLobbyMaterials().setFloorBlock(
						new ItemStack(floorMaterialSection.getInt("id"), 1,
							(short) floorMaterialSection.getInt("data")));
				}
			}
			if (warzoneRootSection.isItemStack(lobbyPrefix + "materials.outline")) {
				warzone.getLobbyMaterials().setOutlineBlock(
						warzoneRootSection.getItemStack(lobbyPrefix + "materials.outline"));
			} else {
				ConfigurationSection floorMaterialSection = warzoneRootSection
						.getConfigurationSection(lobbyPrefix + "materials.outline");
				if (floorMaterialSection != null) {
					warzone.getLobbyMaterials().setOutlineBlock(
						new ItemStack(floorMaterialSection.getInt("id"), 1,
							(short) floorMaterialSection.getInt("data")));
				}
			}
			if (warzoneRootSection.isItemStack(lobbyPrefix + "materials.gate")) {
				warzone.getLobbyMaterials().setGateBlock(
						warzoneRootSection.getItemStack(lobbyPrefix + "materials.gate"));
			} else {
				ConfigurationSection floorMaterialSection = warzoneRootSection
						.getConfigurationSection(lobbyPrefix + "materials.gate");
				if (floorMaterialSection != null) {
					warzone.getLobbyMaterials().setGateBlock(
						new ItemStack(floorMaterialSection.getInt("id"), 1,
							(short) floorMaterialSection.getInt("data")));
				}
			}
			if (warzoneRootSection.isItemStack(lobbyPrefix + "materials.light")) {
				warzone.getLobbyMaterials().setLightBlock(
						warzoneRootSection.getItemStack(lobbyPrefix + "materials.light"));
			} else {
				ConfigurationSection floorMaterialSection = warzoneRootSection
						.getConfigurationSection(lobbyPrefix + "materials.light");
				if (floorMaterialSection != null) {
					warzone.getLobbyMaterials().setLightBlock(
						new ItemStack(floorMaterialSection.getInt("id"), 1,
							(short) floorMaterialSection.getInt("data")));
				}
			}
			
			// lobby world
			String lobbyWorldName = warzoneRootSection.getString(lobbyPrefix + "world");
			World lobbyWorld = War.war.getServer().getWorld(lobbyWorldName);
						
			// create the lobby
			Volume lobbyVolume = null;
			try {
				lobbyVolume = warzone.loadStructure("lobby", lobbyWorld, connection);
			} catch (SQLException e) {
				War.war.getLogger().log(Level.WARNING, "Failed to load warzone lobby", e);
			}
			ZoneLobby lobby = new ZoneLobby(warzone, lobbyFace, lobbyVolume);
			warzone.setLobby(lobby);
			
			// warzone materials
			if (warzoneRootSection.isItemStack(zoneInfoPrefix + "materials.main")) {
				warzone.getWarzoneMaterials().setMainBlock(
						warzoneRootSection.getItemStack(zoneInfoPrefix + "materials.main"));
			} else {
				ConfigurationSection floorMaterialSection = warzoneRootSection
						.getConfigurationSection(zoneInfoPrefix + "materials.main");
				if (floorMaterialSection != null) {
					warzone.getWarzoneMaterials().setMainBlock(
						new ItemStack(floorMaterialSection.getInt("id"), 1,
							(short) floorMaterialSection.getInt("data")));
				}
			}
			if (warzoneRootSection.isItemStack(zoneInfoPrefix + "materials.stand")) {
				warzone.getWarzoneMaterials().setStandBlock(
						warzoneRootSection.getItemStack(zoneInfoPrefix + "materials.stand"));
			} else {
				ConfigurationSection floorMaterialSection = warzoneRootSection
						.getConfigurationSection(zoneInfoPrefix + "materials.stand");
				if (floorMaterialSection != null) {
					warzone.getWarzoneMaterials().setStandBlock(
						new ItemStack(floorMaterialSection.getInt("id"), 1,
							(short) floorMaterialSection.getInt("data")));
				}
			}
			if (warzoneRootSection.isItemStack(zoneInfoPrefix + "materials.light")) {
				warzone.getWarzoneMaterials().setLightBlock(
						warzoneRootSection.getItemStack(zoneInfoPrefix + "materials.light"));
			} else {
				ConfigurationSection floorMaterialSection = warzoneRootSection
						.getConfigurationSection(zoneInfoPrefix + "materials.light");
				if (floorMaterialSection != null) {
					warzone.getWarzoneMaterials().setLightBlock(
						new ItemStack(floorMaterialSection.getInt("id"), 1,
							(short) floorMaterialSection.getInt("data")));
				}
			}
			try {
				connection.close();
			} catch (SQLException ignored) {
			}

			return warzone;
		}
		
		return null;
	}
	
	public static void save(Warzone warzone) {
		YamlConfiguration warzoneYmlConfig = new YamlConfiguration();
		ConfigurationSection warzoneRootSection = warzoneYmlConfig.createSection("set");
		(new File(War.war.getDataFolder().getPath() + "/dat/warzone-" + warzone.getName())).mkdir();	// create folder
		
		ConfigurationSection warzoneSection = warzoneRootSection.createSection("warzone." + warzone.getName());
		
		// Warzone settings
		if (!warzone.getWarzoneConfig().isEmpty()) {
			ConfigurationSection warzoneConfigSection = warzoneSection.createSection("config");
			warzone.getWarzoneConfig().saveTo(warzoneConfigSection);
		}
				
		ConfigurationSection warzoneInfoSection = warzoneSection.createSection("info");
		
		// authors
		warzoneInfoSection.set("authors", warzone.getAuthors());
		
		// teleport
		ConfigurationSection teleSection = warzoneInfoSection.createSection("teleport");
		teleSection.set("x", warzone.getTeleport().getBlockX());
		teleSection.set("y", warzone.getTeleport().getBlockY());
		teleSection.set("z", warzone.getTeleport().getBlockZ());
		teleSection.set("yaw", toIntYaw(warzone.getTeleport().getYaw()));
	
		// world
		warzoneInfoSection.set("world", warzone.getWorld().getName());	
		
		// lobby
		if (warzone.getLobby() != null) {
			String lobbyOrientation = "";
			if (Direction.SOUTH() == warzone.getLobby().getWall()) {
				lobbyOrientation = "south";
			} else if (Direction.EAST() == warzone.getLobby().getWall()) {
				lobbyOrientation = "east";
			} else if (Direction.NORTH() == warzone.getLobby().getWall()) {
				lobbyOrientation = "north";
			} else if (Direction.WEST() == warzone.getLobby().getWall()) {
				lobbyOrientation = "west";
			}
			
			ConfigurationSection lobbySection = warzoneInfoSection.createSection("lobby");
			lobbySection.set("orientation", lobbyOrientation);
			lobbySection.set("world", warzone.getLobby().getVolume().getWorld().getName());
			
			lobbySection.set("materials.floor", warzone.getLobbyMaterials().getFloorBlock());
			lobbySection.set("materials.outline", warzone.getLobbyMaterials().getOutlineBlock());
			lobbySection.set("materials.gate", warzone.getLobbyMaterials().getGateBlock());
			lobbySection.set("materials.light", warzone.getLobbyMaterials().getLightBlock());
		}
		
		// materials
		if (warzone.getLobby() != null) {
			warzoneInfoSection.set("materials.main", warzone.getWarzoneMaterials().getMainBlock());
			warzoneInfoSection.set("materials.stand", warzone.getWarzoneMaterials().getStandBlock());
			warzoneInfoSection.set("materials.light", warzone.getWarzoneMaterials().getLightBlock());
		}

		// rallyPoint
		if (warzone.getRallyPoint() != null) {
			ConfigurationSection rpSection = warzoneInfoSection.createSection("rallypoint");
			rpSection.set("x", warzone.getRallyPoint().getBlockX());
			rpSection.set("y", warzone.getRallyPoint().getBlockY());
			rpSection.set("z", warzone.getRallyPoint().getBlockZ());
			rpSection.set("yaw", toIntYaw(warzone.getRallyPoint().getYaw()));
		}
		
		// monuments
		if (warzone.getMonuments().size() > 0) {
			ConfigurationSection monumentsSection = warzoneInfoSection.createSection("monument");
			
			List<String> monumentNames = new ArrayList<String>();
			for (Monument monument : warzone.getMonuments()) {
				monumentNames.add(monument.getName());
			}
			monumentsSection.set("names", monumentNames);
			
			for (Monument monument : warzone.getMonuments()) {
				
				ConfigurationSection monumentSection = monumentsSection.createSection(monument.getName());
				monumentSection.set("x", monument.getLocation().getBlockX());
				monumentSection.set("y", monument.getLocation().getBlockY());
				monumentSection.set("z", monument.getLocation().getBlockZ());
				monumentSection.set("yaw", toIntYaw(monument.getLocation().getYaw()));
			}
		}

		// capture points
		if (warzone.getCapturePoints().size() > 0) {
			ConfigurationSection cpsSection = warzoneInfoSection.createSection("capturepoint");

			List<String> cpNames = new ArrayList<String>();
			for (CapturePoint cp : warzone.getCapturePoints()) {
				cpNames.add(cp.getName());
			}
			cpsSection.set("names", cpNames);

			for (CapturePoint cp : warzone.getCapturePoints()) {

				ConfigurationSection cpSection = cpsSection.createSection(cp.getName());
				cpSection.set("x", cp.getLocation().getBlockX());
				cpSection.set("y", cp.getLocation().getBlockY());
				cpSection.set("z", cp.getLocation().getBlockZ());
				cpSection.set("yaw", cp.getLocation().getYaw());
				if (cp.getDefaultController() != null) {
					cpSection.set("controller", cp.getDefaultController().name());
				}
			}
		}

		// bombs
		if (warzone.getBombs().size() > 0) {
			ConfigurationSection bombsSection = warzoneInfoSection.createSection("bomb");

			List<String> bombNames = new ArrayList<String>();
			for (Bomb bomb : warzone.getBombs()) {
				bombNames.add(bomb.getName());
			}
			bombsSection.set("names", bombNames);

			for (Bomb bomb : warzone.getBombs()) {

				ConfigurationSection bombSection = bombsSection.createSection(bomb.getName());
				bombSection.set("x", bomb.getLocation().getBlockX());
				bombSection.set("y", bomb.getLocation().getBlockY());
				bombSection.set("z", bomb.getLocation().getBlockZ());
				bombSection.set("yaw", toIntYaw(bomb.getLocation().getYaw()));
			}
		}

		// cakes
		if (warzone.getCakes().size() > 0) {
			ConfigurationSection cakesSection = warzoneInfoSection.createSection("cake");
			
			List<String> cakeNames = new ArrayList<String>();
			for (Cake cake : warzone.getCakes()) {
				cakeNames.add(cake.getName());
			}
			cakesSection.set("names", cakeNames);
			
			for (Cake cake : warzone.getCakes()) {
				
				ConfigurationSection cakeSection = cakesSection.createSection(cake.getName());
				cakeSection.set("x", cake.getLocation().getBlockX());
				cakeSection.set("y", cake.getLocation().getBlockY());
				cakeSection.set("z", cake.getLocation().getBlockZ());
				cakeSection.set("yaw", toIntYaw(cake.getLocation().getYaw()));
			}
		}
		
		ConfigurationSection teamsSection = warzoneRootSection.createSection("team");
		
		// teams
		List<Team> teams = warzone.getTeams();
		
		List<String> teamNames = new ArrayList<String>();
		for (Team team : teams) {
			teamNames.add(team.getName());
		}
		if (teamNames.size() > 0) {
			teamsSection.set("names", teamNames);
		}

		// Team default settings
		if (!warzone.getTeamDefaultConfig().isEmpty()) {
			ConfigurationSection teamConfigSection = teamsSection.createSection("default.config");
			warzone.getTeamDefaultConfig().saveTo(teamConfigSection);
		}		
		
		// defaultLoadouts
		if (warzone.getDefaultInventories().hasLoadouts()) {
			ConfigurationSection loadoutsSection = teamsSection.createSection("default.loadout");
			LoadoutYmlMapper.fromLoadoutsToConfig(warzone.getDefaultInventories().getNewLoadouts(), loadoutsSection);
		}
		
		// defaultReward
		if (warzone.getDefaultInventories().hasReward()) {
			ConfigurationSection rewardsSection = teamsSection.createSection("default.reward");
			LoadoutYmlMapper.fromLoadoutToConfig("default", warzone.getDefaultInventories().getReward(), rewardsSection);
		}	
		
		for (Team team : teams) {
			if (!team.getTeamConfig().isEmpty()) {
				// team specific config
				ConfigurationSection teamConfigSection = teamsSection.createSection(team.getName() + ".config");
				team.getTeamConfig().saveTo(teamConfigSection);
			}
			
			if (team.getInventories().hasLoadouts()) {
				// team specific loadouts
				ConfigurationSection loadoutsSection = teamsSection.createSection(team.getName() + ".loadout");
				LoadoutYmlMapper.fromLoadoutsToConfig(team.getInventories().getNewLoadouts(), loadoutsSection);
			}
			
			if (team.getInventories().hasReward()) {
				// team specific reward
				ConfigurationSection rewardsSection = teamsSection.createSection(team.getName() + ".reward");
				LoadoutYmlMapper.fromLoadoutToConfig("default", team.getInventories().getReward(), rewardsSection);
			}

			ConfigurationSection teamInfoSection = teamsSection.createSection(team.getName() + ".info");
			
			List<Map<String, Object>> spawnSerilization = new ArrayList<Map<String, Object>>();
			for (Location spawn : team.getTeamSpawns()) {
				Map<String, Object> map = new HashMap<String, Object>();
				map.put("x", spawn.getBlockX());
				map.put("y", spawn.getBlockY());
				map.put("z", spawn.getBlockZ());
				map.put("yaw", toIntYaw(spawn.getYaw()));
				spawnSerilization.add(map);
			}
			teamInfoSection.set("spawns", spawnSerilization);
			
			if (team.getTeamFlag() != null) {
				ConfigurationSection flagSection = teamInfoSection.createSection("flag");
				Location teamFlag = team.getTeamFlag();
				flagSection.set("x", teamFlag.getBlockX());
				flagSection.set("y", teamFlag.getBlockY());
				flagSection.set("z", teamFlag.getBlockZ());
				flagSection.set("yaw", toIntYaw(teamFlag.getYaw()));
			}
		}
		Connection connection = null;
		try {
			connection = ZoneVolumeMapper.getZoneConnection(warzone.getVolume(), warzone.getName(), warzone.getWorld());
		} catch (SQLException e) {
			War.war.getLogger().log(Level.WARNING, "Failed to load warzone structures volume", e);
		}
		// monument blocks
		for (Monument monument : warzone.getMonuments()) {
			try {
				ZoneVolumeMapper.saveStructure(monument.getVolume(), connection);
			} catch (SQLException e) {
				War.war.getLogger().log(Level.WARNING, "Failed to save warzone structures volume", e);
			}
		}

		// capture point blocks
		for (CapturePoint cp : warzone.getCapturePoints()) {
			try {
				ZoneVolumeMapper.saveStructure(cp.getVolume(), connection);
			} catch (SQLException e) {
				War.war.getLogger().log(Level.WARNING, "Failed to save warzone structures volume", e);
			}
		}

		// bomb blocks
		for (Bomb bomb : warzone.getBombs()) {
			try {
				ZoneVolumeMapper.saveStructure(bomb.getVolume(), connection);
			} catch (SQLException e) {
				War.war.getLogger().log(Level.WARNING, "Failed to save warzone structures volume", e);
			}
		}
		
		// cake blocks
		for (Cake cake : warzone.getCakes()) {
			try {
				ZoneVolumeMapper.saveStructure(cake.getVolume(), connection);
			} catch (SQLException e) {
				War.war.getLogger().log(Level.WARNING, "Failed to save warzone structures volume", e);
			}
		}

		// team spawn & flag blocks
		for (Team team : teams) {
			for (Volume volume : team.getSpawnVolumes().values()) {
				try {
					ZoneVolumeMapper.saveStructure(volume, connection);
				} catch (SQLException e) {
					War.war.getLogger().log(Level.WARNING, "Failed to save warzone structures volume", e);
				}
			}
			if (team.getFlagVolume() != null) {
				try {
					ZoneVolumeMapper.saveStructure(team.getFlagVolume(), connection);
				} catch (SQLException e) {
					War.war.getLogger().log(Level.WARNING, "Failed to save warzone structures volume", e);
				}
			}
		}

		if (warzone.getLobby() != null) {
			try {
				ZoneVolumeMapper.saveStructure(warzone.getLobby().getVolume(), connection);
			} catch (SQLException e) {
				War.war.getLogger().log(Level.WARNING, "Failed to save warzone structures volume", e);
			}
		}
		try {
			connection.close();
		} catch (SQLException ignored) {
		}

		// Save to disk
		try {
			File warzoneConfigFile = new File(War.war.getDataFolder().getPath() + "/warzone-" + warzone.getName() + ".yml");
			warzoneYmlConfig.save(warzoneConfigFile);
		} catch (IOException e) {
			War.war.log("Failed to save warzone-" + warzone.getName() + ".yml", Level.WARNING);
			e.printStackTrace();
		}
	}

	private static int toIntYaw(float yaw) {
		int intYaw = 0;
		if (yaw >= 0) {
			intYaw = (int) (yaw % 360);
		} else {
			intYaw = (int) (360 + (yaw % 360));
		}
		return intYaw;
	}

	public static void delete(Warzone zone) {
		// Kill old warzone, but use it to create the renamed copy
		zone.unload();
		zone.getVolume().resetBlocks();	// We're need a clean land
		
		String name = zone.getName();
				
		// Move old files
		(new File(War.war.getDataFolder().getPath() + "/temp/deleted/")).mkdir();
		(new File(War.war.getDataFolder().getPath() + "/warzone-" + name + ".yml")).renameTo(new File(War.war.getDataFolder().getPath() + "/temp/deleted/warzone-" + name + ".yml"));
		(new File(War.war.getDataFolder().getPath() + "/temp/deleted/dat/warzone-" + name)).mkdirs();

		String oldPath = War.war.getDataFolder().getPath() + "/dat/warzone-" + name + "/";
		File oldZoneFolder = new File(oldPath);
		File[] oldZoneFiles = oldZoneFolder.listFiles();
		for (File file : oldZoneFiles) {
			file.renameTo(new File(War.war.getDataFolder().getPath() + "/temp/deleted/dat/warzone-" + name + "/" + file.getName()));
		}
		oldZoneFolder.delete();
	}
}
