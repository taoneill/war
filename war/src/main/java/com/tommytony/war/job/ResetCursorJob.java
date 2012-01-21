package com.tommytony.war.job;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import com.tommytony.war.volume.BlockInfo;

public class ResetCursorJob implements Runnable {

	private final Block cornerBlock;
	private final BlockInfo[] originalCursorBlocks;
	private final boolean isSoutheast;

	public ResetCursorJob(Block cornerBlock, BlockInfo[] originalCursorBlocks, boolean isSoutheast) {
		this.cornerBlock = cornerBlock;
		this.originalCursorBlocks = originalCursorBlocks;
		this.isSoutheast = isSoutheast;
	}

	public void run() {
		if (this.isSoutheast) {
			this.cornerBlock.setType(this.originalCursorBlocks[0].getType());
			this.cornerBlock.setData(this.originalCursorBlocks[0].getData());
			this.cornerBlock.getRelative(BlockFace.WEST).setType(this.originalCursorBlocks[1].getType());
			this.cornerBlock.getRelative(BlockFace.WEST).setData(this.originalCursorBlocks[1].getData());
			this.cornerBlock.getRelative(BlockFace.NORTH).setType(this.originalCursorBlocks[2].getType());
			this.cornerBlock.getRelative(BlockFace.NORTH).setData(this.originalCursorBlocks[2].getData());
		} else {
			this.cornerBlock.setType(this.originalCursorBlocks[0].getType());
			this.cornerBlock.setData(this.originalCursorBlocks[0].getData());
			this.cornerBlock.getRelative(BlockFace.EAST).setType(this.originalCursorBlocks[1].getType());
			this.cornerBlock.getRelative(BlockFace.EAST).setData(this.originalCursorBlocks[1].getData());
			this.cornerBlock.getRelative(BlockFace.SOUTH).setType(this.originalCursorBlocks[2].getType());
			this.cornerBlock.getRelative(BlockFace.SOUTH).setData(this.originalCursorBlocks[2].getData());
		}
	}
}
