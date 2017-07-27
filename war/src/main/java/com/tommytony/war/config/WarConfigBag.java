package com.tommytony.war.config;

import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Level;

import com.tommytony.war.War;
import com.tommytony.war.mapper.WarYmlMapper;
import org.bukkit.command.CommandSender;
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

	public String getString(WarConfig config) {
		if (this.bag.containsKey(config)) {
			return (String)this.bag.get(config);
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
				} else if (config.getConfigType().equals(String.class)) {
					this.put(config, warConfigSection.getString(config.toString()));
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
				} else if (warConfig.getConfigType().equals(String.class)) {
					String str = namedParams.get(namedParam);
					this.bag.put(warConfig, str);
				}
				if (warConfig == WarConfig.LANGUAGE) {
					War.reloadLanguage();
				}
				returnMessage += warConfig.toString() + " set to " + namedParams.get(namedParam); 
			}
		}
		return returnMessage;
	}

	public static void afterUpdate(CommandSender sender, String namedParamReturn, boolean wantsToPrint) {
		WarYmlMapper.save();
		if (wantsToPrint) {
			String config = War.war.printConfig();
			War.war.msg(sender, "War config saved. " + namedParamReturn + " " + config);
		} else {
			War.war.msg(sender, "War config saved. " + namedParamReturn);
		}
		War.war.log(sender.getName() + " updated War configuration. " + namedParamReturn, Level.INFO);
	}
}
