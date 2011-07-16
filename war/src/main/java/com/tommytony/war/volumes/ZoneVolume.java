package com.tommytony.war.volumes;

import org.bukkit.World;
import org.bukkit.block.Block;

import bukkit.tommytony.war.War;

import com.tommytony.war.Monument;
import com.tommytony.war.Team;
import com.tommytony.war.Warzone;
import com.tommytony.war.mappers.ZoneVolumeMapper;

/**
 * 
 * @author tommytony
 *
 */
public class ZoneVolume extends Volume {

	private Warzone zone;
	private boolean isSaved = false;

	public ZoneVolume(String name, War war, World world, Warzone zone) {
		super(name, war, world);
		this.zone = zone;
	}
	
	@Override
	public int saveBlocks()	{
		// Save blocks directly to disk (i.e. don't put everything in memory)
		int saved = ZoneVolumeMapper.save(this, zone.getName(), this.getWar());
		getWar().logInfo("Saved " + saved + " blocks in warzone " + zone.getName() + ".");
		isSaved = true;
		return saved;
	}
	
	@Override
	public boolean isSaved() {
		return isSaved;
	}
	
	public void loadCorners() {
		ZoneVolumeMapper.load(this, zone.getName(), this.getWar(), this.getWorld(), true);
		isSaved = true;
	}
	
	@Override
	public int resetBlocks()	{
		// Load blocks directly from disk and onto the map (i.e. no more in-memory warzone blocks)
		int reset = ZoneVolumeMapper.load(this, zone.getName(), this.getWar(), this.getWorld(), false);
		getWar().logInfo("Reset " + reset + " blocks in warzone " + zone.getName() + ".");
		isSaved = true;
		return reset;
	}
	
	@Override
	public void setBlockTypes(int[][][] blockTypes) {
		return;
	}
	
	@Override
	public void setBlockDatas(byte[][][] blockData) {
		return;
	}

