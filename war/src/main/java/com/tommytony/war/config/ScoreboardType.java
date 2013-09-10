package com.tommytony.war.config;

public enum ScoreboardType {

	NONE(null),
	POINTS("Points"),
	LIFEPOOL("Lifepool");
	private final String displayName;

	private ScoreboardType(String displayName) {
		this.displayName = displayName;
	}

	@Override
	public String toString() {
		return super.toString().toLowerCase();
	}

	public static ScoreboardType getFromString(String string) {
		for (ScoreboardType boardMode : ScoreboardType.values()) {
			if (string.toLowerCase().equals(boardMode.toString())) {
				return boardMode;
			}
		}

		return ScoreboardType.NONE;
	}

	public String getDisplayName() {
		return displayName;
	}
}
