package com.tommytony.war.mapper;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import com.tommytony.war.War;

public class PlayerStatsYmlMapper implements PlayerStatsMapper {

	private static YamlConfiguration pStatsYml;
	
	public File load() {
		(War.war.getDataFolder()).mkdir();
		(new File(War.war.getDataFolder(), "stat")).mkdir();
		File pStatFile = new File(War.war.getDataFolder().getPath() + "/stat/pstat.yml");
		//check to see if we exist
		if(!pStatFile.exists()) {
			try {
				pStatFile.createNewFile();
				War.war.log("Created pstat.yml file", Level.INFO);
			} catch (IOException e) {
				War.war.log("Error creating pstat.yml file", Level.WARNING);
			}
		}
		return pStatFile;
	}
	
	public void init() {
		pStatsYml = YamlConfiguration.loadConfiguration(this.load());
		pStatsYml.createSection("set");
	}
	
	public int load(String p, int stat) {
		ConfigurationSection playerConfigSection = getConfigSection(p);
		switch(stat) {
		    case KILL:
		    	return playerConfigSection.getInt("kill");
		    case DEATH:
		    	return playerConfigSection.getInt("death");
		    case WIN:
		    	return playerConfigSection.getInt("win");
		    case LOSS:
		    	return playerConfigSection.getInt("loss");
		    default:
		    	return -1;
		}
	}
	
	public int[] load(String p) {
		ConfigurationSection playerConfigSection = getConfigSection(p);
		if(playerConfigSection == null) {
			return new int[] {0, 0, 0, 0};
		}
		int[] ret = new int[4];
		ret[0] = playerConfigSection.getInt("kill");
		ret[1] = playerConfigSection.getInt("death");
		ret[2] = playerConfigSection.getInt("win");
		ret[3] = playerConfigSection.getInt("loss");
		return ret;
	}
	
	public void save(String p, int stat, int amt) {
		ConfigurationSection playerConfigSection = getConfigSection(p);
		switch(stat) {
		    case KILL:
		        playerConfigSection.set("kill", amt);
		        return;
		    case DEATH:
			    playerConfigSection.set("death", amt);
			    return;
		    case WIN:
			    playerConfigSection.set("win", amt);
			    return;
		    case LOSS:
			    playerConfigSection.set("loss", amt);
			    return;
			default:
				return;
		}
	}
	
	public void save(String p, int[] stats) {
		this.save(p, stats, false);
	}
	
	public ConfigurationSection getConfigSection(String p) {
		ConfigurationSection rootConfigSection = this.getConfig().getConfigurationSection("set");
		return rootConfigSection.getConfigurationSection("war.stats." + p);
	}
	
	public YamlConfiguration getConfig() {
		return pStatsYml;
	}
	
	public void saveToDisk() {
		try {
		    pStatsYml.save(new File(War.war.getDataFolder().getPath() + "/stat/pstat.yml"));
		} catch(IOException e) {
			War.war.log("Failed to save Stats file to disk", Level.SEVERE);
		}
	}

	public void close() {
		this.saveToDisk();
	}

	public void save(String player, int[] stats, boolean makeNewPlayer) {
		ConfigurationSection rootConfigSection = this.getConfig().getConfigurationSection("set");
		ConfigurationSection playerConfigSection;
		if(makeNewPlayer) {
			playerConfigSection = rootConfigSection.createSection("war.stats." + player);
			playerConfigSection.set("kill", 0);
			playerConfigSection.set("death", 0);
			playerConfigSection.set("win", 0);
			playerConfigSection.set("loss", 0);
		} else {
            playerConfigSection = rootConfigSection.createSection("war.stats." + player);
			playerConfigSection.set("kill", stats[0]);
			playerConfigSection.set("death", stats[1]);
			playerConfigSection.set("win", stats[2]);
			playerConfigSection.set("loss", stats[3]);
		}
	}
}