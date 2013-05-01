package com.tommytony.war.mapper;

import java.io.File;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import com.tommytony.war.War;

/** 
 * @author grinning
 * @package com.tommytony.war.mapper
 * @description Implementation of a Player Stats tracker for the YML format
 */

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
	}
	
	public int load(String p, int stat) {
		String path = "war.stats." + p + ".";
		if(!this.getConfig().isInt(path + "kill")) {
			this.save(p, null, true);
			return -1;
		}
		switch(stat) {
		    case KILL:
		    	return this.getConfig().getInt(path + "kill");
		    case DEATH:
		    	return this.getConfig().getInt(path + "death");
		    case WIN:
		    	return this.getConfig().getInt(path + "win");
		    case LOSS:
		    	return this.getConfig().getInt(path + "loss");
		    default:
		    	return -1;
		}
	}
	
	public int[] load(String p) {
		String path = "war.stats." + p + ".";
		int[] ret = new int[4];
		ret[0] = this.getConfig().getInt(path + "kill");
		ret[1] = this.getConfig().getInt(path + "death");
		ret[2] = this.getConfig().getInt(path + "win");
		ret[3] = this.getConfig().getInt(path + "loss");
		return ret;
	}
	
	public void save(String p, int stat, int amt) {
		String path = "war.stats." + p + ".";
		switch(stat) {
		    case KILL:
		        this.getConfig().set(path + "kill", amt);
		        return;
		    case DEATH:
			    this.getConfig().set(path + "death", amt);
			    return;
		    case WIN:
			    this.getConfig().set(path + "win", amt);
			    return;
		    case LOSS:
			    this.getConfig().set(path + "loss", amt);
			    return;
			default:
				return;
		}
	}
	
	public void save(String p, int[] stats) {
		this.save(p, stats, false);
	}
	
	public ConfigurationSection getConfigSection(String p) {
		return this.getConfig().getConfigurationSection("war.stats." + p);
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
		String path = "war.stats." + player + ".";
		if(makeNewPlayer) {
			this.getConfig().set(path + "kill", 0);
			this.getConfig().set(path + "death", 0);
			this.getConfig().set(path + "win", 0);
			this.getConfig().set(path + "loss", 0);
		} else {
			this.getConfig().set(path + "kill", stats[0]);
			this.getConfig().set(path + "death", stats[1]);
			this.getConfig().set(path + "win", stats[2]);
			this.getConfig().set(path + "loss", stats[3]);
		}
	}
}