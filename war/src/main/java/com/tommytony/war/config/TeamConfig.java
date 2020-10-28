package com.tommytony.war.config;


public enum TeamConfig {
	FLAGMUSTBEHOME (Boolean.class, "Flag Must Be Home", "If true, enemy flag cannot be captured if your flag is out"),
	FLAGPOINTSONLY (Boolean.class, null, null), // obsolete
	FLAGRETURN (FlagReturn.class, "Flag Return Destination", "Defines where the flag must be returned to capture\nOptions: spawn, flag, or both"),
	LIFEPOOL (Integer.class, "Lifepool", "Sets maximum team lives"),
	MAXSCORE (Integer.class, "Max Score", "Sets the point limit for when a team will win"),
	NOHUNGER (Boolean.class, "No Hunger", "If true, player hunger will not decrease"),
	PLAYERLOADOUTASDEFAULT (Boolean.class, "Player Loadout As Default", "If true, the default loadout will be the items the player brings into the zone"),
	RESPAWNTIMER (Integer.class, "Respawn Time", "Time, in seconds, required to wait after each death"),
	SATURATION (Integer.class, "Saturation", "Set player saturation to this level after each death"),
	SPAWNSTYLE (TeamSpawnStyle.class, "Spawn Style", "Sets the type spawn point\nOptions: small, big, flat, invisible"),
	TEAMSIZE (Integer.class, "Team Size", "Maximum players that may play on a team"),
	PERMISSION (String.class, "Required Permission", "Only allow players with a certain permission to join a team"),
	XPKILLMETER (Boolean.class, "XP Kill Meter", "Use the XP bar to count kills"),
	KILLSTREAK (Boolean.class, "Killstreak Rewards", "Reward players for kills based on war.yml configuration"),
	BLOCKWHITELIST (String.class, "Block Whitelist", "Comma-separated list of blocks players may break or place, 'all' removes this limit"),
	PLACEBLOCK (Boolean.class, "Place Blocks", "If true, players can place blocks"),
	APPLYPOTION(String.class, "Apply Potion Effect", "Give players a potion effect after each death, Format: EFFECT;DURATION;STRENGTH"),
	ECOREWARD(Double.class, "Economy Reward", "Give the winning team this much money, requires Vault plugin"),
	INVENTORYDROP(Boolean.class, "Drop Inventory", "If true, players will drop items on death"),
	BORDERDROP(Boolean.class, "Drop Near Border", "If true, players can drop items near the border\nUsually enabled to prevent duping");
	
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
