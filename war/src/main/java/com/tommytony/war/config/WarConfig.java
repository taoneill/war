package com.tommytony.war.config;


public enum WarConfig {
	BUILDINZONESONLY (Boolean.class, "Build in zones only", "Allow building in zones only"),
	DISABLEBUILDMESSAGE (Boolean.class, "Disable build message", "Silently prevent building outside zones"),
	DISABLEPVPMESSAGE (Boolean.class, "Disable PVP message", "Silently prevent PVP"),
	KEEPOLDZONEVERSIONS (Boolean.class, "Keep old zone versions", "If true, archive the warzone on each save"),
	MAXZONES (Integer.class, "Max zones", "Limit on number of zones that can be created"),
	PVPINZONESONLY (Boolean.class, "PVP in zones only", "If true, limits PVP to warzones"),
	TNTINZONESONLY (Boolean.class, "TNT in zones only", "If true, limits TNT to warzones"),
	RESETSPEED (Integer.class, "Reset speed", "Number of blocks to reset per tick"),
	MAXSIZE (Integer.class, "Max size", "Maximum volume of a warzone"),
	LANGUAGE (String.class, "Language", "Preferred server language"),
	AUTOJOIN (String.class, "Auto-join", "Name of warzone to send players to upon join"),
	TPWARMUP(Integer.class, "TP warmup", "Amount of seconds a player must wait after requesting a teleport");
	
	private final Class<?> configType;
	private final String title;
	private final String description;

	WarConfig(Class<?> configType, String title, String description) {
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

	public static WarConfig warConfigFromString(String str) {
		String lowered = str.toLowerCase();
		for (WarConfig config : WarConfig.values()) {
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
