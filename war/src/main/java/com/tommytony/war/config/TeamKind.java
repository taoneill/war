package com.tommytony.war.config;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.getspout.spoutapi.gui.Color;

public enum TeamKind {
	WHITE ((byte) 0, Material.WOOL, ChatColor.WHITE),
	ORANGE ((byte) 1, Material.WOOL, ChatColor.GOLD),
	MAGENTA ((byte) 2, Material.WOOL, ChatColor.LIGHT_PURPLE),
	BLUE ((byte) 3, Material.WOOL, ChatColor.BLUE),
	GOLD ((byte) 4, Material.WOOL, ChatColor.YELLOW), // yellow = gold
	GREEN ((byte) 5, Material.WOOL, ChatColor.GREEN),
	PINK ((byte) 6, Material.WOOL, ChatColor.WHITE),
	GRAY ((byte) 7, Material.WOOL, ChatColor.DARK_GRAY),
	IRON ((byte) 8, Material.WOOL, ChatColor.GRAY), // lightgrey = iron
	DIAMOND ((byte) 9, Material.WOOL, ChatColor.DARK_AQUA), // cyan = diamond
	PURPLE ((byte) 10, Material.WOOL, ChatColor.DARK_PURPLE),
	NAVY ((byte) 11, Material.WOOL, ChatColor.DARK_BLUE),
	BROWN ((byte) 12, Material.WOOL, ChatColor.DARK_RED),
	DARKGREEN ((byte) 13, Material.WOOL, ChatColor.DARK_GREEN),
	RED ((byte) 14, Material.WOOL, ChatColor.RED),
	BLACK ((byte) 15, Material.WOOL, ChatColor.BLACK);

	private final byte data;
	private final ChatColor color;
	private final Material material;

	private TeamKind(byte data, Material material, ChatColor color) {
		this.data = data;
		this.material = material;
		this.color = color;
	}

	public static TeamKind teamKindFromString(String str) {
		String lowered = str.toLowerCase();
		for (TeamKind kind : TeamKind.values()) {
			if (kind.toString().startsWith(lowered)) {
				return kind;
			}
		}
		return null;
	}

	public byte getData() {
		return this.data;
	}
	public ChatColor getColor() {
		return this.color;
	}
	
	/**
	 * Don't call unless War.war.isSpoutServer() is true
	 * @return
	 */
	public Color getSpoutColor() {
		int colorCode = (int)this.data;
		switch (colorCode) {
			case 0:
				return new Color(255,255,255);
			case 1:
				return new Color(255,128,0);
			case 2:
				return new Color(255,128,255);
			case 3:
				return new Color(0,0,255);
			case 4:
				return new Color(0,255,255);
			case 5:
				return new Color(0,255,0);
			case 6:
				return new Color(255,255,255);
			case 7:
				return new Color(100,100,100);
			case 8:
				return new Color(200,200,200);
			case 9:
				return new Color(128,255,255);
			case 10:
				return new Color(128,0,255);
			case 11:
				return new Color(0,0,128);
			case 12:
				return new Color(128,0,0);
			case 13:
				return new Color(0,128,0);
			case 14:
				return new Color(255,0,0);
			case 15:
				return new Color(0,0,0);
			default:
				return new Color(255,255,255);
		}
	}

	public Material getMaterial() {
		return this.material;
	}

	@Override
	public String toString() {
		return super.toString().toLowerCase();
	}
}
