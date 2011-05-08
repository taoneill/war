package com.tommytony.war.volumes;

import org.bukkit.World;
import org.bukkit.block.Block;

import bukkit.tommytony.war.War;

public class ZoneVolume extends Volume {

	public ZoneVolume(String name, War war, World world) {
		super(name, war, world);
		// TODO Auto-generated constructor stub
	}

	public void setNorthwest(Block block) throws NotNorthwestException {
		// northwest defaults to top block
		BlockInfo topBlock = new BlockInfo(block.getX(), 127, block.getZ(), block.getTypeId(), block.getData());
		if(getCornerOne() == null)
		{
			if(getCornerTwo() == null) {
				// northwest defaults to corner 1			
				super.setCornerOne(topBlock);
			} else if (getCornerTwo().getX() <= block.getX() || getCornerTwo().getZ() >= block.getZ()) {
				throw new NotNorthwestException();
			} else {
				// corner 2 already set, but we're sure we're located at the northwest of it
				super.setCornerOne(topBlock);
			}
		} else if (getCornerTwo() == null){
			// corner 1 already exists, set northwest as corner 2 (only if it's at the northwest of corner 1)
			if (getCornerOne().getX() <= block.getX() || getCornerOne().getZ() >= block.getZ()) {
				throw new NotNorthwestException();
			}
			super.setCornerTwo(topBlock);
		} else {
			// both corners already set: we are resizing
			BlockInfo minXBlock = getMinXBlock(); // north means min X
			minXBlock.setX(block.getX());	// mutating, argh!
			BlockInfo maxZBlock = getMaxZBlock(); // west means max Z
			maxZBlock.setZ(block.getZ());
		}
	}
	
	public void setSoutheast(Block block) throws NotSoutheastException {
		// southeast defaults to bottom block
		BlockInfo bottomBlock = new BlockInfo(block.getX(), 0, block.getZ(), block.getTypeId(), block.getData());
		if(getCornerTwo() == null)
		{
			if(getCornerOne() == null) {
				// southeast defaults to corner 2			
				super.setCornerTwo(bottomBlock);
			} else if (getCornerOne().getX() >= block.getX() || getCornerOne().getZ() <= block.getZ()) {
				throw new NotSoutheastException();
			} else {
				// corner 1 already set, but we're sure we're located at the southeast of it
				super.setCornerTwo(bottomBlock);
			}
		}  else if (getCornerOne() == null){
			// corner 1 already exists, set northwest as corner 2 (only if it's at the northwest of corner 1)
			if (getCornerTwo().getX() >= block.getX() || getCornerTwo().getZ() <= block.getZ()) {
				throw new NotSoutheastException();
			}
			super.setCornerOne(bottomBlock);
		} else {
			// both corners already set: we are resizing
			BlockInfo maxXBlock = getMaxXBlock(); // south means max X
			maxXBlock.setX(block.getX());	// mutating, argh!
			BlockInfo minZBlock = getMinZBlock(); // east means min Z
			minZBlock.setZ(block.getZ());
		}
	}
	
	

}
