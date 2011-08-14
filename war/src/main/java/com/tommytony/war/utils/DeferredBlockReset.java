package com.tommytony.war.utils;

public class DeferredBlockReset {

	private final int x;
	private final int y;
	private final int z;
	private final int blockType;
	private final byte blockData;
	private String[] lines;

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
}
