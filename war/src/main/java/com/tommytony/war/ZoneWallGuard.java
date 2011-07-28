package com.tommytony.war;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import com.tommytony.war.volumes.BlockInfo;

import bukkit.tommytony.war.War;

/**
 *
 * @author tommytony
 *
 */
public class ZoneWallGuard {
	private Player player;
	private Warzone warzone;
	private Location playerLocation;
	private BlockFace wall;
	private List<BlockInfo> glassified = new ArrayList<BlockInfo>();

	public ZoneWallGuard(Player player, War war, Warzone warzone, BlockFace wall) {
		this.player = player;
		this.wall = wall;
		this.playerLocation = player.getLocation();
		this.warzone = warzone;
		this.activate();
	}

	private void activate() {
		List<Block> nearestWallBlocks = this.warzone.getNearestWallBlocks(this.playerLocation);

		// add wall guard blocks
		for (Block block : nearestWallBlocks) {
			this.glassify(block, this.wall);
			if (this.wall != BlockFace.UP && this.wall != BlockFace.DOWN) {
				this.glassify(block.getFace(BlockFace.UP), this.wall);
				this.glassify(block.getFace(BlockFace.UP, 2), this.wall);
				this.glassify(block.getFace(BlockFace.DOWN), this.wall);
				this.glassify(block.getFace(BlockFace.DOWN, 2), this.wall);
			}
			if (this.wall == BlockFace.NORTH && this.warzone.getVolume().isNorthWallBlock(block)) {
				this.glassify(block.getFace(BlockFace.EAST), BlockFace.NORTH);
				this.glassify(block.getFace(BlockFace.EAST).getFace(BlockFace.UP), BlockFace.NORTH);
				this.glassify(block.getFace(BlockFace.EAST).getFace(BlockFace.DOWN), BlockFace.NORTH);
				this.glassify(block.getFace(BlockFace.EAST, 2), BlockFace.NORTH);
				this.glassify(block.getFace(BlockFace.EAST, 2).getFace(BlockFace.UP), BlockFace.NORTH);
				this.glassify(block.getFace(BlockFace.EAST, 2).getFace(BlockFace.DOWN), BlockFace.NORTH);
				this.glassify(block.getFace(BlockFace.EAST).getFace(BlockFace.UP, 2), BlockFace.NORTH);
				this.glassify(block.getFace(BlockFace.EAST).getFace(BlockFace.DOWN, 2), BlockFace.NORTH);
				this.glassify(block.getFace(BlockFace.WEST), BlockFace.NORTH);
				this.glassify(block.getFace(BlockFace.WEST).getFace(BlockFace.UP), BlockFace.NORTH);
				this.glassify(block.getFace(BlockFace.WEST).getFace(BlockFace.DOWN), BlockFace.NORTH);
				this.glassify(block.getFace(BlockFace.WEST, 2), BlockFace.NORTH);
				this.glassify(block.getFace(BlockFace.WEST, 2).getFace(BlockFace.UP), BlockFace.NORTH);
				this.glassify(block.getFace(BlockFace.WEST, 2).getFace(BlockFace.DOWN), BlockFace.NORTH);
				this.glassify(block.getFace(BlockFace.WEST).getFace(BlockFace.UP, 2), BlockFace.NORTH);
				this.glassify(block.getFace(BlockFace.WEST).getFace(BlockFace.DOWN, 2), BlockFace.NORTH);
			} else if (this.wall == BlockFace.SOUTH && this.warzone.getVolume().isSouthWallBlock(block)) {
				this.glassify(block.getFace(BlockFace.EAST), BlockFace.SOUTH);
				this.glassify(block.getFace(BlockFace.EAST).getFace(BlockFace.UP), BlockFace.SOUTH);
				this.glassify(block.getFace(BlockFace.EAST).getFace(BlockFace.DOWN), BlockFace.SOUTH);
				this.glassify(block.getFace(BlockFace.EAST, 2), BlockFace.SOUTH);
				this.glassify(block.getFace(BlockFace.EAST, 2).getFace(BlockFace.UP), BlockFace.SOUTH);
				this.glassify(block.getFace(BlockFace.EAST, 2).getFace(BlockFace.DOWN), BlockFace.SOUTH);
				this.glassify(block.getFace(BlockFace.EAST).getFace(BlockFace.UP, 2), BlockFace.SOUTH);
				this.glassify(block.getFace(BlockFace.EAST).getFace(BlockFace.DOWN, 2), BlockFace.SOUTH);
				this.glassify(block.getFace(BlockFace.WEST), BlockFace.SOUTH);
				this.glassify(block.getFace(BlockFace.WEST).getFace(BlockFace.UP), BlockFace.SOUTH);
				this.glassify(block.getFace(BlockFace.WEST).getFace(BlockFace.DOWN), BlockFace.SOUTH);
				this.glassify(block.getFace(BlockFace.WEST, 2), BlockFace.SOUTH);
				this.glassify(block.getFace(BlockFace.WEST, 2).getFace(BlockFace.UP), BlockFace.SOUTH);
				this.glassify(block.getFace(BlockFace.WEST, 2).getFace(BlockFace.DOWN), BlockFace.SOUTH);
				this.glassify(block.getFace(BlockFace.WEST).getFace(BlockFace.UP, 2), BlockFace.SOUTH);
				this.glassify(block.getFace(BlockFace.WEST).getFace(BlockFace.DOWN, 2), BlockFace.SOUTH);
			} else if (this.wall == BlockFace.EAST && this.warzone.getVolume().isEastWallBlock(block)) {
				this.glassify(block.getFace(BlockFace.NORTH), BlockFace.EAST);
				this.glassify(block.getFace(BlockFace.NORTH).getFace(BlockFace.UP), BlockFace.EAST);
				this.glassify(block.getFace(BlockFace.NORTH).getFace(BlockFace.DOWN), BlockFace.EAST);
				this.glassify(block.getFace(BlockFace.NORTH, 2), BlockFace.EAST);
				this.glassify(block.getFace(BlockFace.NORTH, 2).getFace(BlockFace.UP), BlockFace.EAST);
				this.glassify(block.getFace(BlockFace.NORTH, 2).getFace(BlockFace.DOWN), BlockFace.EAST);
				this.glassify(block.getFace(BlockFace.NORTH).getFace(BlockFace.UP, 2), BlockFace.EAST);
				this.glassify(block.getFace(BlockFace.NORTH).getFace(BlockFace.DOWN, 2), BlockFace.EAST);
				this.glassify(block.getFace(BlockFace.SOUTH), BlockFace.EAST);
				this.glassify(block.getFace(BlockFace.SOUTH).getFace(BlockFace.UP), BlockFace.EAST);
				this.glassify(block.getFace(BlockFace.SOUTH).getFace(BlockFace.DOWN), BlockFace.EAST);
				this.glassify(block.getFace(BlockFace.SOUTH, 2), BlockFace.EAST);
				this.glassify(block.getFace(BlockFace.SOUTH, 2).getFace(BlockFace.UP), BlockFace.EAST);
				this.glassify(block.getFace(BlockFace.SOUTH, 2).getFace(BlockFace.DOWN), BlockFace.EAST);
				this.glassify(block.getFace(BlockFace.SOUTH).getFace(BlockFace.UP, 2), BlockFace.EAST);
				this.glassify(block.getFace(BlockFace.SOUTH).getFace(BlockFace.DOWN, 2), BlockFace.EAST);
			} else if (this.wall == BlockFace.WEST && this.warzone.getVolume().isWestWallBlock(block)) {
				this.glassify(block.getFace(BlockFace.NORTH), BlockFace.WEST);
				this.glassify(block.getFace(BlockFace.NORTH).getFace(BlockFace.UP), BlockFace.WEST);
				this.glassify(block.getFace(BlockFace.NORTH).getFace(BlockFace.DOWN), BlockFace.WEST);
				this.glassify(block.getFace(BlockFace.NORTH, 2), BlockFace.WEST);
				this.glassify(block.getFace(BlockFace.NORTH, 2).getFace(BlockFace.UP), BlockFace.WEST);
				this.glassify(block.getFace(BlockFace.NORTH, 2).getFace(BlockFace.DOWN), BlockFace.WEST);
				this.glassify(block.getFace(BlockFace.NORTH).getFace(BlockFace.UP, 2), BlockFace.WEST);
				this.glassify(block.getFace(BlockFace.NORTH).getFace(BlockFace.DOWN, 2), BlockFace.WEST);
				this.glassify(block.getFace(BlockFace.SOUTH), BlockFace.WEST);
				this.glassify(block.getFace(BlockFace.SOUTH).getFace(BlockFace.UP), BlockFace.WEST);
				this.glassify(block.getFace(BlockFace.SOUTH).getFace(BlockFace.DOWN), BlockFace.WEST);
				this.glassify(block.getFace(BlockFace.SOUTH, 2), BlockFace.WEST);
				this.glassify(block.getFace(BlockFace.SOUTH, 2).getFace(BlockFace.UP), BlockFace.WEST);
				this.glassify(block.getFace(BlockFace.SOUTH, 2).getFace(BlockFace.DOWN), BlockFace.WEST);
				this.glassify(block.getFace(BlockFace.SOUTH).getFace(BlockFace.UP, 2), BlockFace.WEST);
				this.glassify(block.getFace(BlockFace.SOUTH).getFace(BlockFace.DOWN, 2), BlockFace.WEST);
			} else if (this.wall == BlockFace.UP && this.warzone.getVolume().isUpWallBlock(block)) {
				this.glassify(block.getFace(BlockFace.EAST), BlockFace.UP);
				this.glassify(block.getFace(BlockFace.EAST, 2), BlockFace.UP);
				this.glassify(block.getFace(BlockFace.WEST), BlockFace.UP);
				this.glassify(block.getFace(BlockFace.WEST, 2), BlockFace.UP);
				this.glassify(block.getFace(BlockFace.NORTH), BlockFace.UP);
				this.glassify(block.getFace(BlockFace.NORTH).getFace(BlockFace.EAST), BlockFace.UP);
				this.glassify(block.getFace(BlockFace.NORTH).getFace(BlockFace.WEST), BlockFace.UP);
				this.glassify(block.getFace(BlockFace.NORTH, 2), BlockFace.UP);
				this.glassify(block.getFace(BlockFace.NORTH, 2).getFace(BlockFace.EAST), BlockFace.UP);
				this.glassify(block.getFace(BlockFace.NORTH, 2).getFace(BlockFace.WEST), BlockFace.UP);
				this.glassify(block.getFace(BlockFace.NORTH).getFace(BlockFace.EAST, 2), BlockFace.UP);
				this.glassify(block.getFace(BlockFace.NORTH).getFace(BlockFace.WEST, 2), BlockFace.UP);
				this.glassify(block.getFace(BlockFace.SOUTH), BlockFace.UP);
				this.glassify(block.getFace(BlockFace.SOUTH).getFace(BlockFace.EAST), BlockFace.UP);
				this.glassify(block.getFace(BlockFace.SOUTH).getFace(BlockFace.WEST), BlockFace.UP);
				this.glassify(block.getFace(BlockFace.SOUTH, 2), BlockFace.UP);
				this.glassify(block.getFace(BlockFace.SOUTH, 2).getFace(BlockFace.EAST), BlockFace.UP);
				this.glassify(block.getFace(BlockFace.SOUTH, 2).getFace(BlockFace.WEST), BlockFace.UP);
				this.glassify(block.getFace(BlockFace.SOUTH).getFace(BlockFace.UP, 2), BlockFace.UP);
				this.glassify(block.getFace(BlockFace.SOUTH).getFace(BlockFace.DOWN, 2), BlockFace.UP);
			} else if (this.wall == BlockFace.DOWN && this.warzone.getVolume().isDownWallBlock(block)) {
				this.glassify(block.getFace(BlockFace.EAST), BlockFace.DOWN);
				this.glassify(block.getFace(BlockFace.EAST, 2), BlockFace.DOWN);
				this.glassify(block.getFace(BlockFace.WEST), BlockFace.DOWN);
				this.glassify(block.getFace(BlockFace.WEST, 2), BlockFace.DOWN);
				this.glassify(block.getFace(BlockFace.NORTH), BlockFace.DOWN);
				this.glassify(block.getFace(BlockFace.NORTH).getFace(BlockFace.EAST), BlockFace.DOWN);
				this.glassify(block.getFace(BlockFace.NORTH).getFace(BlockFace.WEST), BlockFace.DOWN);
				this.glassify(block.getFace(BlockFace.NORTH, 2), BlockFace.DOWN);
				this.glassify(block.getFace(BlockFace.NORTH, 2).getFace(BlockFace.EAST), BlockFace.DOWN);
				this.glassify(block.getFace(BlockFace.NORTH, 2).getFace(BlockFace.WEST), BlockFace.DOWN);
				this.glassify(block.getFace(BlockFace.NORTH).getFace(BlockFace.EAST, 2), BlockFace.DOWN);
				this.glassify(block.getFace(BlockFace.NORTH).getFace(BlockFace.WEST, 2), BlockFace.DOWN);
				this.glassify(block.getFace(BlockFace.SOUTH), BlockFace.DOWN);
				this.glassify(block.getFace(BlockFace.SOUTH).getFace(BlockFace.EAST), BlockFace.DOWN);
				this.glassify(block.getFace(BlockFace.SOUTH).getFace(BlockFace.WEST), BlockFace.DOWN);
				this.glassify(block.getFace(BlockFace.SOUTH, 2), BlockFace.DOWN);
				this.glassify(block.getFace(BlockFace.SOUTH, 2).getFace(BlockFace.EAST), BlockFace.DOWN);
				this.glassify(block.getFace(BlockFace.SOUTH, 2).getFace(BlockFace.WEST), BlockFace.DOWN);
				this.glassify(block.getFace(BlockFace.SOUTH).getFace(BlockFace.DOWN, 2), BlockFace.DOWN);
				this.glassify(block.getFace(BlockFace.SOUTH).getFace(BlockFace.DOWN, 2), BlockFace.DOWN);
			}
		}
	}

