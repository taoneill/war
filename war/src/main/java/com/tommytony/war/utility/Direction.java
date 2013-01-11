package com.tommytony.war.utility;

import org.bukkit.block.BlockFace;

public class Direction {
	
	public final static boolean isLegacy = BlockFace.NORTH.getModX() == -1;
	
	public static BlockFace NORTH() {
		if (!isLegacy) {
			return BlockFace.WEST;
		} else {
			return BlockFace.NORTH;
		}
	}
	
	public static BlockFace EAST() {
		if (!isLegacy) {
			return BlockFace.NORTH;
		} else {
			return BlockFace.EAST;
		}
	}
	
	public static BlockFace SOUTH() {
		if (!isLegacy) {
			return BlockFace.EAST;
		} else {
			return BlockFace.SOUTH;
		}
	}
	
	public static BlockFace WEST() {
		if (!isLegacy) {
			return BlockFace.SOUTH;
		} else {
			return BlockFace.WEST;
		}
	}
	
	public static BlockFace NORTH_EAST() {
		if (!isLegacy) {
			return BlockFace.NORTH_WEST;
		} else {
			return BlockFace.NORTH_EAST;
		}
	}
	
	public static BlockFace NORTH_WEST() {
		if (!isLegacy) {
			return BlockFace.SOUTH_WEST;
		} else {
			return BlockFace.NORTH_WEST;
		}
	}
	
	public static BlockFace SOUTH_EAST() {
		if (!isLegacy) {
			return BlockFace.NORTH_EAST;
		} else {
			return BlockFace.SOUTH_EAST;
		}
	}
	
	public static BlockFace SOUTH_WEST() {
		if (!isLegacy) {
			return BlockFace.NORTH_WEST;
		} else {
			return BlockFace.SOUTH_WEST;
		}
	}
}
