package com.tommytony.war.config;

public enum WarzoneConfig {
	AUTOASSIGN (Boolean.class, "Auto-assign", "If true, distributes players across teams"),
	BLOCKHEADS (Boolean.class, "Team helmets", "If true, players are given a team-colored hat"),
	DEATHMESSAGES (Boolean.class, "Death notification", "If true, notify the zone when players are killed"),
	DISABLED (Boolean.class, "Disabled", "If true, prevent players from joining the zone"),
	FRIENDLYFIRE (Boolean.class, "Friendly fire", "If true, players are allowed to injure teammates"),
	GLASSWALLS (Boolean.class, "Glass walls", "If true, use magic glass walls to keep players in/out of zones"),
	INSTABREAK (Boolean.class, "Insta-break", "If true, players break blocks instantly (Spleef)"),
	MINTEAMS (Integer.class, "Min teams", "Minimum number of active teams required to start the battle"),
	MINPLAYERS (Integer.class, "Min players", "Minimum number of players required per team to start the battle"),
	MONUMENTHEAL (Integer.class, "Monument heal", "Number of hearts given to players jumping on the monument"),
	NOCREATURES (Boolean.class, "No creatures", "If true, prevent mob spawning"),
	NODROPS (Boolean.class, "No drops", "If true, prevent players from dropping items"),
	PVPINZONE (Boolean.class, "PVP", "Enable/disable PVP in the zone (Spleef)"),
	REALDEATHS (Boolean.class, "Real deaths", "If true, send players to the real Minecraft death screen"),
	RESETONEMPTY (Boolean.class, "Reset on empty", "If true, reset the zone when all players leave"),
	RESETONCONFIGCHANGE (Boolean.class, "Reset on config change", "If true, reset every time the zone config is modified"),
	RESETONLOAD (Boolean.class, "Reset on load", "If true, reset warzone when the server starts"),
	RESETONUNLOAD (Boolean.class, "Reset on unload", "If true, reset warzone when the server stops"),
	UNBREAKABLE (Boolean.class, "Unbreakable", "If true, prevent breaking blocks"),
	JOINMIDBATTLE (Boolean.class, "Join mid battle", "If true, players are allowed to join during a battle"),
	AUTOJOIN (Boolean.class, "Auto-join", "If true, bypass the zone lobby and auto-assign the player"),
	SCOREBOARD (ScoreboardType.class, "Scoreboard type", "Type of scoreboard for this zone (none, points, lifepool, top kills)"),
	SOUPHEALING (Boolean.class, "Soup healing", "If true, allow players to heal by consuming soup"),
	ALLOWENDER (Boolean.class, "Allow ender chests", "Ender chests are usually blocked to prevent item duplication"),
	RESETBLOCKS (Boolean.class, "Reset blocks", "If true, reset warzone blocks each battle");
	
	
	private final Class<?> configType;
	private final String title;
	private final String description;

	WarzoneConfig(Class<?> configType, String title, String description) {
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

	public static WarzoneConfig warzoneConfigFromString(String str) {
		String lowered = str.toLowerCase();
		for (WarzoneConfig config : WarzoneConfig.values()) {
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
