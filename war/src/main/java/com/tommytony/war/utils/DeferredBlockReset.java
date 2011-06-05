package com.tommytony.war.utils;

import java.util.List;

import org.bukkit.inventory.ItemStack;

public class DeferredBlockReset {

	private final int x;
	private final int y;
	private final int z;
	private final int blockType;
	private final byte blockData;
	private String[] lines;
	private List<ItemStack> items;

	public DeferredBlockReset(int x, int y, int z, int blockType, byte blockData) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.blockType = blockType;
		this.blockData = blockData;
	}
	
	// Signs
	public DeferredBlockReset(int x, int y, int z, int blockType, byte blockData, String[] signLines) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.blockType = blockType;
		this.blockData = blockData;
		lines = signLines;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getZ() {
		return z;
	}

	public int getBlockType() {
		return blockType;
	}

	public byte getBlockData() {
		return blockData;
	}

	public String[] getLines() {
		return lines;
	}

	
	
}
