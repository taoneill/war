package com.tommytony.war;

import org.bukkit.ChatColor;
import org.bukkit.Material;

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

	public Material getMaterial() {
		return this.material;
	}

	@Override
	public String toString() {
		return super.toString().toLowerCase();
	}
}
