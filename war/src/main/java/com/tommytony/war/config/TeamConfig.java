package com.tommytony.war.config;


public enum TeamConfig {
	FLAGMUSTBEHOME (Boolean.class, "Flag must be home", "If true, enemy flag cannot be captured if your flag is out"),
	FLAGPOINTSONLY (Boolean.class, null, null), // obsolete
	FLAGRETURN (FlagReturn.class, "Flag return destination", "Defines where the flag must be returned to capture"),
	LIFEPOOL (Integer.class, "Lifepool", "Maximum team lives"),
	MAXSCORE (Integer.class, "Max score", "When the team gets this many points, they win"),
	NOHUNGER (Boolean.class, "No Hunger", "If true, player hunger will not decrease"),
	PLAYERLOADOUTASDEFAULT (Boolean.class, "Player loadout as default", "If set, the default loadout will be the items the player brings into the zone"),
	RESPAWNTIMER (Integer.class, "Respawn time", "Force players to wait in their spawn for this many seconds after each death"),
	SATURATION (Integer.class, "Saturation", "Set player saturation to this level after each death"),
	SPAWNSTYLE (TeamSpawnStyle.class, "Spawn style", "Choose from several different sizes for the spawn points"),
	TEAMSIZE (Integer.class, "Team size", "Maximum players that may play on a team"),
	PERMISSION (String.class, "Required permission", "Only allow players with a certain permission to join a team"),
	XPKILLMETER (Boolean.class, "XP kill meter", "Use the XP bar to count kills"),
	KILLSTREAK (Boolean.class, "Killstreak rewards", "Reward players for kills based on war.yml configuration"),
	BLOCKWHITELIST (String.class, "Block whitelist", "Comma-separated list of blocks players may break or place. 'all' removes this limit"),
	PLACEBLOCK (Boolean.class, "Place blocks", "If false, players cannot build. See unbreakable"),
	APPLYPOTION(String.class, "Apply potion", "Give players a potion effect after each death. Format: EFFECT;DURATION;STRENGTH"),
	ECOREWARD(Double.class, "Economy reward", "Give the winning team this much money. Requires Vault plugin"),
	INVENTORYDROP(Boolean.class, "Drop inventory", "If false, players will not drop items on death"),
	BORDERDROP(Boolean.class, "Drop near border", "If false, players cannot drop items near the border, to prevent duping");
	
	private final Class<?> configType;
	private final String title;
	private final String description;

	TeamConfig(Class<?> configType, String title, String description) {
		this.configType = configType;
		this.title = title;
		this.description = description;
	}

	public Class<?> getConfigType() {
		return configType;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public static TeamConfig teamConfigFromString(String str) {
		String lowered = str.toLowerCase();
		for (TeamConfig config : TeamConfig.values()) {
			if (config.toString().startsWith(lowered)) {
				return config;
			}
		}
		return null;
	}
		
	public String toStringWithValue(Object value) {
		return this.toString() + ":" + value.toString();
	}
	
	@Override
	public String toString() {
		return super.toString().toLowerCase();
	}
}
