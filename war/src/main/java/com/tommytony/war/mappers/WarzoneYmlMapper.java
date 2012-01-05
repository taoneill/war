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
			String zoneInfoPrefix = "warzone." + name + ".info.";
			
			// world
			String worldStr = warzoneYmlConfig.getString(zoneInfoPrefix + "world");
			World world = War.war.getServer().getWorld(worldStr);
			
			// Create the zone
			Warzone warzone = new Warzone(world, name);

			// teleport
			int teleX = warzoneYmlConfig.getInt(zoneInfoPrefix + "teleport.x");
			int teleY = warzoneYmlConfig.getInt(zoneInfoPrefix + "teleport.y");
			int teleZ = warzoneYmlConfig.getInt(zoneInfoPrefix + "teleport.z");
			int teleYaw = warzoneYmlConfig.getInt(zoneInfoPrefix + "teleport.yaw");
			warzone.setTeleport(new Location(world, teleX, teleY, teleZ, teleYaw, 0));
			
			if (warzoneYmlConfig.contains("team.default")) {
				// defaultLoadouts
				ConfigurationSection loadoutsSection = warzoneYmlConfig.getConfigurationSection("team.default.loadout");
				LoadoutYmlMapper.fromConfigToLoadouts(loadoutsSection, warzone.getDefaultInventories().getLoadouts());

				// defaultReward
				ConfigurationSection rewardsSection = warzoneYmlConfig.getConfigurationSection("team.default.reward");
				HashMap<Integer, ItemStack> reward = new HashMap<Integer, ItemStack>();
				LoadoutYmlMapper.fromConfigToLoadout(rewardsSection, reward, "default");
				warzone.getDefaultInventories().setReward(reward);
				
				// Team default settings
				ConfigurationSection teamConfigSection = warzoneYmlConfig.getConfigurationSection("team.default.config");
				warzone.getTeamDefaultConfig().loadFrom(teamConfigSection);
			}
			
			// Warzone settings
			if (warzoneYmlConfig.contains("warzone.config")) {
				ConfigurationSection warzoneConfigSection = warzoneYmlConfig.getConfigurationSection("warzone.config");
				warzone.getWarzoneConfig().loadFrom(warzoneConfigSection);
			}

			// authors
			if (warzoneYmlConfig.contains(zoneInfoPrefix + "authors")) {
				for(String authorStr : warzoneYmlConfig.getStringList("authors")) {
					if (!authorStr.equals("")) {
						warzone.addAuthor(authorStr);
					}
				}
			}

			// rallyPoint
			if (warzoneYmlConfig.contains(zoneInfoPrefix + "rallypoint")) {
				int rpX = warzoneYmlConfig.getInt(zoneInfoPrefix + "rallypoint.x");
				int rpY = warzoneYmlConfig.getInt(zoneInfoPrefix + "rallypoint.y");
				int rpZ = warzoneYmlConfig.getInt(zoneInfoPrefix + "rallypoint.z");
				int rpYaw = warzoneYmlConfig.getInt(zoneInfoPrefix + "rallypoint.yaw");
				Location rallyPoint = new Location(world, rpX, rpY, rpZ, rpYaw, 0);
				warzone.setRallyPoint(rallyPoint);
			}

			// monuments
			if (warzoneYmlConfig.contains(zoneInfoPrefix + "monument")) {
				List<String> monunmentNames = warzoneYmlConfig.getStringList(zoneInfoPrefix + "monument.names");
				for (String monumentName : monunmentNames) {
					if (monumentName != null && !monumentName.equals("")) {
						String monumentPrefix = zoneInfoPrefix + "monument." + monumentName + ".";
						int monumentX = warzoneYmlConfig.getInt(monumentPrefix + "x");
						int monumentY = warzoneYmlConfig.getInt(monumentPrefix + "y");
						int monumentZ = warzoneYmlConfig.getInt(monumentPrefix + "z");
						int monumentYaw = warzoneYmlConfig.getInt(monumentPrefix + "yaw");
						Monument monument = new Monument(monumentName, warzone, new Location(world, monumentX, monumentY, monumentZ, monumentYaw, 0));
						warzone.getMonuments().add(monument);
					}
				}
			}
			
			// teams
			List<String> teamsNames = warzoneYmlConfig.getStringList("team.names");
			for (String teamName : teamsNames) {
				// team info
				String teamInfoPrefix = "team." + teamName + ".info";
				int teamX = warzoneYmlConfig.getInt(teamInfoPrefix + "spawn.x");
				int teamY = warzoneYmlConfig.getInt(teamInfoPrefix + "spawn.y");
				int teamZ = warzoneYmlConfig.getInt(teamInfoPrefix + "spawn.z");
				int teamYaw = warzoneYmlConfig.getInt(teamInfoPrefix + "spawn.yaw");
				Location teamLocation = new Location(world, teamX, teamY, teamZ, teamYaw, 0);

				Team team = new Team(teamName, TeamKind.teamKindFromString(teamName), teamLocation, warzone);
				warzone.getTeams().add(team);
				
				if (warzoneYmlConfig.contains(teamInfoPrefix + "flag")) {
					int flagX = warzoneYmlConfig.getInt(teamInfoPrefix + "flag.x");
					int flagY = warzoneYmlConfig.getInt(teamInfoPrefix + "flag.y");
					int flagZ = warzoneYmlConfig.getInt(teamInfoPrefix + "flag.z");
					int flagYaw = warzoneYmlConfig.getInt(teamInfoPrefix + "flag.yaw");
					Location flagLocation = new Location(world, flagX, flagY, flagZ, flagYaw, 0);
					team.setTeamFlag(flagLocation);
				}
				
				String teamConfigPrefix = "team." + teamName + ".config";
				if (warzoneYmlConfig.contains(teamConfigPrefix)) {
					// team specific config
					ConfigurationSection teamConfigSection = warzoneYmlConfig.getConfigurationSection(teamConfigPrefix);
					team.getTeamConfig().loadFrom(teamConfigSection);
				}
				
				team.setRemainingLives(team.getTeamConfig().getInt(TeamConfig.LIFEPOOL));
				
				String teamLoadoutPrefix = "team." + teamName + ".loadout";
				if (warzoneYmlConfig.contains(teamLoadoutPrefix)) {
					// team specific loadouts
					ConfigurationSection loadoutsSection = warzoneYmlConfig.getConfigurationSection(teamLoadoutPrefix);
					LoadoutYmlMapper.fromConfigToLoadouts(loadoutsSection, team.getInventories().getLoadouts());
				}
				
				String teamRewardPrefix = "team." + teamName + ".reward";
				if (warzoneYmlConfig.contains(teamRewardPrefix)) {
					// team specific reward
					ConfigurationSection rewardsSection = warzoneYmlConfig.getConfigurationSection(teamRewardPrefix);
					HashMap<Integer, ItemStack> reward = new HashMap<Integer, ItemStack>();
					LoadoutYmlMapper.fromConfigToLoadout(rewardsSection, reward, "default");
					warzone.getDefaultInventories().setReward(reward);
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
			String lobbyOrientation = warzoneYmlConfig.getString(lobbyPrefix + "orientation");
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
			String lobbyWorldName = warzoneYmlConfig.getString(lobbyPrefix + "world");
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
		(new File(War.war.getDataFolder().getPath() + "/dat/warzone-" + warzone.getName())).mkdir();	// create folder
		
		String zoneInfoPrefix = "warzone." + warzone.getName() + ".info.";
		
		// world
		warzoneYmlConfig.set(zoneInfoPrefix + "world", warzone.getWorld().getName());

		// teleport
		ConfigurationSection teleSection = warzoneYmlConfig.createSection(zoneInfoPrefix + "teleport");
		teleSection.set(zoneInfoPrefix + "teleport.x", warzone.getTeleport().getBlockX());
		teleSection.set(zoneInfoPrefix + "teleport.y", warzone.getTeleport().getBlockY());
		teleSection.set(zoneInfoPrefix + "teleport.z", warzone.getTeleport().getBlockZ());
		teleSection.set(zoneInfoPrefix + "teleport.yaw", toIntYaw(warzone.getTeleport().getYaw()));

		// teams
		List<Team> teams = warzone.getTeams();
		List<String> teamNames = new ArrayList<String>();
		for (Team team : teams) {
			String teamPrefix = "team." + team.getName() + ".";
			teamNames.add(team.getName());
			
			Location spawn = team.getTeamSpawn();
			warzoneYmlConfig.set(teamPrefix + "spawn.x", spawn.getBlockX());
			warzoneYmlConfig.set(teamPrefix + "spawn.y", spawn.getBlockY());
			warzoneYmlConfig.set(teamPrefix + "spawn.z", spawn.getBlockZ());
			warzoneYmlConfig.set(teamPrefix + "spawn.yaw", toIntYaw(spawn.getYaw()));
			
			if (team.getTeamFlag() != null) {
				Location teamFlag = team.getTeamFlag();
				warzoneYmlConfig.set(teamPrefix + "flag.x", teamFlag.getBlockX());
				warzoneYmlConfig.set(teamPrefix + "flag.y", teamFlag.getBlockY());
				warzoneYmlConfig.set(teamPrefix + "flag.z", teamFlag.getBlockZ());
				warzoneYmlConfig.set(teamPrefix + "flag.yaw", toIntYaw(teamFlag.getYaw()));
			}
			
			if (!team.getTeamConfig().isEmpty()) {
				// team specific config
				ConfigurationSection teamConfigSection = warzoneYmlConfig.createSection("team." + team.getName() + ".config");
				team.getTeamConfig().saveTo(teamConfigSection);
			}
			
			if (team.getInventories().hasLoadouts()) {
				// team specific loadouts
				ConfigurationSection loadoutsSection = warzoneYmlConfig.createSection("team." + team.getName() + ".loadout");
				LoadoutYmlMapper.fromLoadoutsToConfig(team.getInventories().getLoadouts(), loadoutsSection);
			}
			
			if (team.getInventories().hasReward()) {
				// team specific reward
				ConfigurationSection rewardsSection = warzoneYmlConfig.createSection("team." + team.getName() + ".reward");
				LoadoutYmlMapper.fromLoadoutToConfig(team.getInventories().getReward(), rewardsSection, "default");
			}
		}
		
		if (teamNames.size() > 0) {
			warzoneYmlConfig.set("team.names", teamNames);
		}
		
		// defaultLoadouts
		if (warzone.getDefaultInventories().hasLoadouts()) {
			ConfigurationSection loadoutsSection = warzoneYmlConfig.createSection("team.default.loadout");
			LoadoutYmlMapper.fromLoadoutsToConfig(warzone.getDefaultInventories().getLoadouts(), loadoutsSection);
		}
		
		// defaultReward
		if (warzone.getDefaultInventories().hasReward()) {
			ConfigurationSection rewardsSection = warzoneYmlConfig.createSection("team.default.reward");
			LoadoutYmlMapper.fromLoadoutToConfig(warzone.getDefaultInventories().getReward(), rewardsSection, "default");
		}
		
		// Warzone settings
		if (!warzone.getWarzoneConfig().isEmpty()) {
			ConfigurationSection warzoneConfigSection = warzoneYmlConfig.createSection("warzone." + warzone.getName() + ".config");
			warzone.getWarzoneConfig().saveTo(warzoneConfigSection);
		}
		
		// Team default settings
		if (!warzone.getTeamDefaultConfig().isEmpty()) {
			ConfigurationSection teamConfigSection = warzoneYmlConfig.createSection("team.default.config");
			warzone.getTeamDefaultConfig().saveTo(teamConfigSection);
		}
		
		// authors
		warzoneYmlConfig.set(zoneInfoPrefix + "authors", warzone.getAuthors());
		
		// rallyPoint
		if (warzone.getRallyPoint() != null) {
			ConfigurationSection rpSection = warzoneYmlConfig.createSection(zoneInfoPrefix + "rallypoint");
			rpSection.set(zoneInfoPrefix + "x", warzone.getTeleport().getBlockX());
			rpSection.set(zoneInfoPrefix + "y", warzone.getTeleport().getBlockY());
			rpSection.set(zoneInfoPrefix + "z", warzone.getTeleport().getBlockZ());
			rpSection.set(zoneInfoPrefix + "yaw", toIntYaw(warzone.getTeleport().getYaw()));
		}

		// monuments
		if (warzone.getMonuments().size() > 0) {
			ConfigurationSection monumentSection = warzoneYmlConfig.createSection(zoneInfoPrefix + "monument");
			List<String> monumentNames = new ArrayList<String>();
			for (Monument monument : warzone.getMonuments()) {
				monumentNames.add(monument.getName());
				monumentSection.set(monument.getName() + ".x", monument.getLocation().getBlockX());
				monumentSection.set(monument.getName() + ".y", monument.getLocation().getBlockX());
				monumentSection.set(monument.getName() + ".z", monument.getLocation().getBlockX());
				monumentSection.set(monument.getName() + ".yaw", toIntYaw(monument.getLocation().getYaw()));
			}
			monumentSection.set("names", monumentNames);
		}

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
			
			warzoneYmlConfig.set(zoneInfoPrefix + "lobby.orientation", lobbyOrientation);
			warzoneYmlConfig.set(zoneInfoPrefix + "lobby.world", warzone.getLobby().getVolume().getWorld().getName());
		}

		// monument blocks
		for (Monument monument : warzone.getMonuments()) {
			VolumeMapper.save(monument.getVolume(), warzone.getName());
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
