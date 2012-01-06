package com.tommytony.war.config;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;

import bukkit.tommytony.war.War;

public class WarzoneConfigBag {

	HashMap<WarzoneConfig, Object> bag = new HashMap<WarzoneConfig, Object>();
		
	public void put(WarzoneConfig config, Object value) {
		bag.put(config, value);
	}
	
	public boolean isEmpty() {
		return bag.keySet().size() == 0;
	}
	
	public Object getValue(WarzoneConfig config) {
		if (bag.containsKey(config)) {
			return bag.get(config); 
		} else {
			// use War default config
			return War.war.getWarzoneDefaultConfig().getValue(config);
		}
	}
	
	public Integer getInt(WarzoneConfig config) {
		if (bag.containsKey(config)) {
			return (Integer)bag.get(config); 
		} else {
			// use War default config
			return War.war.getWarzoneDefaultConfig().getInt(config);
		}
	}
	
	public Boolean getBoolean(WarzoneConfig config) {
		if (bag.containsKey(config)) {
			return (Boolean)bag.get(config); 
		} else {
			// use War default config
			return War.war.getWarzoneDefaultConfig().getBoolean(config);
		}
	}

	public void loadFrom(ConfigurationSection warzoneConfigSection) {
		for (WarzoneConfig config : WarzoneConfig.values()) {
			if (warzoneConfigSection.contains(config.toString())) {
				if (config.getConfigType().equals(Integer.class)) {
					this.put(config, warzoneConfigSection.getInt(config.toString()));
				} else if (config.getConfigType().equals(Boolean.class)) {
					this.put(config, warzoneConfigSection.getBoolean(config.toString()));
				}
			}
		}
	}

	public void saveTo(ConfigurationSection warzoneConfigSection) {
		for (WarzoneConfig config : WarzoneConfig.values()) {
			if (this.bag.containsKey(config)) {
				warzoneConfigSection.set(config.toString(), this.bag.get(config));
			}
		}
	}
	
	public String updateFromNamedParams(Map<String, String> namedParams) {
		String returnMessage = "";
		for (String namedParam : namedParams.keySet()) {
			WarzoneConfig warzoneConfig = WarzoneConfig.warzoneConfigFromString(namedParam);
			
			// param update
			if (warzoneConfig != null) {
				if (warzoneConfig.getConfigType().equals(Integer.class)) {
					int intValue = Integer.parseInt(namedParams.get(namedParam));
					this.bag.put(warzoneConfig, intValue);
				} else if (warzoneConfig.getConfigType().equals(Boolean.class)) {
					String onOff = namedParams.get(namedParam);
					this.bag.put(warzoneConfig, onOff.equals("on") || onOff.equals("true"));
				}
				returnMessage += warzoneConfig.toString() + " set to " + namedParams.get(namedParam); 
			} else if (namedParam.equals("delete")) {
				String toDelete = namedParams.get(namedParam);
				warzoneConfig = WarzoneConfig.warzoneConfigFromString(toDelete);
				
				// param delete (to restore inheritance)
				if (warzoneConfig != null) {
					this.bag.remove(warzoneConfig);
					returnMessage += warzoneConfig.toString() + " removed";
				}
			}
		}
		return returnMessage;
	}
}
