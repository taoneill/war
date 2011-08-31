package com.tommytony.war;

/**
 *
 * @author tommytony
 *
 */
public enum TeamSpawnStyle {
	INVISIBLE,
	SMALL,
	FLAT,
	BIG;

	@Override
	public String toString() {
		return super.toString().toLowerCase();
	}

	public static TeamSpawnStyle getStyleByString(String string) {
		for (TeamSpawnStyle style : TeamSpawnStyle.values()) {
			if (string.toLowerCase().equals(style.toString())) {
				return style;
			}
		}

		return TeamSpawnStyle.BIG;
	}
}
