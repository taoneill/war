package com.tommytony.war.config;

public enum WarzoneConfig {
	AUTOASSIGN (Boolean.class, "Auto-Assign", "If true, distributes players across teams"),
	BLOCKHEADS (Boolean.class, "Team Helmets", "If true, players are given a team-colored hat"),
	DEATHMESSAGES (Boolean.class, "Death Notification", "If true, notify the zone when players are killed"),
	DISABLED (Boolean.class, "Disable Zone", "If true, prevent players from joining the zone"),
	FRIENDLYFIRE (Boolean.class, "Friendly Fire", "If true, players are allowed to injure teammates"),
	GLASSWALLS (Boolean.class, "Glass Walls", "If true, use magic glass walls to keep players in/out of zones"),
	INSTABREAK (Boolean.class, "Insta-Break", "If true, players break blocks instantly\nUseful for Spleef gamemodes"),
	MINTEAMS (Integer.class, "Minimum Teams", "Minimum number of active teams required to start the battle"),
	MINPLAYERS (Integer.class, "Minimum Players", "Minimum number of players required per team to start the battle"),
	MONUMENTHEAL (Integer.class, "Monument Heal", "Number of hearts given to players jumping on the monument"),
	NOCREATURES (Boolean.class, "No Mobs", "If true, prevent mob spawning"),
	NODROPS (Boolean.class, "No Drops", "If true, prevent players from dropping items"),
	PVPINZONE (Boolean.class, "PVP", "If true, PVP is enabled\nUseful for Spleef gamemodes"),
	REALDEATHS (Boolean.class, "Real Deaths", "If true, send players to the real Minecraft death screen"),
	RESETONEMPTY (Boolean.class, "Reset on Empty", "If true, reset the zone when all players leave"),
	RESETONCONFIGCHANGE (Boolean.class, "Reset on Config Change", "If true, reset every time the zone config is modified"),
	RESETONLOAD (Boolean.class, "Reset on Load", "If true, reset warzone when the server starts"),
	RESETONUNLOAD (Boolean.class, "Reset on Unload", "If true, reset warzone when the server stops"),
	UNBREAKABLE (Boolean.class, "Unbreakable Blocks", "If true, prevent block breaking"),
	JOINMIDBATTLE (Boolean.class, "Join Mid-Battle", "If true, players are allowed to join during a battle"),
	AUTOJOIN (Boolean.class, "Auto-Join", "If true, bypass the zone lobby and auto-assign the player a team"),
	SCOREBOARD (ScoreboardType.class, "Scoreboard Type", "Type of scoreboard for this zone\nOptions: none, points, lifepool, top kills"),
	SOUPHEALING (Boolean.class, "Soup Healing", "If true, allow players to heal by consuming soup"),
	ALLOWENDER (Boolean.class, "Allow Ender Chests", "If true, ender chests are allowed\nEnder chests are usually blocked to prevent item duplication"),
	RESETBLOCKS (Boolean.class, "Reset Blocks", "If true, reset warzone blocks each battle"),
	CAPTUREPOINTTIME (Integer.class, "Capture Control Time", "Time, in seconds, required to gain control of a capture point"),
	PREPTIME(Integer.class, "Preparation Time", "Time, in seconds, before players are allowed to fight");
	
	
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
