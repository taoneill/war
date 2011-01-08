package com.tommytony.war;

import org.bukkit.Block;
import org.bukkit.World;
import org.bukkit.block.Sign;

public class Volume {
	private final String name;
	private final World world;
	private Block cornerOne;
	private Block cornerTwo;
	private Block[][][] blocks = null;
	private final War war;	

	public Volume(String name, War war, World world) {
		this.name = name;
		this.war = war;
		this.world = world;
	}

	public boolean hasTwoCorners() {
		return cornerOne != null && cornerTwo != null;
	}
	
	public void setCornerOne(Block block) {
		this.cornerOne = block;
	}
	
	public boolean saveBlocks() {
		try {
			if(hasTwoCorners()) {
				this.blocks = new Block[getSizeX()][getSizeY()][getSizeZ()];
				int x = getMinX();
				for(int i = 0; i < getSizeX(); i++){
					int y = getMinY();
					for(int j = 0; j < getSizeY(); j++){
						int z = getMinZ();
						for(int k = 0;k < getSizeZ(); k++) {
							this.blocks[i][j][k] = world.getBlockAt(x, y, z);
							z++;
						}
						y++;
					}
					x++;
				}
				return true;
			}		
			return false;
		} catch (Exception e) {
			this.war.getLogger().warning(war.str("Failed to save volume " + name + " blocks. " + e.getMessage()));
			return false;
		}
	}
	
	public boolean resetBlocks() {
		try {
			if(hasTwoCorners() && blocks != null) {
				int x = getMinX();
				for(int i = 0; i < getSizeX(); i++){
					int y = getMinY();
					for(int j = 0; j < getSizeY(); j++){
						int z = getMinZ();
						for(int k = 0;k < getSizeZ(); k++) {
							Block oldBlock = blocks[i][j][k];
							Block currentBlock = world.getBlockAt(x, y, z);
							currentBlock.setType(oldBlock.getType());
							currentBlock.setData(currentBlock.getData());
							if(oldBlock instanceof Sign) {
								Sign oldSign = (Sign) oldBlock;
								Sign currentSign = (Sign) currentBlock;
								currentSign.setLine(0, oldSign.getLine(0));
								currentSign.setLine(1, oldSign.getLine(1));
								currentSign.setLine(2, oldSign.getLine(2));
								currentSign.setLine(3, oldSign.getLine(3));
							}
							z++;
						}
						y++;
					}
					x++;
				}
				return true;
			}		
			return false;
		} catch (Exception e) {
			this.war.getLogger().warning(war.str("Failed to reset volume " + name + " blocks. " + e.getMessage()));
			return false;
		}
	}

	public void setCornerTwo(Block block) {
		this.cornerTwo = block;
	}
	
	public Block getMinXBlock() {
		if(cornerOne.getX() < cornerTwo.getX()) return cornerOne;
		return cornerTwo;
	}
	
	public Block getMinYBlock() {
		if(cornerOne.getY() < cornerTwo.getY()) return cornerOne;
		return cornerTwo;
	}
	
	public Block getMinZBlock() {
		if(cornerOne.getZ() < cornerTwo.getZ()) return cornerOne;
		return cornerTwo;
	}
	
	public int getMinX() {
		return getMinXBlock().getX();
	}
	
	public int getMinY() {
		return getMinYBlock().getY();
	}
	
	public int getMinZ() {
		return getMinZBlock().getZ();
	}
	
	public Block getMaxXBlock() {
		if(cornerOne.getX() < cornerTwo.getX()) return cornerTwo;
		return cornerOne;
	}
	
	public Block getMaxYBlock() {
		if(cornerOne.getY() < cornerTwo.getY()) return cornerTwo;
		return cornerOne;
	}
	
	public Block getMaxZBlock() {
		if(cornerOne.getZ() < cornerTwo.getZ()) return cornerTwo;
		return cornerOne;
	}
	
	public int getMaxX() {
		return getMaxXBlock().getX();
	}
	
	public int getMaxY() {
		return getMaxYBlock().getY();
	}
	
	public int getMaxZ() {
		return getMaxZBlock().getZ();
	}
	
	public int getSizeX() {
		return getMaxX() - getMinX();
	}
	
	public int getSizeY() {
		return getMaxY() - getMinY();
	}
	
	public int getSizeZ() {
		return getMaxZ() - getMinZ();
	}
}
