package com.tommytony.war.config;

import java.util.EnumMap;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;

public class WarConfigBag {

	EnumMap<WarConfig, Object> bag = new EnumMap<WarConfig, Object>(WarConfig.class);
		
	public void put(WarConfig config, Object value) {
		this.bag.put(config, value);
	}
	
	public Object getValue(WarConfig config) {
		if (this.bag.containsKey(config)) {
			return this.bag.get(config); 
		} else {
			return null;
		}
	}
	
	public Integer getInt(WarConfig config) {
		if (this.bag.containsKey(config)) {
			return (Integer)this.bag.get(config); 
		} else {
			return null;
		}
	}
	
	public Boolean getBoolean(WarConfig config) {
		if (this.bag.containsKey(config)) {
			return (Boolean)this.bag.get(config); 
		} else {
			return null;
		}
	}

	public void loadFrom(ConfigurationSection warConfigSection) {
		for (WarConfig config : WarConfig.values()) {
			if (warConfigSection.contains(config.toString())) {
				if (config.getConfigType().equals(Integer.class)) {
					this.put(config, warConfigSection.getInt(config.toString()));
				} else if (config.getConfigType().equals(Boolean.class)) {
					this.put(config, warConfigSection.getBoolean(config.toString()));
				}
			}
		}
	}

	public void saveTo(ConfigurationSection warConfigSection) {
		for (WarConfig config : WarConfig.values()) {
			if (this.bag.containsKey(config)) {
				warConfigSection.set(config.toString(), this.bag.get(config));
			}
		}
	}
	
	public String updateFromNamedParams(Map<String, String> namedParams) {
		String returnMessage = "";
		for (String namedParam : namedParams.keySet()) {
			WarConfig warConfig = WarConfig.warConfigFromString(namedParam);
			if (warConfig  != null) {
				if (warConfig.getConfigType().equals(Integer.class)) {
					int intValue = Integer.parseInt(namedParams.get(namedParam));
					this.bag.put(warConfig, intValue);
				} else if (warConfig.getConfigType().equals(Boolean.class)) {
					String onOff = namedParams.get(namedParam);
					this.bag.put(warConfig, onOff.equals("on") || onOff.equals("true"));
				}
				returnMessage += warConfig.toString() + " set to " + namedParams.get(namedParam); 
			}
		}
		return returnMessage;
	}
}
