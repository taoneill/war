package com.tommytony.war.mappers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

public class PropertiesConverter {
	File properties;
	File yaml;
	public PropertiesConverter (File properties, File yaml) {
		this.properties = properties;
		this.yaml = yaml;
	}
	public void ConvertWarCfg() throws FileNotFoundException, IOException, InvalidConfigurationException {
		PropertiesFile propertiesUtil = new PropertiesFile(properties.getAbsolutePath());
		YamlConfiguration yamlUtil = new YamlConfiguration();
		if (!yaml.exists()) {
			yaml.createNewFile();
		}
		propertiesUtil.load();
		yamlUtil.load(yaml);
		yamlUtil.set("warzones", propertiesUtil.getString("warzones"));
	    yamlUtil.set("zoneMakers", propertiesUtil.getString("zoneMakers"));
	    yamlUtil.set("commandWhitelist", propertiesUtil.getString("commandWhitelist"));
	    yamlUtil.set("defaultLoadout", propertiesUtil.getString("defaultLoadout"));
	    yamlUtil.set("defaultExtraLoadouts", propertiesUtil.getString("defaultExtraLoadouts"));
	    yamlUtil.set("maxZones", propertiesUtil.getInt("maxZones"));
	    yamlUtil.set("defaultLifePool", propertiesUtil.getInt("defaultLifePool"));
	    yamlUtil.set("defaultMonumentHeal", propertiesUtil.getInt("defaultMonumentHeal"));
	    yamlUtil.set("defaultFriendlyFire", propertiesUtil.getBoolean("defaultFriendlyFire"));
	    yamlUtil.set("defaultAutoAssignOnly", propertiesUtil.getBoolean("defaultAutoAssignOnly"));
	    yamlUtil.set("defaultFlagPointsOnly", propertiesUtil.getBoolean("defaultFlagPointsOnly"));
	    yamlUtil.set("defaultTeamCap", propertiesUtil.getInt("defaultTeamCap"));
	    yamlUtil.set("defaultScoreCap", propertiesUtil.getInt("defaultScoreCap"));
	    yamlUtil.set("pvpInZonesOnly", propertiesUtil.getBoolean("pvpInZonesOnly"));
	    yamlUtil.set("defaultBlockHeads", propertiesUtil.getBoolean("defaultBlockHeads"));
	    yamlUtil.set("buildInZonesOnly", propertiesUtil.getBoolean("buildInZonesOnly"));
	    yamlUtil.set("disablePvpMessage", propertiesUtil.getBoolean("disablePvpMessage"));
	    yamlUtil.set("tntInZonesOnly", propertiesUtil.getBoolean("tntInZonesOnly"));
	    yamlUtil.set("spawnStyle", propertiesUtil.getString("spawnStyle"));
	    yamlUtil.set("flagReturn", propertiesUtil.getString("flagReturn"));
	    yamlUtil.set("defaultReward", propertiesUtil.getString("defaultReward"));
	    yamlUtil.set("defaultUnbreakableZoneBlocks", propertiesUtil.getBoolean("defaultUnbreakableZoneBlocks"));
	    yamlUtil.set("defaultNoCreatures", propertiesUtil.getBoolean("defaultNoCreatures"));
	    yamlUtil.set("defaultGlassWalls", propertiesUtil.getBoolean("defaultGlassWalls"));
	    yamlUtil.set("defaultPvpInZone", propertiesUtil.getBoolean("defaultPvpInZone"));
	    yamlUtil.set("defaultInstaBreak", propertiesUtil.getBoolean("defaultInstaBreak"));
	    yamlUtil.set("defaultNoDrops", propertiesUtil.getBoolean("defaultNoDrops"));
	    yamlUtil.set("defaultNoHunger", propertiesUtil.getBoolean("defaultNoHunger"));
	    yamlUtil.set("defaultSaturation", propertiesUtil.getInt("defaultSaturation"));
	    yamlUtil.set("defaultMinPlayers", propertiesUtil.getInt("defaultMinPlayers"));
	    yamlUtil.set("defaultMinTeams", propertiesUtil.getInt("defaultMinTeams"));
	    yamlUtil.set("defaultResetOnEmpty", propertiesUtil.getBoolean("defaultResetOnEmpty"));
	    yamlUtil.set("defaultResetOnLoad", propertiesUtil.getBoolean("defaultResetOnLoad"));
	    yamlUtil.set("defaultResetOnUnload", propertiesUtil.getBoolean("defaultResetOnUnload"));
	    yamlUtil.set("warhub", propertiesUtil.getString("warhub"));
	    yamlUtil.save(yaml);
	    propertiesUtil.close();

	}
	public void ConvertZoneCfg() throws FileNotFoundException, IOException, InvalidConfigurationException {
		PropertiesFile propertiesUtil = new PropertiesFile(properties.getAbsolutePath());
		YamlConfiguration yamlUtil = new YamlConfiguration();
		if (!yaml.exists()) {
			yaml.createNewFile();
		}
		propertiesUtil.load();
		yamlUtil.load(yaml);
		yamlUtil.set("name", propertiesUtil.getString("name"));
	    yamlUtil.set("world", propertiesUtil.getString("world"));
	    yamlUtil.set("teleport", propertiesUtil.getString("teleport"));
	    yamlUtil.set("teams", propertiesUtil.getString("teams"));
	    yamlUtil.set("teamFlags", propertiesUtil.getString("teamFlags"));
	    yamlUtil.set("friendlyFire", propertiesUtil.getString("friendlyFire"));
	    yamlUtil.set("loadout", propertiesUtil.getString("loadout"));
	    yamlUtil.set("extraLoadouts", propertiesUtil.getString("extraLoadouts"));
	    yamlUtil.set("author", propertiesUtil.getString("author"));
	    yamlUtil.set("lifePool", propertiesUtil.getString("lifePool"));
	    yamlUtil.set("monumentHeal", propertiesUtil.getString("monumentHeal"));
	    yamlUtil.set("autoAssignOnly", propertiesUtil.getString("autoAssignOnly"));
	    yamlUtil.set("flagPointsOnly", propertiesUtil.getString("flagPointsOnly"));
	    yamlUtil.set("teamCap", propertiesUtil.getString("teamCap"));
	    yamlUtil.set("scoreCap", propertiesUtil.getString("scoreCap"));
	    yamlUtil.set("blockHeads", propertiesUtil.getString("blockHeads"));
	    yamlUtil.set("spawnStyle", propertiesUtil.getString("spawnStyle"));
	    yamlUtil.set("flagReturn", propertiesUtil.getString("flagReturn"));
	    yamlUtil.set("reward", propertiesUtil.getString("reward"));
	    yamlUtil.set("unbreakableZoneBlocks", propertiesUtil.getString("unbreakableZoneBlocks"));
	    yamlUtil.set("disabled", propertiesUtil.getString("disabled"));
	    yamlUtil.set("noCreatures", propertiesUtil.getString("noCreatures"));
	    yamlUtil.set("glassWalls", propertiesUtil.getString("glassWalls"));
	    yamlUtil.set("pvpInZone", propertiesUtil.getString("pvpInZone"));
	    yamlUtil.set("instaBreak", propertiesUtil.getString("instaBreak"));
	    yamlUtil.set("noDrops", propertiesUtil.getString("noDrops"));
	    yamlUtil.set("noHunger", propertiesUtil.getString("noHunger"));
	    yamlUtil.set("saturation", propertiesUtil.getString("saturation"));
	    yamlUtil.set("minPlayers", propertiesUtil.getString("minPlayers"));
	    yamlUtil.set("minTeams", propertiesUtil.getString("minTeams"));
	    yamlUtil.set("resetOnEmpty", propertiesUtil.getString("resetOnEmpty"));
	    yamlUtil.set("resetOnLoad", propertiesUtil.getString("resetOnLoad"));
	    yamlUtil.set("resetOnUnload", propertiesUtil.getString("resetOnUnload"));
	    yamlUtil.set("rallyPoint", propertiesUtil.getString("rallyPoint"));
	    yamlUtil.set("monuments", propertiesUtil.getString("monuments"));
	    yamlUtil.set("lobby", propertiesUtil.getString("lobby"));
	    yamlUtil.save(yaml);
	    propertiesUtil.close();

	}
}
