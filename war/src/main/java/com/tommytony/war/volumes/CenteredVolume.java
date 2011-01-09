package com.tommytony.war.volumes;

import org.bukkit.Block;
import org.bukkit.Location;

import com.tommytony.war.War;
import com.tommytony.war.Warzone;

public class CenteredVolume extends Volume {

	private Block center;
	private int sideSize = -1;

	public CenteredVolume(String name, Location center, War war, Warzone warzone) {
		super(name, war, warzone);
		setCenter(warzone.getWorld().getBlockAt(center.getBlockX(), center.getBlockY(), center.getBlockZ()));
	}
	
	public void setCenter(Block block) {
		this.resetBlocks();
		this.center = block;
		if(sideSize != -1) {
			calculateCorners();
		}
	}
	
	private void calculateCorners() {
		int topHalfOfSide = sideSize / 2;
		
		int x = center.getX() + topHalfOfSide;
		int y = center.getY() + topHalfOfSide;
		int z = center.getZ() + topHalfOfSide;
		Block cornerOne = getWarzone().getWorld().getBlockAt(x, y, z);
		setCornerOne(cornerOne);
		
		int bottomHalfOfSide = sideSize - topHalfOfSide;
		x = center.getX() - bottomHalfOfSide;
		y = center.getY() - bottomHalfOfSide;
		z = center.getZ() - bottomHalfOfSide;
		Block cornerTwo = getWarzone().getWorld().getBlockAt(x, y, z);
		setCornerTwo(cornerTwo);
	}

	public void setSideSize(int sideSize) {
		this.resetBlocks();
		this.sideSize = sideSize;
		if(center != null) {
			calculateCorners();
		}
	}

}
