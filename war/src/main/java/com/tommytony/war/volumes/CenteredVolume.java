package com.tommytony.war.volumes;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import bukkit.tommytony.war.War;

/**
 * 
 * @author tommytony
 *
 */
public class CenteredVolume extends Volume {

	private Block center;
	private int sideSize = -1;
	private final World world;

	public CenteredVolume(String name, Block center, int sideSize, War war, World world) {
		super(name, war, world);
		this.world = world;
		setCenter(center);
		setSideSize(sideSize);
	}
	
	public void changeCenter(Location newCenter) {
		changeCenter(world.getBlockAt(newCenter.getBlockX(), 
														newCenter.getBlockY(), 
														newCenter.getBlockZ()), 
															this.sideSize);
	}
	
	public void changeCenter(Block newCenter, int sideSize) {
		this.resetBlocks();
		this.center = newCenter;
		this.sideSize = sideSize;
		this.calculateCorners();
	}
	
	public void setCenter(Block block) {
		this.center = block;
	}
	
	public void calculateCorners() {
		int topHalfOfSide = sideSize / 2;
		
		int x = center.getX() + topHalfOfSide;
		int y = center.getY() + topHalfOfSide;
		int z = center.getZ() + topHalfOfSide;
		Block cornerOne = world.getBlockAt(x, y, z);
		setCornerOne(cornerOne);
		
		if(sideSize % 2 == 0) {	// not a real center, bottom half is larger by 1
			int bottomHalfOfSide = sideSize - topHalfOfSide;
			x = center.getX() - bottomHalfOfSide;
			y = center.getY() - bottomHalfOfSide;
			z = center.getZ() - bottomHalfOfSide;
			Block cornerTwo = world.getBlockAt(x, y, z);
			setCornerTwo(cornerTwo);
		} else {
			x = center.getX() - topHalfOfSide;
			y = center.getY() - topHalfOfSide;
			z = center.getZ() - topHalfOfSide;
			Block cornerTwo = world.getBlockAt(x, y, z);
			setCornerTwo(cornerTwo);
		}
	}

	private void setSideSize(int sideSize) {
		this.sideSize = sideSize;
	}

}
