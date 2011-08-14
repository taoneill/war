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

	public ZoneVolume(String name, World world, Warzone zone) {
		super(name, world);
		this.zone = zone;
	}

	@Override
	public int saveBlocks() {
		// Save blocks directly to disk (i.e. don't put everything in memory)
		int saved = ZoneVolumeMapper.save(this, this.zone.getName());
		War.war.log("Saved " + saved + " blocks in warzone " + this.zone.getName() + ".", java.util.logging.Level.INFO);
		this.isSaved = true;
		return saved;
	}

	@Override
	public boolean isSaved() {
		return this.isSaved;
	}

	public void loadCorners() {
		ZoneVolumeMapper.load(this, this.zone.getName(), this.getWorld(), true);
		this.isSaved = true;
	}

	@Override
	public int resetBlocks() {
		// Load blocks directly from disk and onto the map (i.e. no more in-memory warzone blocks)
		int reset = ZoneVolumeMapper.load(this, this.zone.getName(), this.getWorld(), false);
		War.war.log("Reset " + reset + " blocks in warzone " + this.zone.getName() + ".", java.util.logging.Level.INFO);
		this.isSaved = true;
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
		BlockInfo oldCornerOne = this.getCornerOne();
		BlockInfo oldCornerTwo = this.getCornerTwo();
		if (this.getCornerOne() == null) {
			if (this.getCornerTwo() == null) {
				// northwest defaults to corner 1
				super.setCornerOne(topBlock);
			} else if (this.getCornerTwo().getX() <= block.getX() || this.getCornerTwo().getZ() >= block.getZ()) {
				throw new NotNorthwestException();
			} else {
				// corner 2 already set, but we're sure we're located at the northwest of it
				super.setCornerOne(topBlock);
			}
		} else if (this.getCornerTwo() == null) {
			// corner 1 already exists, set northwest as corner 2 (only if it's at the northwest of corner 1)
			if (this.getCornerOne().getX() <= block.getX() || this.getCornerOne().getZ() >= block.getZ()) {
				throw new NotNorthwestException();
			}
			super.setCornerTwo(topBlock);
		} else {
			// both corners already set: we are resizing (only if the new block is northwest relative to the southeasternmost block)
			if (this.getSoutheastX() <= block.getX() || this.getSoutheastZ() >= block.getZ()) {
				throw new NotNorthwestException();
			}
			BlockInfo minXBlock = this.getMinXBlock(); // north means min X
			minXBlock.setX(block.getX()); // mutating, argh!
			BlockInfo maxZBlock = this.getMaxZBlock(); // west means max Z
			maxZBlock.setZ(block.getZ());
		}
		if (this.tooSmall() || this.zoneStructuresAreOutside()) {
			super.setCornerOne(oldCornerOne);
			super.setCornerTwo(oldCornerTwo);
			throw new TooSmallException();
		} else if (this.tooBig()) {
			super.setCornerOne(oldCornerOne);
			super.setCornerTwo(oldCornerTwo);
			throw new TooBigException();
		}
	}

	public int getNorthwestX() {
		if (!this.hasTwoCorners()) {
			return 0;
		} else {
			return this.getMinX();
		}
	}

	public int getNorthwestZ() {
		if (!this.hasTwoCorners()) {
			return 0;
		} else {
			return this.getMaxZ();
		}
	}

	public void setSoutheast(Block block) throws NotSoutheastException, TooSmallException, TooBigException {
		// southeast defaults to bottom block
		BlockInfo bottomBlock = new BlockInfo(block.getX(), 0, block.getZ(), block.getTypeId(), block.getData());
		BlockInfo oldCornerOne = this.getCornerOne();
		BlockInfo oldCornerTwo = this.getCornerTwo();
		if (this.getCornerTwo() == null) {
			if (this.getCornerOne() == null) {
				// southeast defaults to corner 2
				super.setCornerTwo(bottomBlock);
			} else if (this.getCornerOne().getX() >= block.getX() || this.getCornerOne().getZ() <= block.getZ()) {
				throw new NotSoutheastException();
			} else {
				// corner 1 already set, but we're sure we're located at the southeast of it
				super.setCornerTwo(bottomBlock);
			}
		} else if (this.getCornerOne() == null) {
			// corner 2 already exists, set northwest as corner 1 (only if it's at the southeast of corner 2)
			if (this.getCornerTwo().getX() >= block.getX() || this.getCornerTwo().getZ() <= block.getZ()) {
				throw new NotSoutheastException();
			}
			super.setCornerOne(bottomBlock);
		} else {
			// both corners already set: we are resizing (only if the new block is southeast relative to the northwesternmost block)
			if (this.getNorthwestX() >= block.getX() || this.getNorthwestZ() <= block.getZ()) {
				throw new NotSoutheastException();
			}
			BlockInfo maxXBlock = this.getMaxXBlock(); // south means max X
			maxXBlock.setX(block.getX()); // mutating, argh!
			BlockInfo minZBlock = this.getMinZBlock(); // east means min Z
			minZBlock.setZ(block.getZ());
		}
		if (this.tooSmall() || this.zoneStructuresAreOutside()) {
			super.setCornerOne(oldCornerOne);
			super.setCornerTwo(oldCornerTwo);
			throw new TooSmallException();
		} else if (this.tooBig()) {
			super.setCornerOne(oldCornerOne);
			super.setCornerTwo(oldCornerTwo);
			throw new TooBigException();
		}

	}

	public int getSoutheastX() {
		if (!this.hasTwoCorners()) {
			return 0;
		} else {
			return this.getMaxX();
		}
	}

	public int getSoutheastZ() {
		if (!this.hasTwoCorners()) {
			return 0;
		} else {
			return this.getMinZ();
		}
	}

	public int getCenterY() {
		if (!this.hasTwoCorners()) {
			return 0;
		} else {
			return this.getMinY() + (this.getMaxY() - this.getMinY()) / 2;
		}
	}

	public void setZoneCornerOne(Block block) throws TooSmallException, TooBigException {
		BlockInfo oldCornerOne = this.getCornerOne();
		super.setCornerOne(block);
		if (this.tooSmall() || this.zoneStructuresAreOutside()) {
			super.setCornerOne(oldCornerOne);
			throw new TooSmallException();
		} else if (this.tooBig()) {
			super.setCornerOne(oldCornerOne);
			throw new TooBigException();
		}
	}

	public void setZoneCornerTwo(Block block) throws TooSmallException, TooBigException {
		BlockInfo oldCornerTwo = this.getCornerTwo();
		super.setCornerTwo(block);
		if (this.tooSmall() || this.zoneStructuresAreOutside()) {
			super.setCornerTwo(oldCornerTwo);
			throw new TooSmallException();
		} else if (this.tooBig()) {
			super.setCornerTwo(oldCornerTwo);
			throw new TooBigException();
		}
	}

	public boolean tooSmall() {
		if (this.hasTwoCorners() && ((this.getMaxX() - this.getMinX() < 10) || (this.getMaxY() - this.getMinY() < 10) || (this.getMaxZ() - this.getMinZ() < 10))) {
			return true;
		}
		return false;
	}

	public boolean tooBig() {
		if (this.hasTwoCorners() && ((this.getMaxX() - this.getMinX() > 750) || (this.getMaxY() - this.getMinY() > 750) || (this.getMaxZ() - this.getMinZ() > 750))) {
			return true;
		}
		return false;
	}

	public boolean zoneStructuresAreOutside() {
		// check team spawns & flags
		for (Team team : this.zone.getTeams()) {
			if (team.getTeamSpawn() != null) {
				if (!this.isInside(team.getSpawnVolume().getCornerOne()) || !this.isInside(team.getSpawnVolume().getCornerTwo())) {
					return true;
				}
			}
			if (team.getTeamFlag() != null) {
				if (!this.isInside(team.getFlagVolume().getCornerOne()) || !this.isInside(team.getFlagVolume().getCornerTwo())) {
					return true;
				}
			}
		}
		// check monuments
		for (Monument monument : this.zone.getMonuments()) {
			if (monument.getVolume() != null) {
				if (!this.isInside(monument.getVolume().getCornerOne()) || !this.isInside(monument.getVolume().getCornerTwo())) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean isInside(BlockInfo info) {
		if (info.getX() <= this.getMaxX() && info.getX() >= this.getMinX() && info.getY() <= this.getMaxY() && info.getY() >= this.getMinY() && info.getZ() <= this.getMaxZ() && info.getZ() >= this.getMinZ()) {
			return true;
		}
		return false;
	}

	public boolean isWallBlock(Block block) {
		return this.isEastWallBlock(block) || this.isNorthWallBlock(block) || this.isSouthWallBlock(block) || this.isWestWallBlock(block) || this.isUpWallBlock(block) || this.isDownWallBlock(block);
	}

	public boolean isEastWallBlock(Block block) {
		if (this.getMinZ() == block.getZ() && block.getX() <= this.getMaxX() && block.getX() >= this.getMinX() && block.getY() >= this.getMinY() && block.getY() <= this.getMaxY()) {
			return true; // east wall
		}
		return false;
	}

	public boolean isSouthWallBlock(Block block) {
		if (this.getMaxX() == block.getX() && block.getZ() <= this.getMaxZ() && block.getZ() >= this.getMinZ() && block.getY() >= this.getMinY() && block.getY() <= this.getMaxY()) {
			return true; // south wall
		}
		return false;
	}

	public boolean isNorthWallBlock(Block block) {
		if (this.getMinX() == block.getX() && block.getZ() <= this.getMaxZ() && block.getZ() >= this.getMinZ() && block.getY() >= this.getMinY() && block.getY() <= this.getMaxY()) {
			return true; // north wall
		}
		return false;
	}

	public boolean isWestWallBlock(Block block) {
		if (this.getMaxZ() == block.getZ() && block.getX() <= this.getMaxX() && block.getX() >= this.getMinX() && block.getY() >= this.getMinY() && block.getY() <= this.getMaxY()) {
			return true; // west wall
		}
		return false;
	}

	public boolean isUpWallBlock(Block block) {
		if (this.getMaxY() == block.getY() && block.getX() <= this.getMaxX() && block.getX() >= this.getMinX() && block.getZ() >= this.getMinZ() && block.getZ() <= this.getMaxZ()) {
			return true; // top wall
		}
		return false;
	}

	public boolean isDownWallBlock(Block block) {
		if (this.getMinY() == block.getY() && block.getX() <= this.getMaxX() && block.getX() >= this.getMinX() && block.getZ() >= this.getMinZ() && block.getZ() <= this.getMaxZ()) {
			return true; // bottom wall
		}
		return false;
	}
}
