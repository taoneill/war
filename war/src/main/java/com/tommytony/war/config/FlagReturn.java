package com.tommytony.war.config;

public enum FlagReturn {
	BOTH,
	FLAG,
	SPAWN;

	@Override
	public String toString() {
		return super.toString().toLowerCase();
	}

	public static FlagReturn getFromString(String string) {
		for (FlagReturn flagMode : FlagReturn.values()) {
			if (string.toLowerCase().equals(flagMode.toString())) {
				return flagMode;
			}
		}

		return FlagReturn.BOTH;
	}
}
