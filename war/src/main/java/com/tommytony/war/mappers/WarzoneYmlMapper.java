package com.tommytony.war.mappers;

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

import bukkit.tommytony.war.War;

import com.tommytony.war.Bomb;
import com.tommytony.war.Cake;
import com.tommytony.war.Monument;
import com.tommytony.war.Team;
import com.tommytony.war.TeamKind;
import com.tommytony.war.Warzone;
import com.tommytony.war.ZoneLobby;
import com.tommytony.war.config.TeamConfig;
import com.tommytony.war.volumes.Volume;
import com.tommytony.war.volumes.ZoneVolume;

public class WarzoneYmlMapper {

	public static Warzone load(String name, boolean createNewVolume) {
		File warzoneTxtFile = new File(War.war.getDataFolder().getPath() + "/warzone-" + name + ".txt");
		File warzoneYmlFile = new File(War.war.getDataFolder().getPath() + "/warzone-" + name + ".yml");
		
		// Convert from TXT to YML if needed
		if (warzoneTxtFile.exists() && !warzoneYmlFile.exists()) {
			// Since we're converting, WarTxtMapper didn't load the warzones. 
			// We need to load the old-text-format-Warzone into memory.
			Warzone zoneToConvert = WarzoneTxtMapper.load(name, false);
			WarzoneYmlMapper.save(zoneToConvert, false);
			War.war.log("Converted warzone-" + name + ".txt to warzone-" + name + ".yml", Level.INFO);
		}
		
		if (!warzoneYmlFile.exists()) {
			War.war.log("File warzone-" + name + ".yml not found", Level.WARNING);
		} else {
			YamlConfiguration warzoneYmlConfig = YamlConfiguration.loadConfiguration(warzoneYmlFile);
			ConfigurationSection warzoneRootSection = warzoneYmlConfig.getConfigurationSection("set");
			
			String zoneInfoPrefix = "warzone." + name + ".info.";
			
			// world
			String worldStr = warzoneRootSection.getString(zoneInfoPrefix + "world");
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
					}
					
					team.setRemainingLives(team.getTeamConfig().resolveInt(TeamConfig.LIFEPOOL));
					
					String teamLoadoutPrefix = "team." + teamName + ".loadout";
					if (warzoneRootSection.contains(teamLoadoutPrefix)) {
						// team specific loadouts
						ConfigurationSection loadoutsSection = warzoneRootSection.getConfigurationSection(teamLoadoutPrefix);
						LoadoutYmlMapper.fromConfigToLoadouts(loadoutsSection, team.getInventories().getLoadouts());
					}
					
					String teamRewardPrefix = "team." + teamName + ".reward";
					if (warzoneRootSection.contains(teamRewardPrefix)) {
						// team specific reward
						ConfigurationSection rewardsSection = warzoneRootSection.getConfigurationSection(teamRewardPrefix);
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
				lobbyFace = BlockFace.SOUTH;
			} else if (lobbyOrientation.equals("east")) {
				lobbyFace = BlockFace.EAST;
			} else if (lobbyOrientation.equals("north")) {
				lobbyFace = BlockFace.NORTH;
			} else if (lobbyOrientation.equals("west")) {
				lobbyFace = BlockFace.WEST;
			}
			
			// lobby world
			String lobbyWorldName = warzoneRootSection.getString(lobbyPrefix + "world");
			World lobbyWorld = War.war.getServer().getWorld(lobbyWorldName);
						
			// create the lobby
			Volume lobbyVolume = VolumeMapper.loadVolume("lobby", warzone.getName(), lobbyWorld);
			ZoneLobby lobby = new ZoneLobby(warzone, lobbyFace, lobbyVolume);
			warzone.setLobby(lobby);

			return warzone;
		}
		
		return null;
	}
	
	public static void save(Warzone warzone, boolean saveAllBlocks) {
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
			if (BlockFace.SOUTH == warzone.getLobby().getWall()) {
				lobbyOrientation = "south";
			} else if (BlockFace.EAST == warzone.getLobby().getWall()) {
				lobbyOrientation = "east";
			} else if (BlockFace.NORTH == warzone.getLobby().getWall()) {
				lobbyOrientation = "north";
			} else if (BlockFace.WEST == warzone.getLobby().getWall()) {
				lobbyOrientation = "west";
			}
			
			ConfigurationSection lobbySection = warzoneInfoSection.createSection("lobby");
			lobbySection.set("orientation", lobbyOrientation);
			lobbySection.set("world", warzone.getLobby().getVolume().getWorld().getName());
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

	public static void delete(String name) {
		File zoneFolder = new File(War.war.getDataFolder().getPath() + "/dat/warzone-" + name);
		File[] files = zoneFolder.listFiles();
		for (File file : files) {
			boolean deletedData = file.delete();
			if (!deletedData) {
				War.war.log("Failed to delete file " + file.getName(), Level.WARNING);
			}
		}
		boolean deletedData = zoneFolder.delete();
		if (!deletedData) {
			War.war.log("Failed to delete folder " + zoneFolder.getName(), Level.WARNING);
		}
		File zoneFile = new File(War.war.getDataFolder().getPath() + "/warzone-" + name + ".txt");
		if (zoneFile.exists()) {
			deletedData = zoneFile.delete();
			if (!deletedData) {
				War.war.log("Failed to delete file " + zoneFile.getName(), Level.WARNING);
			}
		}
		zoneFile = new File(War.war.getDataFolder().getPath() + "/warzone-" + name + ".yml");
		deletedData = zoneFile.delete();
		if (!deletedData) {
			War.war.log("Failed to delete file " + zoneFile.getName(), Level.WARNING);
		}
	}
}
