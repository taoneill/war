package com.tommytony.war.jobs;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import com.tommytony.war.volumes.BlockInfo;

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
		if(isSoutheast) {
			cornerBlock.setType(originalCursorBlocks[0].getType());
			cornerBlock.setData(originalCursorBlocks[0].getData());
			cornerBlock.getFace(BlockFace.WEST).setType(originalCursorBlocks[1].getType());
			cornerBlock.getFace(BlockFace.WEST).setData(originalCursorBlocks[1].getData());
			cornerBlock.getFace(BlockFace.NORTH).setType(originalCursorBlocks[2].getType());
			cornerBlock.getFace(BlockFace.NORTH).setData(originalCursorBlocks[2].getData());
		} else {
			cornerBlock.setType(originalCursorBlocks[0].getType());
			cornerBlock.setData(originalCursorBlocks[0].getData());
			cornerBlock.getFace(BlockFace.EAST).setType(originalCursorBlocks[1].getType());
			cornerBlock.getFace(BlockFace.EAST).setData(originalCursorBlocks[1].getData());
			cornerBlock.getFace(BlockFace.SOUTH).setType(originalCursorBlocks[2].getType());
			cornerBlock.getFace(BlockFace.SOUTH).setData(originalCursorBlocks[2].getData());
		}
	}
}
