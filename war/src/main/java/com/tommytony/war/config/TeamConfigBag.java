package com.tommytony.war.config;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;

import bukkit.tommytony.war.War;

import com.tommytony.war.FlagReturn;
import com.tommytony.war.TeamSpawnStyle;
import com.tommytony.war.Warzone;

public class TeamConfigBag {

	private HashMap<TeamConfig, Object> bag = new HashMap<TeamConfig, Object>();
	private Warzone warzone;
		
	public TeamConfigBag(Warzone warzone) {
		this.warzone = warzone;
	}
	
	public TeamConfigBag() {
		this.warzone = null;
	}
	
	public boolean contains(TeamConfig config) {
		return this.bag.containsKey(config);
	}
	
	public boolean isEmpty() {
		return this.bag.keySet().size() == 0;
	}
	
	public void put(TeamConfig config, Object value) {
		this.bag.put(config, value);
	}

	public Object getValue(TeamConfig config) {
		if (this.contains(config)) {
			return this.bag.get(config);
		} else {
			return null;
		}
	}
	
	public Object resolveValue(TeamConfig config) {
		if (this.contains(config)) {
			return this.bag.get(config); 
		} else if (this.warzone != null && this.warzone.getTeamDefaultConfig().contains(config)){
			// use Warzone default config
			return this.warzone.getTeamDefaultConfig().resolveValue(config);
		} else {
			// use War default config
			return War.war.getTeamDefaultConfig().resolveValue(config);
		}
	}
	
	public Integer getInt(TeamConfig config) {
		if (this.contains(config)) {
			return (Integer)this.bag.get(config);
		}
		return null;
	}
	
	public Integer resolveInt(TeamConfig config) {
		if (this.contains(config)) {
			return (Integer)this.bag.get(config); 
		} else if (this.warzone != null && this.warzone.getTeamDefaultConfig().contains(config)){
			// use Warzone default config
			return this.warzone.getTeamDefaultConfig().resolveInt(config);
		} else {
			// use War default config
			return War.war.getTeamDefaultConfig().resolveInt(config);
		}
	}
	
	public Boolean getBoolean(TeamConfig config) {
		if (this.contains(config)) {
			return (Boolean)this.bag.get(config);
		}
		return null;
	}
	
	public Boolean resolveBoolean(TeamConfig config) {
		if (this.contains(config)) {
			return (Boolean)this.bag.get(config); 
		} else if (this.warzone != null && this.warzone.getTeamDefaultConfig().contains(config)){
			// use Warzone default config
			return this.warzone.getTeamDefaultConfig().resolveBoolean(config);
		} else {
			// use War default config
			return War.war.getTeamDefaultConfig().resolveBoolean(config);
		}
	}
	
	public FlagReturn resolveFlagReturn() {
		if (this.contains(TeamConfig.FLAGRETURN)) {
			return (FlagReturn)this.bag.get(TeamConfig.FLAGRETURN); 
		} else if (this.warzone != null && this.warzone.getTeamDefaultConfig().contains(TeamConfig.FLAGRETURN)){
			// use Warzone default config
			return this.warzone.getTeamDefaultConfig().resolveFlagReturn();
		} else {
			// use War default config
			return War.war.getTeamDefaultConfig().resolveFlagReturn();
		}
	}
	
	public FlagReturn getFlagReturn() {
		if (this.contains(TeamConfig.FLAGRETURN)) {
			return (FlagReturn)this.bag.get(TeamConfig.FLAGRETURN); 
		}
		return null;
	}
	
	public TeamSpawnStyle resolveSpawnStyle() {
		if (this.contains(TeamConfig.SPAWNSTYLE)) {
			return (TeamSpawnStyle)this.bag.get(TeamConfig.SPAWNSTYLE); 
		} else if (this.warzone != null && this.warzone.getTeamDefaultConfig().contains(TeamConfig.SPAWNSTYLE)){
			// use War default config
			return this.warzone.getTeamDefaultConfig().resolveSpawnStyle();
		} else {
			return War.war.getTeamDefaultConfig().resolveSpawnStyle();
		}
	}
	
	public TeamSpawnStyle getSpawnStyle() {
		if (this.contains(TeamConfig.SPAWNSTYLE)) {
			return (TeamSpawnStyle)this.bag.get(TeamConfig.SPAWNSTYLE); 
		}
		return null;
	}
	
	public void loadFrom(ConfigurationSection teamConfigSection) {
		for (TeamConfig config : TeamConfig.values()) {
			if (teamConfigSection.contains(config.toString())) {
				if (config.getConfigType().equals(Integer.class)) {
					this.put(config, teamConfigSection.getInt(config.toString()));
				} else if (config.getConfigType().equals(Boolean.class)) {
					this.put(config, teamConfigSection.getBoolean(config.toString()));
				} else if (config.getConfigType().equals(FlagReturn.class)) {
					String flagReturnStr = teamConfigSection.getString(config.toString());
					FlagReturn returnMode = FlagReturn.getFromString(flagReturnStr);
					if (returnMode != null) {
						this.put(config, returnMode);
					}
				} else if (config.getConfigType().equals(TeamSpawnStyle.class)) {
					String spawnStyleStr = teamConfigSection.getString(config.toString());
					TeamSpawnStyle style = TeamSpawnStyle.getStyleFromString(spawnStyleStr);
					if (style != null) {
						this.put(config, style);
					}
				} 
			}
		}
	}

	public void saveTo(ConfigurationSection teamConfigSection) {
		for (TeamConfig config : TeamConfig.values()) {
			if (this.contains(config)) {
				if (config.getConfigType().equals(Integer.class) 
						|| config.getConfigType().equals(Boolean.class)) {
					teamConfigSection.set(config.toString(), this.bag.get(config));
				} else {
					teamConfigSection.set(config.toString(), this.bag.get(config).toString());
				}
			}
		}
	}
	
	public String updateFromNamedParams(Map<String, String> namedParams) {
		String returnMessage = "";
		for (String namedParam : namedParams.keySet()) {
			TeamConfig teamConfig = TeamConfig.teamConfigFromString(namedParam);
			if (teamConfig != null) {
				if (teamConfig.getConfigType().equals(Integer.class)) {
					int intValue = Integer.parseInt(namedParams.get(namedParam));
					this.bag.put(teamConfig, intValue);
				} else if (teamConfig.getConfigType().equals(Boolean.class)) {
					String onOff = namedParams.get(namedParam);
					this.bag.put(teamConfig, onOff.equals("on") || onOff.equals("true"));
				} else if (teamConfig.getConfigType().equals(FlagReturn.class)) {
					FlagReturn flagValue = FlagReturn.getFromString(namedParams.get(namedParam));
					this.bag.put(teamConfig, flagValue);
				} else if (teamConfig.getConfigType().equals(TeamSpawnStyle.class)) {
					TeamSpawnStyle spawnValue = TeamSpawnStyle.getStyleFromString(namedParams.get(namedParam));
					this.bag.put(teamConfig, spawnValue);
				}
				returnMessage += " " + teamConfig.toString() + " set to " + namedParams.get(namedParam); 
			} else if (namedParam.equals("delete")) {
				String toDelete = namedParams.get(namedParam);
				teamConfig = TeamConfig.teamConfigFromString(toDelete);
				
				// param delete (to restore inheritance)
				if (teamConfig != null) {
					this.bag.remove(teamConfig);
					returnMessage += " " + teamConfig.toString() + " removed";
				}
			}
		}
		return returnMessage;
	}
}
