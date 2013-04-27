package com.tommytony.war.mapper;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

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
	
	public int load(Player p, int stat) {
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
		    	return 0;
		}
	}
	
	public int[] load(Player p) {
		ConfigurationSection playerConfigSection = getConfigSection(p);
		int[] ret = new int[4];
		ret[0] = playerConfigSection.getInt("kill");
		ret[1] = playerConfigSection.getInt("death");
		ret[2] = playerConfigSection.getInt("win");
		ret[3] = playerConfigSection.getInt("loss");
		return ret;
	}
	
	public void save(Player p, int stat, int amt) {
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
	
	public void save(Player p, int[] stats) {
		ConfigurationSection playerConfigSection = getConfigSection(p);
		playerConfigSection.set("kill", stats[0]);
		playerConfigSection.set("death", stats[1]);
		playerConfigSection.set("win", stats[2]);
		playerConfigSection.set("loss", stats[3]);
	}
	
	public ConfigurationSection getConfigSection(Player p) {
		ConfigurationSection rootConfigSection = this.getConfig().getConfigurationSection("set");
		ConfigurationSection playerConfigSection;
		if(rootConfigSection.get("war.stats." + p.getName()) == null){
			War.war.log("No section for player " + p.getName() + " in stats file, creating one.", Level.INFO);
			playerConfigSection = rootConfigSection.createSection("war.stats." + p.getName());
			playerConfigSection.set("kill", 0);
			playerConfigSection.set("death", 0);
			playerConfigSection.set("win", 0);
			playerConfigSection.set("loss", 0);
		} else {
			playerConfigSection = rootConfigSection.getConfigurationSection("war.stats." + p.getName());
		}
		return playerConfigSection;
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
}