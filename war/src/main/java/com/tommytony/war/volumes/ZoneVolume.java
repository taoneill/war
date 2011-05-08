package com.tommytony.war.volumes;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

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
			// corner 2 already exists, set northwest as corner 1 (only if it's at the southeast of corner 2)
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
	
	public boolean isWallBlock(Block block){
		return isEastWallBlock(block) || isNorthWallBlock(block) 
		|| isSouthWallBlock(block) || isWestWallBlock(block) 
		|| isUpWallBlock(block) || isDownWallBlock(block);
	}
	
	public boolean isEastWallBlock(Block block) {
		if(getMinZ() == block.getZ()
				&& block.getX() <= getMaxX()
				&& block.getX() >= getMinX()
				&& block.getY() >= getMinY()
				&& block.getY() <= getMaxY()) {
			return true; 	// east wall
		}
		return false;
	}
	
	public boolean isSouthWallBlock(Block block) {
		if (getMaxX() == block.getX()
				&& block.getZ() <= getMaxZ()
				&& block.getZ() >= getMinZ()
				&& block.getY() >= getMinY()
				&& block.getY() <= getMaxY()) {
			return true;	// south wall
		}
		return false;
	}
	
	public boolean isNorthWallBlock(Block block) {
		if (getMinX() == block.getX()
				&& block.getZ() <= getMaxZ()
				&& block.getZ() >= getMinZ()
				&& block.getY() >= getMinY()
				&& block.getY() <= getMaxY()) {
			return true;	// north wall
		}
		return false;
	}
	
	public boolean isWestWallBlock(Block block) {
		if (getMaxZ() == block.getZ()
				&& block.getX() <= getMaxX()
				&& block.getX() >= getMinX()
				&& block.getY() >= getMinY()
				&& block.getY() <= getMaxY()) {
			return true;	// west wall
		}
		return false;
	}
	
	public boolean isUpWallBlock(Block block) {
		if (getMaxY() == block.getY()
				&& block.getX() <= getMaxX()
				&& block.getX() >= getMinX()
				&& block.getZ() >= getMinZ()
				&& block.getZ() <= getMaxZ()) {
			return true;	// top wall
		}
		return false;
	}
	
	public boolean isDownWallBlock(Block block) {
		if (getMaxY() == block.getY()
				&& block.getX() <= getMaxX()
				&& block.getX() >= getMinX()
				&& block.getZ() >= getMinZ()
				&& block.getZ() <= getMaxZ()) {
			return true;	// bottom wall
		}
		return false;
	}

	public int resetWallBlocks(BlockFace wall) {
		int noOfResetBlocks = 0;
		try {
			if(hasTwoCorners() && getBlockTypes() != null) {
				if(wall == BlockFace.EAST) {
					int z = getMinZ();
					int k = 0;
					int y = getMinY();
					for(int j = 0; j < getSizeY(); j++) {
						int x = getMinX();
						for(int i = 0; i < getSizeX(); i++) {
							int oldBlockType = getBlockTypes()[i][j][k];
							byte oldBlockData = getBlockDatas()[i][j][k];
							Block currentBlock = getWorld().getBlockAt(x, y, z);
							if(resetBlock(oldBlockType, oldBlockData, currentBlock)) {
								noOfResetBlocks++;
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
							int oldBlockType = getBlockTypes()[i][j][k];
							byte oldBlockData = getBlockDatas()[i][j][k];
							Block currentBlock = getWorld().getBlockAt(x, y, z);
							if(resetBlock(oldBlockType, oldBlockData, currentBlock)) {
								noOfResetBlocks++;
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
							int oldBlockType = getBlockTypes()[i][j][k];
							byte oldBlockData = getBlockDatas()[i][j][k];
							Block currentBlock = getWorld().getBlockAt(x, y, z);
							if(resetBlock(oldBlockType, oldBlockData, currentBlock)) {
								noOfResetBlocks++;
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
							int oldBlockType = getBlockTypes()[i][j][k];
							byte oldBlockData = getBlockDatas()[i][j][k];
							Block currentBlock = getWorld().getBlockAt(x, y, z);
							if(resetBlock(oldBlockType, oldBlockData, currentBlock)) {
								noOfResetBlocks++;
							}
							z++;
						}
						y++;
					}
				} else if(wall == BlockFace.UP) {
					int x = getMinX();
					int y = getMaxY();
					int j = getSizeY()-1;
					for(int i = 0;i < getSizeX(); i++) {
						int z = getMinZ();
						for(int k = 0; k < getSizeZ(); k++) {
							int oldBlockType = getBlockTypes()[i][j][k];
							byte oldBlockData = getBlockDatas()[i][j][k];
							Block currentBlock = getWorld().getBlockAt(x, y, z);
							if(resetBlock(oldBlockType, oldBlockData, currentBlock)) {
								noOfResetBlocks++;
							}							
							z++;
						}
						x++;
					}
				} else if(wall == BlockFace.DOWN) {
					int x = getMinX();
					int y = getMinY();
					int j = 0;
					for(int i = 0;i < getSizeX(); i++) {
						int z = getMinZ();
						for(int k = 0; k < getSizeZ(); k++) {
							int oldBlockType = getBlockTypes()[i][j][k];
							byte oldBlockData = getBlockDatas()[i][j][k];
							Block currentBlock = getWorld().getBlockAt(x, y, z);
							if(resetBlock(oldBlockType, oldBlockData, currentBlock)) {
								noOfResetBlocks++;
							}							
							z++;
						}
						x++;
					}
				}
			}		
		} catch (Exception e) {
			this.getWar().logWarn("Failed to reset wall " + wall + " in volume " + getName() + ". " + e.getClass().toString() + " " + e.getMessage());
		}
		return noOfResetBlocks;
	}
	
	private boolean resetBlock(int oldBlockType, byte oldBlockData, Block currentBlock) {
		if(currentBlock.getTypeId() != oldBlockType ||
				(currentBlock.getTypeId() == oldBlockType && currentBlock.getData() != oldBlockData) ||
				(currentBlock.getTypeId() == oldBlockType && currentBlock.getData() == oldBlockData &&
						(oldBlockType == Material.WALL_SIGN.getId() || oldBlockType == Material.SIGN_POST.getId())
				)
			) {
				currentBlock.setTypeId(oldBlockType);
				currentBlock.setData(oldBlockData);
//				if(oldBlockInfo.is(Material.SIGN) || oldBlockInfo.is(Material.SIGN_POST)) {
//					BlockState state = currentBlock.getState();
//					Sign currentSign = (Sign) state;
//					currentSign.setLine(0, oldBlockInfo.getSignLines()[0]);
//					currentSign.setLine(1, oldBlockInfo.getSignLines()[0]);
//					currentSign.setLine(2, oldBlockInfo.getSignLines()[0]);
//					currentSign.setLine(3, oldBlockInfo.getSignLines()[0]);
//					state.update();
//				}
				return true;
			}
		return false;
	}

}
