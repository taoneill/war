package com.tommytony.war.mapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

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
import com.tommytony.war.structure.Bomb;
import com.tommytony.war.structure.Cake;
import com.tommytony.war.structure.HubLobbyMaterials;
import com.tommytony.war.structure.Monument;
import com.tommytony.war.structure.WarzoneMaterials;
import com.tommytony.war.structure.ZoneLobby;
import com.tommytony.war.utility.Direction;
import com.tommytony.war.volume.Volume;
import com.tommytony.war.volume.ZoneVolume;

public class WarzoneYmlMapper {

	public static Warzone load(String name, boolean createNewVolume) {
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
				LoadoutYmlMapper.fromConfigToLoadouts(loadoutsSection, warzone.getDefaultInventories().getLoadouts());
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
					int teamX = warzoneRootSection.getInt(teamInfoPrefix + "spawn.x");
					int teamY = warzoneRootSection.getInt(teamInfoPrefix + "spawn.y");
					int teamZ = warzoneRootSection.getInt(teamInfoPrefix + "spawn.z");
					int teamYaw = warzoneRootSection.getInt(teamInfoPrefix + "spawn.yaw");
					Location teamLocation = new Location(world, teamX, teamY, teamZ, teamYaw, 0);
	
					Team team = new Team(teamName, TeamKind.teamKindFromString(teamName), teamLocation, warzone);
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
						LoadoutYmlMapper.fromConfigToLoadouts(loadoutsSection, team.getInventories().getLoadouts());
					} else if (warzoneRootSection.contains(teamLoadoutPrefix.toLowerCase())) {
						// try lowercase instead
						ConfigurationSection loadoutsSection = warzoneRootSection.getConfigurationSection(teamLoadoutPrefix.toLowerCase());
						LoadoutYmlMapper.fromConfigToLoadouts(loadoutsSection, team.getInventories().getLoadouts());
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

			if (createNewVolume) {
				ZoneVolume zoneVolume = new ZoneVolume(warzone.getName(), world, warzone);
				warzone.setVolume(zoneVolume);
			}

			// monument blocks
			for (Monument monument : warzone.getMonuments()) {
				monument.setVolume(VolumeMapper.loadVolume(monument.getName(), warzone.getName(), world));
			}
			
			// bomb blocks
			for (Bomb bomb : warzone.getBombs()) {
				bomb.setVolume(VolumeMapper.loadVolume("bomb-" + bomb.getName(), warzone.getName(), world));
			}
			
			// cake blocks
			for (Cake cake : warzone.getCakes()) {
				cake.setVolume(VolumeMapper.loadVolume("cake-" + cake.getName(), warzone.getName(), world));
			}
			
			// team spawn blocks
			for (Team team : warzone.getTeams()) {
				team.setSpawnVolume(VolumeMapper.loadVolume(team.getName(), warzone.getName(), world));
				if (team.getTeamFlag() != null) {
					team.setFlagVolume(VolumeMapper.loadVolume(team.getName() + "flag", warzone.getName(), world));
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
			int floorId = War.war.getWarhubMaterials().getFloorId();	// default warhub
			int floorData = War.war.getWarhubMaterials().getFloorData();	
			ConfigurationSection floorMaterialSection = warzoneRootSection.getConfigurationSection(lobbyPrefix + "materials.floor");
			if (floorMaterialSection != null) {
				floorId = floorMaterialSection.getInt("id");
				floorData = floorMaterialSection.getInt("data");
			}
			
			int outlineId = War.war.getWarhubMaterials().getOutlineId();
			int outlineData = War.war.getWarhubMaterials().getOutlineData();	
			ConfigurationSection outlineMaterialSection = warzoneRootSection.getConfigurationSection(lobbyPrefix + "materials.outline");
			if (outlineMaterialSection != null) {
				outlineId = outlineMaterialSection.getInt("id");
				outlineData = outlineMaterialSection.getInt("data");
			}
			
			int gateId = War.war.getWarhubMaterials().getGateId();
			int gateData = War.war.getWarhubMaterials().getGateData();;	
			ConfigurationSection gateMaterialSection = warzoneRootSection.getConfigurationSection(lobbyPrefix + "materials.gate");
			if (gateMaterialSection != null) {
				gateId = gateMaterialSection.getInt("id");
				gateData = gateMaterialSection.getInt("data");
			}
			
			int lobbyLightId = War.war.getWarhubMaterials().getLightId();
			int lobbyLightData = War.war.getWarhubMaterials().getLightData();	
			ConfigurationSection lobbyLightMaterialSection = warzoneRootSection.getConfigurationSection(lobbyPrefix + "materials.light");
			if (lobbyLightMaterialSection != null) {
				lobbyLightId = lobbyLightMaterialSection.getInt("id");
				lobbyLightData = lobbyLightMaterialSection.getInt("data");
			}
			
			warzone.setLobbyMaterials(new HubLobbyMaterials(floorId, (byte)floorData, outlineId, (byte)outlineData, gateId, (byte)gateData, lobbyLightId, (byte)lobbyLightData));
			
			// lobby world
			String lobbyWorldName = warzoneRootSection.getString(lobbyPrefix + "world");
			World lobbyWorld = War.war.getServer().getWorld(lobbyWorldName);
						
			// create the lobby
			Volume lobbyVolume = VolumeMapper.loadVolume("lobby", warzone.getName(), lobbyWorld);
			ZoneLobby lobby = new ZoneLobby(warzone, lobbyFace, lobbyVolume);
			warzone.setLobby(lobby);
			
			// warzone materials
			int mainId = warzone.getWarzoneMaterials().getMainId();
			int mainData = warzone.getWarzoneMaterials().getMainData();	
			ConfigurationSection mainMaterialSection = warzoneRootSection.getConfigurationSection(zoneInfoPrefix + "materials.main");
			if (mainMaterialSection != null) {
				mainId = mainMaterialSection.getInt("id");
				mainData = mainMaterialSection.getInt("data");
			}
			
			int standId = warzone.getWarzoneMaterials().getStandId();
			int standData = warzone.getWarzoneMaterials().getStandData();	
			ConfigurationSection standMaterialSection = warzoneRootSection.getConfigurationSection(zoneInfoPrefix + "materials.stand");
			if (standMaterialSection != null) {
				standId = standMaterialSection.getInt("id");
				standData = standMaterialSection.getInt("data");
			}
			
			int lightId = warzone.getWarzoneMaterials().getLightId();
			int lightData = warzone.getWarzoneMaterials().getLightData();	
			ConfigurationSection lightMaterialSection = warzoneRootSection.getConfigurationSection(zoneInfoPrefix + "materials.light");
			if (lightMaterialSection != null) {
				lightId = lightMaterialSection.getInt("id");
				lightData = lightMaterialSection.getInt("data");
			}
			
			warzone.setWarzoneMaterials(new WarzoneMaterials(mainId, (byte)mainData, standId, (byte)standData, lightId, (byte)lightData));
			
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
			
			ConfigurationSection floorSection = lobbySection.createSection("materials.floor");
			floorSection.set("id", warzone.getLobbyMaterials().getFloorId());
			floorSection.set("data", warzone.getLobbyMaterials().getFloorData());
			ConfigurationSection outlineSection = lobbySection.createSection("materials.outline");
			outlineSection.set("id", warzone.getLobbyMaterials().getOutlineId());
			outlineSection.set("data", warzone.getLobbyMaterials().getOutlineData());
			ConfigurationSection gateSection = lobbySection.createSection("materials.gate");
			gateSection.set("id", warzone.getLobbyMaterials().getGateId());
			gateSection.set("data", warzone.getLobbyMaterials().getGateData());
			ConfigurationSection lightSection = lobbySection.createSection("materials.light");
			lightSection.set("id", warzone.getLobbyMaterials().getLightId());
			lightSection.set("data", warzone.getLobbyMaterials().getLightData());
		}
		
		// materials
		if (warzone.getLobby() != null) {
			ConfigurationSection mainSection = warzoneInfoSection.createSection("materials.main");
			mainSection.set("id", warzone.getWarzoneMaterials().getMainId());
			mainSection.set("data", warzone.getWarzoneMaterials().getMainData());
			ConfigurationSection standSection = warzoneInfoSection.createSection("materials.stand");
			standSection.set("id", warzone.getWarzoneMaterials().getStandId());
			standSection.set("data", warzone.getWarzoneMaterials().getStandData());
			ConfigurationSection lightSection = warzoneInfoSection.createSection("materials.light");
			lightSection.set("id", warzone.getWarzoneMaterials().getLightId());
			lightSection.set("data", warzone.getWarzoneMaterials().getLightData());
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
			LoadoutYmlMapper.fromLoadoutsToConfig(warzone.getDefaultInventories().getLoadouts(), loadoutsSection);
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
				LoadoutYmlMapper.fromLoadoutsToConfig(team.getInventories().getLoadouts(), loadoutsSection);
			}
			
			if (team.getInventories().hasReward()) {
				// team specific reward
				ConfigurationSection rewardsSection = teamsSection.createSection(team.getName() + ".reward");
				LoadoutYmlMapper.fromLoadoutToConfig("default", team.getInventories().getReward(), rewardsSection);
			}

			ConfigurationSection teamInfoSection = teamsSection.createSection(team.getName() + ".info");
			
			ConfigurationSection spawnSection = teamInfoSection.createSection("spawn");
			Location spawn = team.getTeamSpawn();
			spawnSection.set("x", spawn.getBlockX());
			spawnSection.set("y", spawn.getBlockY());
			spawnSection.set("z", spawn.getBlockZ());
			spawnSection.set("yaw", toIntYaw(spawn.getYaw()));
			
			if (team.getTeamFlag() != null) {
				ConfigurationSection flagSection = teamInfoSection.createSection("flag");
				Location teamFlag = team.getTeamFlag();
				flagSection.set("x", teamFlag.getBlockX());
				flagSection.set("y", teamFlag.getBlockY());
				flagSection.set("z", teamFlag.getBlockZ());
				flagSection.set("yaw", toIntYaw(teamFlag.getYaw()));
			}
		}
		
		// monument blocks
		for (Monument monument : warzone.getMonuments()) {
			VolumeMapper.save(monument.getVolume(), warzone.getName());
		}
		
		// bomb blocks
		for (Bomb bomb : warzone.getBombs()) {
			VolumeMapper.save(bomb.getVolume(), warzone.getName());
		}
		
		// cake blocks
		for (Cake cake : warzone.getCakes()) {
			VolumeMapper.save(cake.getVolume(), warzone.getName());
		}

		// team spawn & flag blocks
		for (Team team : teams) {
			VolumeMapper.save(team.getSpawnVolume(), warzone.getName());
			if (team.getFlagVolume() != null) {
				VolumeMapper.save(team.getFlagVolume(), warzone.getName());
			}
		}

		if (warzone.getLobby() != null) {
			VolumeMapper.save(warzone.getLobby().getVolume(), warzone.getName());
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
