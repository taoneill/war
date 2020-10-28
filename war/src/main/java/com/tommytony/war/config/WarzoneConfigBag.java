package com.tommytony.war.config;

import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import com.tommytony.war.mapper.WarzoneYmlMapper;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;

import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Level;


public class WarzoneConfigBag {

	private final Warzone warzone;
	EnumMap<WarzoneConfig, Object> bag = new EnumMap<WarzoneConfig, Object>(WarzoneConfig.class);
		
	public WarzoneConfigBag(Warzone warzone) {
		this.warzone = warzone;
	}

	public WarzoneConfigBag() {
		// default zone settings (at War level) don't have a warzone
		this.warzone = null;
	}

	public static void afterUpdate(Warzone zone, CommandSender sender, String namedParamReturn, boolean wantsToPrint) {
		WarzoneYmlMapper.save(zone);

		String zoneReset = "Some changes may require a /resetzone. ";
		if (zone.getWarzoneConfig().getBoolean(WarzoneConfig.RESETONCONFIGCHANGE)) {
			zone.reinitialize(); // bring back team spawns etc
			zoneReset = "Zone reset. ";
		}

		if (wantsToPrint) {
			War.war.msg(sender, "Warzone config saved. " + zoneReset + namedParamReturn + " " + War.war.printConfig(zone));
		} else {
			War.war.msg(sender, "Warzone config saved. " + zoneReset + namedParamReturn);
		}

		War.war.log(sender.getName() + " updated warzone " + zone.getName() + " configuration." + namedParamReturn, Level.INFO);

		if (War.war.getWarHub() != null) { // maybe the zone was disabled/enabled
			War.war.getWarHub().getVolume().resetBlocks();
			War.war.getWarHub().initialize();
		}
	}
	
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
			return (Integer) bag.get(config);
		} else {
			// use War default config
			return War.war.getWarzoneDefaultConfig().getInt(config);
		}
	}

	public Boolean getBoolean(WarzoneConfig config) {
		if (bag.containsKey(config)) {
			return (Boolean) bag.get(config);
		} else {
			// use War default config
			return War.war.getWarzoneDefaultConfig().getBoolean(config);
		}
	}

	public ScoreboardType getScoreboardType(WarzoneConfig config) {
		if (bag.containsKey(config)) {
			return (ScoreboardType)bag.get(config);
		} else {
			// use War default config
			return War.war.getWarzoneDefaultConfig().getScoreboardType(config);
		}
	}

	public boolean contains(WarzoneConfig config) {
		return this.bag.containsKey(config);
	}

	public void reset() {
		this.bag.clear();
	}

	public void loadFrom(ConfigurationSection warzoneConfigSection) {
		for (WarzoneConfig config : WarzoneConfig.values()) {
			if (warzoneConfigSection.contains(config.toString())) {
				if (config.getConfigType().equals(Integer.class)) {
					this.put(config, warzoneConfigSection.getInt(config.toString()));
				} else if (config.getConfigType().equals(Boolean.class)) {
					this.put(config, warzoneConfigSection.getBoolean(config.toString()));
				} else if (config.getConfigType().equals(ScoreboardType.class)) {
					this.put(config, ScoreboardType.getFromString(warzoneConfigSection.getString(config.toString())));
				}
			}
		}
	}

	public void saveTo(ConfigurationSection warzoneConfigSection) {
		for (WarzoneConfig config : WarzoneConfig.values()) {
			if (this.bag.containsKey(config)) {
				if (config.getConfigType().equals(Integer.class)
						|| config.getConfigType().equals(Boolean.class)) {
					warzoneConfigSection.set(config.toString(), this.bag.get(config));
				} else {
					warzoneConfigSection.set(config.toString(), this.bag.get(config).toString());
				}
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
					if (this.warzone != null && namedParam.equals(WarzoneConfig.AUTOASSIGN.toString())) {
						this.warzone.getLobby().setLocation(this.warzone.getTeleport());
						this.warzone.getLobby().initialize();
					}
				} else if (warzoneConfig.getConfigType().equals(ScoreboardType.class)) {
					String type = namedParams.get(namedParam);
					this.bag.put(warzoneConfig, ScoreboardType.getFromString(type));
				}
				returnMessage += " " + warzoneConfig.toString() + " set to " + namedParams.get(namedParam);
			} else if (namedParam.equals("delete")) {
				String toDelete = namedParams.get(namedParam);
				warzoneConfig = WarzoneConfig.warzoneConfigFromString(toDelete);

				// param delete (to restore inheritance)
				if (warzoneConfig != null) {
					this.bag.remove(warzoneConfig);
					returnMessage += " " + warzoneConfig.toString() + " removed";
				}
			}
		}
		return returnMessage;
	}
}
