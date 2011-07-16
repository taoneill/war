package com.tommytony.war;

import org.bukkit.ChatColor;
import org.bukkit.Material;

public class TeamKind {
	private final Material material;
	private final byte data;
	private final String defaultName;
	private final ChatColor color;

	public TeamKind(String defaultName, Material material, byte data, ChatColor color) {
		this.defaultName = defaultName;
		this.material = material;
		this.data = data;
		this.color = color;

	}

	public Material getMaterial() {
		return this.material;
	}

	public byte getData() {
		return this.data;
	}

	public String getDefaultName() {
		return this.defaultName;
	}

	public ChatColor getColor() {
		return this.color;
	}
}
