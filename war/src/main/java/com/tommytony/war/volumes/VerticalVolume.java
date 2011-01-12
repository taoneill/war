package com.tommytony.war.volumes;

import org.bukkit.Block;

import bukkit.tommytony.war.War;

import com.tommytony.war.Warzone;

public class VerticalVolume extends Volume{

	public VerticalVolume(String name, War war, Warzone warzone) {
		super(name, war, warzone);

	}
	
	@Override
	public void setCornerOne(Block block){
		// corner one defaults to topmost corner
		Block topBlock = getWorld().getBlockAt(block.getX(), 128, block.getZ());
		super.setCornerOne(topBlock);
	}
	
	@Override
	public void setCornerTwo(Block block){
		// corner one defaults to bottom most corner
		Block bottomBlock = getWorld().getBlockAt(block.getX(), 0, block.getZ());
		super.setCornerTwo(bottomBlock);
	}
	
	public boolean isWallBlock(Block block){
		return isEastWallBlock(block) || isNorthWallBlock(block) || isSouthWallBlock(block) || isWestWallBlock(block);
	}
	
	public boolean isEastWallBlock(Block block) {
		if(getMinZ() == block.getZ()
				&& block.getX() <= getMaxX()
				&& block.getX() >= getMinX()) {
			return true; 	// east wall
		}
		return false;
	}
	
	public boolean isSouthWallBlock(Block block) {
		if (getMaxX() == block.getX()
				&& block.getZ() <= getMaxZ()
				&& block.getZ() >= getMinZ()) {
			return true;	// south wall
		}
		return false;
	}
	
	public boolean isNorthWallBlock(Block block) {
		if (getMinX() == block.getX()
				&& block.getZ() <= getMaxZ()
				&& block.getZ() >= getMinZ()) {
			return true;	// north wall
		}
		return false;
	}
	
	public boolean isWestWallBlock(Block block) {
		if (getMaxZ() == block.getZ()
				&& block.getX() <= getMaxX()
				&& block.getX() >= getMinX()) {
			return true;	// west wall
		}
		return false;
	}
}
