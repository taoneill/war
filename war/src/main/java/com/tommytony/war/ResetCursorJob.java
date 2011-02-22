package com.tommytony.war;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class ResetCursorJob implements Runnable {


	private final Block cornerBlock;
	private final Material[] originalCursorBlocks;
	private final boolean isSoutheast;

	public ResetCursorJob(Block cornerBlock, Material[] originalCursorBlocks, boolean isSoutheast) {
		this.cornerBlock = cornerBlock;
		this.originalCursorBlocks = originalCursorBlocks;
		this.isSoutheast = isSoutheast;

	}

	public void run() {
		if(isSoutheast) {
			cornerBlock.setType(originalCursorBlocks[0]);
			cornerBlock.getFace(BlockFace.WEST).setType(originalCursorBlocks[1]);
			cornerBlock.getFace(BlockFace.NORTH).setType(originalCursorBlocks[2]);
		} else {
			cornerBlock.setType(originalCursorBlocks[0]);
			cornerBlock.getFace(BlockFace.EAST).setType(originalCursorBlocks[1]);
			cornerBlock.getFace(BlockFace.SOUTH).setType(originalCursorBlocks[2]);
		}
	}
}
