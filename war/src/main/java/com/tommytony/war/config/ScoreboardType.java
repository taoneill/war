package com.tommytony.war.config;

public enum ScoreboardType {

	NONE(null),
	POINTS("Points"),
	LIFEPOOL("Lifepool"),
	TOPKILLS("Top kills"),
	PLAYERCOUNT("Player count"),
	SWITCHING("Switching");
	private final String displayName;

	ScoreboardType(String displayName) {
		this.displayName = displayName;
	}

	public static ScoreboardType getFromString(String string) {
		for (ScoreboardType boardMode : ScoreboardType.values()) {
			if (string.toLowerCase().equals(boardMode.toString())) {
				return boardMode;
			}
		}

		return ScoreboardType.NONE;
	}

	@Override
	public String toString() {
		return super.toString().toLowerCase();
	}

	public String getDisplayName() {
		return displayName;
	}
}
