package com.tommytony.war;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.getspout.spoutapi.gui.Color;

public enum TeamKind {
	WHITE ((byte) 0, Material.WOOL, ChatColor.WHITE, new Color(255,255,255)),
	ORANGE ((byte) 1, Material.WOOL, ChatColor.GOLD, new Color(255,128,0)),
	MAGENTA ((byte) 2, Material.WOOL, ChatColor.LIGHT_PURPLE, new Color(255,128,255)),
	BLUE ((byte) 3, Material.WOOL, ChatColor.BLUE, new Color(0,0,255)),
	GOLD ((byte) 4, Material.WOOL, ChatColor.YELLOW, new Color(0,255,255)), // yellow = gold
	GREEN ((byte) 5, Material.WOOL, ChatColor.GREEN, new Color(0,255,0)),
	PINK ((byte) 6, Material.WOOL, ChatColor.WHITE, new Color(255,255,255)),
	GRAY ((byte) 7, Material.WOOL, ChatColor.DARK_GRAY, new Color(100,100,100)),
	IRON ((byte) 8, Material.WOOL, ChatColor.GRAY, new Color(200,200,200)), // lightgrey = iron
	DIAMOND ((byte) 9, Material.WOOL, ChatColor.DARK_AQUA, new Color(128,255,255)), // cyan = diamond
	PURPLE ((byte) 10, Material.WOOL, ChatColor.DARK_PURPLE, new Color(128,0,255)),
	NAVY ((byte) 11, Material.WOOL, ChatColor.DARK_BLUE, new Color(0,0,128)),
	BROWN ((byte) 12, Material.WOOL, ChatColor.DARK_RED, new Color(128,0,0)),
	DARKGREEN ((byte) 13, Material.WOOL, ChatColor.DARK_GREEN, new Color(0,128,0)),
	RED ((byte) 14, Material.WOOL, ChatColor.RED, new Color(255,0,0)),
	BLACK ((byte) 15, Material.WOOL, ChatColor.BLACK, new Color(0,0,0));

	private final byte data;
	private final ChatColor color;
	private final Material material;
	private final Color spoutcolor;

	private TeamKind(byte data, Material material, ChatColor color, Color spoutcolor) {
		this.data = data;
		this.material = material;
		this.color = color;
		this.spoutcolor = spoutcolor;
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
	public Color getSpoutColor() {
		return this.spoutcolor;
	}

	public Material getMaterial() {
		return this.material;
	}

	@Override
	public String toString() {
		return super.toString().toLowerCase();
	}
}
