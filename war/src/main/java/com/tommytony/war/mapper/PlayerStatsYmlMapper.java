package com.tommytony.war.mapper;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.tommytony.war.War;

public class PlayerStatsYmlMapper {

	private static YamlConfiguration pStatsYml;
	
	public final static int KILL = 4;
	public final static int LOSS = 5;
	public final static int WIN = 6;
	public final static int DEATH = 7;
	
	public static File load() {
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
	
	public static void init() {
		pStatsYml = YamlConfiguration.loadConfiguration(load());
		pStatsYml.createSection("set");
	}
	
	public static int load(Player p, int stat) {
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
	
	public static void save(Player p, int stat, int updateAmt) {
		ConfigurationSection playerConfigSection = getConfigSection(p);
		switch(stat) {
		    case KILL:
		        playerConfigSection.set("kill", playerConfigSection.getInt("kill") + updateAmt);
		        return;
		    case DEATH:
			    playerConfigSection.set("death", playerConfigSection.getInt("death") + updateAmt);
			    return;
		    case WIN:
			    playerConfigSection.set("win", playerConfigSection.getInt("win") + updateAmt);
			    return;
		    case LOSS:
			    playerConfigSection.set("loss", playerConfigSection.getInt("loss") + updateAmt);
			    return;
			default:
				return;
		}
	}
	
	public static ConfigurationSection getConfigSection(Player p) {
		ConfigurationSection rootConfigSection = getConfig().getConfigurationSection("set");
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
	
	public static YamlConfiguration getConfig() {
		return pStatsYml;
	}
	
	public static void saveToDisk() {
		try {
		    pStatsYml.save(new File(War.war.getDataFolder().getPath() + "/stat/pstat.yml"));
		} catch(IOException e) {
			War.war.log("Failed to save Stats file to disk", Level.SEVERE);
		}
	}
}