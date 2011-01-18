package com.tommytony.war.volumes;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;

import bukkit.tommytony.war.War;

/**
 * 
 * @author tommytony
 *
 */
public class VerticalVolume extends Volume{

	public VerticalVolume(String name, War war, World world) {
		super(name, war, world);

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

	public int resetWallBlocks(BlockFace wall) {
		int noOfResetBlocks = 0;
		try {
			if(hasTwoCorners() && getBlockInfos() != null) {
				if(wall == BlockFace.EAST) {
					int z = getMinZ();
					int k = 0;
					int y = getMinY();
					for(int j = 0; j < getSizeY(); j++) {
						int x = getMinX();
						for(int i = 0; i < getSizeX(); i++) {
							BlockInfo oldBlockInfo = getBlockInfos()[i][j][k];
							if(oldBlockInfo != null) {
								Block currentBlock = getWorld().getBlockAt(x, y, z);
								if(resetBlock(oldBlockInfo, currentBlock)) {
									noOfResetBlocks++;
								}							
							}
							x++;
						}
						y++;
					}
				} else if(wall == BlockFace.WEST) {
					int z = getMaxZ();
					int k = getSizeZ()-1;
					int y = getMinY();
					for(int j = 0; j < getSizeY(); j++) {
						int x = getMinX();
						for(int i = 0; i < getSizeX(); i++) {
							BlockInfo oldBlockInfo = getBlockInfos()[i][j][k];
							if(oldBlockInfo != null) {
								Block currentBlock = getWorld().getBlockAt(x, y, z);
								if(resetBlock(oldBlockInfo, currentBlock)) {
									noOfResetBlocks++;
								}							
							}
							x++;
						}
						y++;
					}
				} else if(wall == BlockFace.NORTH) {
					int x = getMinX();
					int i = 0;
					int y = getMinY();
					for(int j = 0; j < getSizeY(); j++) {
						int z = getMinZ();
						for(int k = 0; k < getSizeZ(); k++) {
							BlockInfo oldBlockInfo = getBlockInfos()[i][j][k];
							if(oldBlockInfo != null) {
								Block currentBlock = getWorld().getBlockAt(x, y, z);
								if(resetBlock(oldBlockInfo, currentBlock)) {
									noOfResetBlocks++;
								}			
							}
							z++;
						}
						y++;
					}
				} else if(wall == BlockFace.SOUTH) {
					int x = getMaxX();
					int i = getSizeX()-1;
					int y = getMinY();
					for(int j = 0; j < getSizeY(); j++) {
						int z = getMinZ();
						for(int k = 0; k < getSizeZ(); k++) {
							BlockInfo oldBlockInfo = getBlockInfos()[i][j][k];
							if(oldBlockInfo != null) {
								Block currentBlock = getWorld().getBlockAt(x, y, z);
								if(resetBlock(oldBlockInfo, currentBlock)) {
									noOfResetBlocks++;
								}							
							}
							z++;
						}
						y++;
					}
				}
			}		
		} catch (Exception e) {
			this.getWar().warn("Failed to reset wall " + wall + " in volume " + getName() + ". " + e.getClass().toString() + " " + e.getMessage());
		}
		return noOfResetBlocks;
	}
	
	private boolean resetBlock(BlockInfo oldBlockInfo, Block currentBlock) {
		if(currentBlock.getTypeId() != oldBlockInfo.getTypeId() ||
				(currentBlock.getTypeId() == oldBlockInfo.getTypeId() && currentBlock.getData() != oldBlockInfo.getData()) ||
				(currentBlock.getTypeId() == oldBlockInfo.getTypeId() && currentBlock.getData() == oldBlockInfo.getData() &&
						(oldBlockInfo.is(Material.SIGN) || oldBlockInfo.is(Material.SIGN_POST))
				)
			) {
				currentBlock.setType(oldBlockInfo.getType());
				currentBlock.setData(oldBlockInfo.getData());
				if(oldBlockInfo.is(Material.SIGN) || oldBlockInfo.is(Material.SIGN_POST)) {
					BlockState state = currentBlock.getState();
					Sign currentSign = (Sign) state;
					currentSign.setLine(0, oldBlockInfo.getSignLines()[0]);
					currentSign.setLine(1, oldBlockInfo.getSignLines()[0]);
					currentSign.setLine(2, oldBlockInfo.getSignLines()[0]);
					currentSign.setLine(3, oldBlockInfo.getSignLines()[0]);
					state.update();
				}
				return true;
			}
		return false;
	}
	
}
