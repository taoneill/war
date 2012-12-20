package com.tommytony.war.volume;

import java.util.logging.Level;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import com.tommytony.war.War;
import com.tommytony.war.utility.Direction;


/**
 *
 * @author tommytony
 *
 */
@Deprecated
public class VerticalVolume extends Volume {

	public VerticalVolume(String name, World world) {
		super(name, world);

	}

	@Override
	public void setCornerOne(Block block) {
		// corner one defaults to topmost corner
		Block topBlock = this.getWorld().getBlockAt(block.getX(), 127, block.getZ());
		super.setCornerOne(topBlock);
	}

	@Override
	public void setCornerTwo(Block block) {
		// corner two defaults to bottom most corner
		Block bottomBlock = this.getWorld().getBlockAt(block.getX(), 0, block.getZ());
		super.setCornerTwo(bottomBlock);
	}

	public boolean isWallBlock(Block block) {
		return this.isEastWallBlock(block) || this.isNorthWallBlock(block) || this.isSouthWallBlock(block) || this.isWestWallBlock(block);
	}

	public boolean isEastWallBlock(Block block) {
		if (this.getMinZ() == block.getZ() && block.getX() <= this.getMaxX() && block.getX() >= this.getMinX()) {
			return true; // east wall
		}
		return false;
	}

	public boolean isSouthWallBlock(Block block) {
		if (this.getMaxX() == block.getX() && block.getZ() <= this.getMaxZ() && block.getZ() >= this.getMinZ()) {
			return true; // south wall
		}
		return false;
	}

	public boolean isNorthWallBlock(Block block) {
		if (this.getMinX() == block.getX() && block.getZ() <= this.getMaxZ() && block.getZ() >= this.getMinZ()) {
			return true; // north wall
		}
		return false;
	}

	public boolean isWestWallBlock(Block block) {
		if (this.getMaxZ() == block.getZ() && block.getX() <= this.getMaxX() && block.getX() >= this.getMinX()) {
			return true; // west wall
		}
		return false;
	}

	public int resetWallBlocks(BlockFace wall) {
		int noOfResetBlocks = 0;
		try {
			if (this.hasTwoCorners() && this.getBlockTypes() != null) {
				if (wall == Direction.EAST()) {
					int z = this.getMinZ();
					int k = 0;
					int y = this.getMinY();
					for (int j = 0; j < this.getSizeY(); j++) {
						int x = this.getMinX();
						for (int i = 0; i < this.getSizeX(); i++) {
							int oldBlockType = this.getBlockTypes()[i][j][k];
							byte oldBlockData = this.getBlockDatas()[i][j][k];
							Block currentBlock = this.getWorld().getBlockAt(x, y, z);
							if (this.resetBlock(oldBlockType, oldBlockData, currentBlock)) {
								noOfResetBlocks++;
							}
							x++;
						}
						y++;
					}
				} else if (wall == Direction.WEST()) {
					int z = this.getMaxZ();
					int k = this.getSizeZ() - 1;
					int y = this.getMinY();
					for (int j = 0; j < this.getSizeY(); j++) {
						int x = this.getMinX();
						for (int i = 0; i < this.getSizeX(); i++) {
							int oldBlockType = this.getBlockTypes()[i][j][k];
							byte oldBlockData = this.getBlockDatas()[i][j][k];
							Block currentBlock = this.getWorld().getBlockAt(x, y, z);
							if (this.resetBlock(oldBlockType, oldBlockData, currentBlock)) {
								noOfResetBlocks++;
							}
							x++;
						}
						y++;
					}
				} else if (wall == Direction.NORTH()) {
					int x = this.getMinX();
					int i = 0;
					int y = this.getMinY();
					for (int j = 0; j < this.getSizeY(); j++) {
						int z = this.getMinZ();
						for (int k = 0; k < this.getSizeZ(); k++) {
							int oldBlockType = this.getBlockTypes()[i][j][k];
							byte oldBlockData = this.getBlockDatas()[i][j][k];
							Block currentBlock = this.getWorld().getBlockAt(x, y, z);
							if (this.resetBlock(oldBlockType, oldBlockData, currentBlock)) {
								noOfResetBlocks++;
							}
							z++;
						}
						y++;
					}
				} else if (wall == Direction.SOUTH()) {
					int x = this.getMaxX();
					int i = this.getSizeX() - 1;
					int y = this.getMinY();
					for (int j = 0; j < this.getSizeY(); j++) {
						int z = this.getMinZ();
						for (int k = 0; k < this.getSizeZ(); k++) {
							int oldBlockType = this.getBlockTypes()[i][j][k];
							byte oldBlockData = this.getBlockDatas()[i][j][k];
							Block currentBlock = this.getWorld().getBlockAt(x, y, z);
							if (this.resetBlock(oldBlockType, oldBlockData, currentBlock)) {
								noOfResetBlocks++;
							}
							z++;
						}
						y++;
					}
				}
			}
		} catch (Exception e) {
			War.war.log("Failed to reset wall " + wall + " in volume " + this.getName() + ". " + e.getClass().toString() + " " + e.getMessage(), Level.WARNING);
		}
		return noOfResetBlocks;
	}

	private boolean resetBlock(int oldBlockType, byte oldBlockData, Block currentBlock) {
		if (currentBlock.getTypeId() != oldBlockType || (currentBlock.getTypeId() == oldBlockType && currentBlock.getData() != oldBlockData) || (currentBlock.getTypeId() == oldBlockType && currentBlock.getData() == oldBlockData && (oldBlockType == Material.WALL_SIGN.getId() || oldBlockType == Material.SIGN_POST.getId()))) {
			currentBlock.setTypeId(oldBlockType);
			currentBlock.setData(oldBlockData);
			// if (oldBlockInfo.is(Material.SIGN) || oldBlockInfo.is(Material.SIGN_POST)) {
			// BlockState state = currentBlock.getState();
			// Sign currentSign = (Sign) state;
			// currentSign.setLine(0, oldBlockInfo.getSignLines()[0]);
			// currentSign.setLine(1, oldBlockInfo.getSignLines()[0]);
			// currentSign.setLine(2, oldBlockInfo.getSignLines()[0]);
			// currentSign.setLine(3, oldBlockInfo.getSignLines()[0]);
			// state.update();
			// }
			return true;
		}
		return false;
	}

}