	private void glassify(Block block, BlockFace wall) {
		// face here means which wall we are working on

		if ((block.getTypeId() == Material.AIR.getId() || block.getTypeId() == Material.WATER.getId()) && (this.warzone.getLobby() == null || (this.warzone.getLobby() != null && !this.warzone.getLobby().blockIsAGateBlock(block, wall)))) {
			if (wall == BlockFace.NORTH) {
				if (this.warzone.getVolume().isNorthWallBlock(block)) {
					this.glassified.add(new BlockInfo(block));
					block.setType(Material.GLASS);
				}
			} else if (wall == BlockFace.SOUTH) {
				if (this.warzone.getVolume().isSouthWallBlock(block)) {
					this.glassified.add(new BlockInfo(block));
					block.setType(Material.GLASS);
				}
			} else if (wall == BlockFace.EAST) {
				if (this.warzone.getVolume().isEastWallBlock(block)) {
					this.glassified.add(new BlockInfo(block));
					block.setType(Material.GLASS);
				}
			} else if (wall == BlockFace.WEST) {
				if (this.warzone.getVolume().isWestWallBlock(block)) {
					this.glassified.add(new BlockInfo(block));
					block.setType(Material.GLASS);
				}
			} else if (wall == BlockFace.UP) {
				if (this.warzone.getVolume().isUpWallBlock(block)) {
					this.glassified.add(new BlockInfo(block));
					block.setType(Material.GLASS);
				}
			} else if (wall == BlockFace.DOWN) {
				if (this.warzone.getVolume().isDownWallBlock(block)) {
					this.glassified.add(new BlockInfo(block));
					block.setType(Material.GLASS);
				}
			}
		}
	}

	public void updatePlayerPosition(Location location) {
		if (this.warzone.isNearWall(location)) {
			this.playerLocation = location;
			this.deactivate();
			this.activate();
		}
	}

	public void deactivate() {
		for (BlockInfo oldBlock : this.glassified) {
			// return to original
			Block glassifiedBlock = this.warzone.getWorld().getBlockAt(oldBlock.getX(), oldBlock.getY(), oldBlock.getZ());
			glassifiedBlock.setTypeId(oldBlock.getTypeId());
			glassifiedBlock.setData(oldBlock.getData());
		}
	}

	public Player getPlayer() {
		return this.player;
	}

	public BlockFace getWall() {
		return this.wall;
	}
}
