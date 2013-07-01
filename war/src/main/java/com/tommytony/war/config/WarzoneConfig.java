package com.tommytony.war.config;

public enum WarzoneConfig {
	AUTOASSIGN (Boolean.class),
	BLOCKHEADS (Boolean.class),
	DEATHMESSAGES (Boolean.class),
	DISABLED (Boolean.class),
	FRIENDLYFIRE (Boolean.class),
	GLASSWALLS (Boolean.class),
	INSTABREAK (Boolean.class),
	MINTEAMS (Integer.class),
	MINPLAYERS (Integer.class),
	MONUMENTHEAL (Integer.class),
	NOCREATURES (Boolean.class),
	NODROPS (Boolean.class),
	PVPINZONE (Boolean.class),
	REALDEATHS (Boolean.class),
	RESETONEMPTY (Boolean.class),
	RESETONCONFIGCHANGE (Boolean.class),
	RESETONLOAD (Boolean.class),
	RESETONUNLOAD (Boolean.class),
	UNBREAKABLE (Boolean.class),
	SOUPHEALING (Boolean.class);
	
	
	private final Class<?> configType;

	private WarzoneConfig(Class<?> configType) {
		this.configType = configType;		
	}

	public Class<?> getConfigType() {
		return configType;
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
