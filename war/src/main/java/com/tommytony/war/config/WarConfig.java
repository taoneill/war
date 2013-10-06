package com.tommytony.war.config;


public enum WarConfig {
	BUILDINZONESONLY (Boolean.class),
	DISABLEBUILDMESSAGE (Boolean.class),
	DISABLEPVPMESSAGE (Boolean.class),
	KEEPOLDZONEVERSIONS (Boolean.class),
	MAXZONES (Integer.class),
	PVPINZONESONLY (Boolean.class),
	TNTINZONESONLY (Boolean.class),
	RESETSPEED (Integer.class);
	
	private final Class<?> configType;

	private WarConfig(Class<?> configType) {
		this.configType = configType;		
	}

	public Class<?> getConfigType() {
		return configType;
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
