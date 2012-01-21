package com.tommytony.war.utility;

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
	private Byte rawNote;

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
		this.lines = signLines;
	}
	
	// Container block
	public DeferredBlockReset(int x, int y, int z, int blockType, byte blockData, List<ItemStack> contents) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.blockType = blockType;
		this.blockData = blockData;
		this.items = contents;
	}
	
	// Noteblock
	public DeferredBlockReset(int x, int y, int z, int blockType, byte blockData, byte rawNote) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.blockType = blockType;
		this.blockData = blockData;
		this.rawNote = rawNote;
	}

	public int getX() {
		return this.x;
	}

	public int getY() {
		return this.y;
	}

	public int getZ() {
		return this.z;
	}

	public int getBlockType() {
		return this.blockType;
	}

	public byte getBlockData() {
		return this.blockData;
	}

	public String[] getLines() {
		return this.lines;
	}

	public List<ItemStack> getItems() {
		return items;
	}

	public Byte getRawNote() {
		return rawNote;
	}
}