	public void setNorthwest(Block block) throws NotNorthwestException, TooSmallException, TooBigException {
		// northwest defaults to top block
		BlockInfo topBlock = new BlockInfo(block.getX(), 127, block.getZ(), block.getTypeId(), block.getData());
		BlockInfo oldCornerOne = getCornerOne();
		BlockInfo oldCornerTwo = getCornerTwo();
		if (getCornerOne() == null)
		{
			if (getCornerTwo() == null) {
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
			// both corners already set: we are resizing (only if the new block is northwest relative to the southeasternmost block)
			if (getSoutheastX() <= block.getX() || getSoutheastZ() >= block.getZ()) {
				throw new NotNorthwestException();
			}
			BlockInfo minXBlock = getMinXBlock(); // north means min X
			minXBlock.setX(block.getX());	// mutating, argh!
			BlockInfo maxZBlock = getMaxZBlock(); // west means max Z
			maxZBlock.setZ(block.getZ());
		}
		if (tooSmall() || zoneStructuresAreOutside()) {
			super.setCornerOne(oldCornerOne);
			super.setCornerTwo(oldCornerTwo);
			throw new TooSmallException();
		} else if (tooBig()) {
			super.setCornerOne(oldCornerOne);
			super.setCornerTwo(oldCornerTwo);
			throw new TooBigException();
		}
	}
	
	public int getNorthwestX() {
		if (!hasTwoCorners())
			return 0;
		else
			return getMinX();
	}
	
	public int getNorthwestZ() {
		if (!hasTwoCorners())
			return 0;
		else
			return getMaxZ();
	}
	
	public void setSoutheast(Block block) throws NotSoutheastException, TooSmallException, TooBigException {
		// southeast defaults to bottom block
		BlockInfo bottomBlock = new BlockInfo(block.getX(), 0, block.getZ(), block.getTypeId(), block.getData());
		BlockInfo oldCornerOne = getCornerOne();
		BlockInfo oldCornerTwo = getCornerTwo();
		if (getCornerTwo() == null)
		{
			if (getCornerOne() == null) {
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
			// both corners already set: we are resizing (only if the new block is southeast relative to the northwesternmost block)
			if (getNorthwestX() >= block.getX() || getNorthwestZ() <= block.getZ()) {
				throw new NotSoutheastException();
			}
			BlockInfo maxXBlock = getMaxXBlock(); // south means max X
			maxXBlock.setX(block.getX());	// mutating, argh!
			BlockInfo minZBlock = getMinZBlock(); // east means min Z
			minZBlock.setZ(block.getZ());
		}
		if (tooSmall() || zoneStructuresAreOutside()) {
			super.setCornerOne(oldCornerOne);
			super.setCornerTwo(oldCornerTwo);
			throw new TooSmallException();
		} else if (tooBig()) {
			super.setCornerOne(oldCornerOne);
			super.setCornerTwo(oldCornerTwo);
			throw new TooBigException();
		}
		
	}
	
	public int getSoutheastX() {
		if (!hasTwoCorners())
			return 0;
		else
			return getMaxX();
	}
	
	public int getSoutheastZ() {
		if (!hasTwoCorners())
			return 0;
		else
			return getMinZ();
	}

	public int getCenterY() {
		if (!hasTwoCorners())
			return 0;
		else
			return getMinY() + (getMaxY() - getMinY())/2;
	}
	
	public void setZoneCornerOne(Block block) throws TooSmallException, TooBigException {
		BlockInfo oldCornerOne = getCornerOne();
		super.setCornerOne(block);
		if (tooSmall() || zoneStructuresAreOutside()) {
			super.setCornerOne(oldCornerOne);
			throw new TooSmallException();
		} else if (tooBig()) {
			super.setCornerOne(oldCornerOne);
			throw new TooBigException();
		}
	}
	
	public void setZoneCornerTwo(Block block) throws TooSmallException, TooBigException {
		BlockInfo oldCornerTwo = getCornerTwo();
		super.setCornerTwo(block);
		if (tooSmall() || zoneStructuresAreOutside()) {
			super.setCornerTwo(oldCornerTwo);
			throw new TooSmallException();
		} else if (tooBig()) {
			super.setCornerTwo(oldCornerTwo);
			throw new TooBigException();
		}
	}
	
	public boolean tooSmall() {
		if (hasTwoCorners() && ((getMaxX() - getMinX() < 10)
			|| (getMaxY() - getMinY() < 10)
			|| (getMaxZ() - getMinZ() < 10))) return true;
		return false;
	}
	
	public boolean tooBig() {
		if (hasTwoCorners() && ((getMaxX() - getMinX() > 750)
				|| (getMaxY() - getMinY() > 750)
				|| (getMaxZ() - getMinZ() > 750))) return true;
		return false;
	}
	
	public boolean zoneStructuresAreOutside() {
		// check team spawns & flags
		for (Team team : zone.getTeams()) {
			if (team.getTeamSpawn() != null) {
				if (!isInside(team.getSpawnVolume().getCornerOne())
						|| !isInside(team.getSpawnVolume().getCornerTwo())) {
					return true;
				}
			}
			if (team.getTeamFlag() != null) {
				if (!isInside(team.getFlagVolume().getCornerOne())
						|| !isInside(team.getFlagVolume().getCornerTwo())) {
					return true;
				}
			}
		}
		// check monuments
		for (Monument monument : zone.getMonuments()) {
			if (monument.getVolume() != null) {
				if (!isInside(monument.getVolume().getCornerOne())
						|| !isInside(monument.getVolume().getCornerTwo())) {
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean isInside(BlockInfo info) {
		if (info.getX() <= getMaxX() && info.getX() >= getMinX() && 
				info.getY() <= getMaxY() && info.getY() >= getMinY() &&
				info.getZ() <= getMaxZ() && info.getZ() >= getMinZ())
			return true;
		return false;
	}

	public boolean isWallBlock(Block block){
		return isEastWallBlock(block) || isNorthWallBlock(block) 
		|| isSouthWallBlock(block) || isWestWallBlock(block) 
		|| isUpWallBlock(block) || isDownWallBlock(block);
	}
	
	public boolean isEastWallBlock(Block block) {
		if (getMinZ() == block.getZ()
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
		if (getMinY() == block.getY()
				&& block.getX() <= getMaxX()
				&& block.getX() >= getMinX()
				&& block.getZ() >= getMinZ()
				&& block.getZ() <= getMaxZ()) {
			return true;	// bottom wall
		}
		return false;
	}

	/*public int resetWallBlocks(BlockFace wall) {
		int noOfResetBlocks = 0;
		try {
			if (hasTwoCorners() && getBlockTypes() != null) {
				if (wall == BlockFace.EAST) {
					int z = getMinZ();
					int k = 0;
					int y = getMinY();
					for (int j = 0; j < getSizeY(); j++) {
						int x = getMinX();
						for (int i = 0; i < getSizeX(); i++) {
							int oldBlockType = getBlockTypes()[i][j][k];
							byte oldBlockData = getBlockDatas()[i][j][k];
							Block currentBlock = getWorld().getBlockAt(x, y, z);
							if (resetBlock(oldBlockType, oldBlockData, currentBlock)) {
								noOfResetBlocks++;
							}							
							x++;
						}
						y++;
					}
				} else if (wall == BlockFace.WEST) {
					int z = getMaxZ();
					int k = getSizeZ()-1;
					int y = getMinY();
					for (int j = 0; j < getSizeY(); j++) {
						int x = getMinX();
						for (int i = 0; i < getSizeX(); i++) {
							int oldBlockType = getBlockTypes()[i][j][k];
							byte oldBlockData = getBlockDatas()[i][j][k];
							Block currentBlock = getWorld().getBlockAt(x, y, z);
							if (resetBlock(oldBlockType, oldBlockData, currentBlock)) {
								noOfResetBlocks++;
							}
							x++;
						}
						y++;
					}
				} else if (wall == BlockFace.NORTH) {
					int x = getMinX();
					int i = 0;
					int y = getMinY();
					for (int j = 0; j < getSizeY(); j++) {
						int z = getMinZ();
						for (int k = 0; k < getSizeZ(); k++) {
							int oldBlockType = getBlockTypes()[i][j][k];
							byte oldBlockData = getBlockDatas()[i][j][k];
							Block currentBlock = getWorld().getBlockAt(x, y, z);
							if (resetBlock(oldBlockType, oldBlockData, currentBlock)) {
								noOfResetBlocks++;
							}
							z++;
						}
						y++;
					}
				} else if (wall == BlockFace.SOUTH) {
					int x = getMaxX();
					int i = getSizeX()-1;
					int y = getMinY();
					for (int j = 0; j < getSizeY(); j++) {
						int z = getMinZ();
						for (int k = 0; k < getSizeZ(); k++) {
							int oldBlockType = getBlockTypes()[i][j][k];
							byte oldBlockData = getBlockDatas()[i][j][k];
							Block currentBlock = getWorld().getBlockAt(x, y, z);
							if (resetBlock(oldBlockType, oldBlockData, currentBlock)) {
								noOfResetBlocks++;
							}
							z++;
						}
						y++;
					}
				} else if (wall == BlockFace.UP) {
					int x = getMinX();
					int y = getMaxY();
					int j = getSizeY()-1;
					for (int i = 0;i < getSizeX(); i++) {
						int z = getMinZ();
						for (int k = 0; k < getSizeZ(); k++) {
							int oldBlockType = getBlockTypes()[i][j][k];
							byte oldBlockData = getBlockDatas()[i][j][k];
							Block currentBlock = getWorld().getBlockAt(x, y, z);
							if (resetBlock(oldBlockType, oldBlockData, currentBlock)) {
								noOfResetBlocks++;
							}							
							z++;
						}
						x++;
					}
				} else if (wall == BlockFace.DOWN) {
					int x = getMinX();
					int y = getMinY();
					int j = 0;
					for (int i = 0;i < getSizeX(); i++) {
						int z = getMinZ();
						for (int k = 0; k < getSizeZ(); k++) {
							int oldBlockType = getBlockTypes()[i][j][k];
							byte oldBlockData = getBlockDatas()[i][j][k];
							Block currentBlock = getWorld().getBlockAt(x, y, z);
							if (resetBlock(oldBlockType, oldBlockData, currentBlock)) {
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
		if (currentBlock.getTypeId() != oldBlockType ||
				(currentBlock.getTypeId() == oldBlockType && currentBlock.getData() != oldBlockData) ||
				(currentBlock.getTypeId() == oldBlockType && currentBlock.getData() == oldBlockData &&
						(oldBlockType == Material.WALL_SIGN.getId() || oldBlockType == Material.SIGN_POST.getId())
				)
			) {
				currentBlock.setTypeId(oldBlockType);
				currentBlock.setData(oldBlockData);
				// TODO: reset wall signs, chests and dispensers properly like in resetBlocks
				return true;
			}
		return false;
	}
	*/


}
