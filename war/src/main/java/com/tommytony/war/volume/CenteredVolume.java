package com.tommytony.war.volume;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 *
 * @author tommytony
 * @deprecated Broken, don't use.
 */
@Deprecated
public class CenteredVolume extends Volume {

	private Block center;
	private int sideSize = -1;
	private final World world;

	public CenteredVolume(String name, Block center, int sideSize, World world) {
		super(name, world);
		this.world = world;
		this.setCenter(center);
		this.setSideSize(sideSize);
	}

	public void changeCenter(Location newCenter) {
		this.changeCenter(this.world.getBlockAt(newCenter.getBlockX(), newCenter.getBlockY(), newCenter.getBlockZ()), this.sideSize);
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
		int topHalfOfSide = this.sideSize / 2;

		int x = this.center.getX() + topHalfOfSide;
		int y = this.center.getY() + topHalfOfSide;
		int z = this.center.getZ() + topHalfOfSide;
		Block cornerOne = this.world.getBlockAt(x, y, z);
		this.setCornerOne(cornerOne);

		if (this.sideSize % 2 == 0) { // not a real center, bottom half is larger by 1
			int bottomHalfOfSide = this.sideSize - topHalfOfSide;
			x = this.center.getX() - bottomHalfOfSide;
			y = this.center.getY() - bottomHalfOfSide;
			z = this.center.getZ() - bottomHalfOfSide;
			Block cornerTwo = this.world.getBlockAt(x, y, z);
			this.setCornerTwo(cornerTwo);
		} else {
			x = this.center.getX() - topHalfOfSide;
			y = this.center.getY() - topHalfOfSide;
			z = this.center.getZ() - topHalfOfSide;
			Block cornerTwo = this.world.getBlockAt(x, y, z);
			this.setCornerTwo(cornerTwo);
		}
	}

	private void setSideSize(int sideSize) {
		this.sideSize = sideSize;
	}

}
