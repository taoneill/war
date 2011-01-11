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
}
