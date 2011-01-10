package com.tommytony.war.volumes;

import org.bukkit.Block;
import org.bukkit.Location;

import com.tommytony.war.War;
import com.tommytony.war.Warzone;

public class CenteredVolume extends Volume {

	private Block center;
	private int sideSize = -1;

	public CenteredVolume(String name, Location center, int sideSize, War war, Warzone warzone) {
		super(name, war, warzone);
		setCenter(warzone.getWorld().getBlockAt(center.getBlockX(), center.getBlockY(), center.getBlockZ()));
		setSideSize(sideSize);
	}
	
	public void changeCenter(Location newCenter) {
		changeCenter(getWarzone().getWorld().getBlockAt(newCenter.getBlockX(), 
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
	
	private void setCenter(Block block) {
		this.center = block;
	}
	
	private void calculateCorners() {
		int topHalfOfSide = sideSize / 2;
		
		int x = center.getX() + topHalfOfSide;
		int y = center.getY() + topHalfOfSide;
		int z = center.getZ() + topHalfOfSide;
		Block cornerOne = getWarzone().getWorld().getBlockAt(x, y, z);
		setCornerOne(cornerOne);
		
		if(sideSize % 2 == 0) {	// not a real center, bottom half is larger by 1
			int bottomHalfOfSide = sideSize - topHalfOfSide;
			x = center.getX() - bottomHalfOfSide;
			y = center.getY() - bottomHalfOfSide;
			z = center.getZ() - bottomHalfOfSide;
			Block cornerTwo = getWarzone().getWorld().getBlockAt(x, y, z);
			setCornerTwo(cornerTwo);
		} else {
			x = center.getX() - topHalfOfSide;
			y = center.getY() - topHalfOfSide;
			z = center.getZ() - topHalfOfSide;
			Block cornerTwo = getWarzone().getWorld().getBlockAt(x, y, z);
			setCornerTwo(cornerTwo);
		}
	}

	private void setSideSize(int sideSize) {
		this.sideSize = sideSize;
	}

}
