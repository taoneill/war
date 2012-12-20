package com.tommytony.war.structure;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import com.tommytony.war.utility.Direction;
import com.tommytony.war.volume.BlockInfo;


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
				this.glassify(block.getRelative(BlockFace.UP), this.wall);
				this.glassify(block.getRelative(BlockFace.UP, 2), this.wall);
				this.glassify(block.getRelative(BlockFace.DOWN), this.wall);
				this.glassify(block.getRelative(BlockFace.DOWN, 2), this.wall);
			}
			if (this.wall == Direction.NORTH() && this.warzone.getVolume().isNorthWallBlock(block)) {
				this.glassify(block.getRelative(Direction.EAST()), Direction.NORTH());
				this.glassify(block.getRelative(Direction.EAST()).getRelative(BlockFace.UP), Direction.NORTH());
				this.glassify(block.getRelative(Direction.EAST()).getRelative(BlockFace.DOWN), Direction.NORTH());
				this.glassify(block.getRelative(Direction.EAST(), 2), Direction.NORTH());
				this.glassify(block.getRelative(Direction.EAST(), 2).getRelative(BlockFace.UP), Direction.NORTH());
				this.glassify(block.getRelative(Direction.EAST(), 2).getRelative(BlockFace.DOWN), Direction.NORTH());
				this.glassify(block.getRelative(Direction.EAST()).getRelative(BlockFace.UP, 2), Direction.NORTH());
				this.glassify(block.getRelative(Direction.EAST()).getRelative(BlockFace.DOWN, 2), Direction.NORTH());
				this.glassify(block.getRelative(Direction.WEST()), Direction.NORTH());
				this.glassify(block.getRelative(Direction.WEST()).getRelative(BlockFace.UP), Direction.NORTH());
				this.glassify(block.getRelative(Direction.WEST()).getRelative(BlockFace.DOWN), Direction.NORTH());
				this.glassify(block.getRelative(Direction.WEST(), 2), Direction.NORTH());
				this.glassify(block.getRelative(Direction.WEST(), 2).getRelative(BlockFace.UP), Direction.NORTH());
				this.glassify(block.getRelative(Direction.WEST(), 2).getRelative(BlockFace.DOWN), Direction.NORTH());
				this.glassify(block.getRelative(Direction.WEST()).getRelative(BlockFace.UP, 2), Direction.NORTH());
				this.glassify(block.getRelative(Direction.WEST()).getRelative(BlockFace.DOWN, 2), Direction.NORTH());
			} else if (this.wall == Direction.SOUTH() && this.warzone.getVolume().isSouthWallBlock(block)) {
				this.glassify(block.getRelative(Direction.EAST()), Direction.SOUTH());
				this.glassify(block.getRelative(Direction.EAST()).getRelative(BlockFace.UP), Direction.SOUTH());
				this.glassify(block.getRelative(Direction.EAST()).getRelative(BlockFace.DOWN), Direction.SOUTH());
				this.glassify(block.getRelative(Direction.EAST(), 2), Direction.SOUTH());
				this.glassify(block.getRelative(Direction.EAST(), 2).getRelative(BlockFace.UP), Direction.SOUTH());
				this.glassify(block.getRelative(Direction.EAST(), 2).getRelative(BlockFace.DOWN), Direction.SOUTH());
				this.glassify(block.getRelative(Direction.EAST()).getRelative(BlockFace.UP, 2), Direction.SOUTH());
				this.glassify(block.getRelative(Direction.EAST()).getRelative(BlockFace.DOWN, 2), Direction.SOUTH());
				this.glassify(block.getRelative(Direction.WEST()), Direction.SOUTH());
				this.glassify(block.getRelative(Direction.WEST()).getRelative(BlockFace.UP), Direction.SOUTH());
				this.glassify(block.getRelative(Direction.WEST()).getRelative(BlockFace.DOWN), Direction.SOUTH());
				this.glassify(block.getRelative(Direction.WEST(), 2), Direction.SOUTH());
				this.glassify(block.getRelative(Direction.WEST(), 2).getRelative(BlockFace.UP), Direction.SOUTH());
				this.glassify(block.getRelative(Direction.WEST(), 2).getRelative(BlockFace.DOWN), Direction.SOUTH());
				this.glassify(block.getRelative(Direction.WEST()).getRelative(BlockFace.UP, 2), Direction.SOUTH());
				this.glassify(block.getRelative(Direction.WEST()).getRelative(BlockFace.DOWN, 2), Direction.SOUTH());
			} else if (this.wall == Direction.EAST() && this.warzone.getVolume().isEastWallBlock(block)) {
				this.glassify(block.getRelative(Direction.NORTH()), Direction.EAST());
				this.glassify(block.getRelative(Direction.NORTH()).getRelative(BlockFace.UP), Direction.EAST());
				this.glassify(block.getRelative(Direction.NORTH()).getRelative(BlockFace.DOWN), Direction.EAST());
				this.glassify(block.getRelative(Direction.NORTH(), 2), Direction.EAST());
				this.glassify(block.getRelative(Direction.NORTH(), 2).getRelative(BlockFace.UP), Direction.EAST());
				this.glassify(block.getRelative(Direction.NORTH(), 2).getRelative(BlockFace.DOWN), Direction.EAST());
				this.glassify(block.getRelative(Direction.NORTH()).getRelative(BlockFace.UP, 2), Direction.EAST());
				this.glassify(block.getRelative(Direction.NORTH()).getRelative(BlockFace.DOWN, 2), Direction.EAST());
				this.glassify(block.getRelative(Direction.SOUTH()), Direction.EAST());
				this.glassify(block.getRelative(Direction.SOUTH()).getRelative(BlockFace.UP), Direction.EAST());
				this.glassify(block.getRelative(Direction.SOUTH()).getRelative(BlockFace.DOWN), Direction.EAST());
				this.glassify(block.getRelative(Direction.SOUTH(), 2), Direction.EAST());
				this.glassify(block.getRelative(Direction.SOUTH(), 2).getRelative(BlockFace.UP), Direction.EAST());
				this.glassify(block.getRelative(Direction.SOUTH(), 2).getRelative(BlockFace.DOWN), Direction.EAST());
				this.glassify(block.getRelative(Direction.SOUTH()).getRelative(BlockFace.UP, 2), Direction.EAST());
				this.glassify(block.getRelative(Direction.SOUTH()).getRelative(BlockFace.DOWN, 2), Direction.EAST());
			} else if (this.wall == Direction.WEST() && this.warzone.getVolume().isWestWallBlock(block)) {
				this.glassify(block.getRelative(Direction.NORTH()), Direction.WEST());
				this.glassify(block.getRelative(Direction.NORTH()).getRelative(BlockFace.UP), Direction.WEST());
				this.glassify(block.getRelative(Direction.NORTH()).getRelative(BlockFace.DOWN), Direction.WEST());
				this.glassify(block.getRelative(Direction.NORTH(), 2), Direction.WEST());
				this.glassify(block.getRelative(Direction.NORTH(), 2).getRelative(BlockFace.UP), Direction.WEST());
				this.glassify(block.getRelative(Direction.NORTH(), 2).getRelative(BlockFace.DOWN), Direction.WEST());
				this.glassify(block.getRelative(Direction.NORTH()).getRelative(BlockFace.UP, 2), Direction.WEST());
				this.glassify(block.getRelative(Direction.NORTH()).getRelative(BlockFace.DOWN, 2), Direction.WEST());
				this.glassify(block.getRelative(Direction.SOUTH()), Direction.WEST());
				this.glassify(block.getRelative(Direction.SOUTH()).getRelative(BlockFace.UP), Direction.WEST());
				this.glassify(block.getRelative(Direction.SOUTH()).getRelative(BlockFace.DOWN), Direction.WEST());
				this.glassify(block.getRelative(Direction.SOUTH(), 2), Direction.WEST());
				this.glassify(block.getRelative(Direction.SOUTH(), 2).getRelative(BlockFace.UP), Direction.WEST());
				this.glassify(block.getRelative(Direction.SOUTH(), 2).getRelative(BlockFace.DOWN), Direction.WEST());
				this.glassify(block.getRelative(Direction.SOUTH()).getRelative(BlockFace.UP, 2), Direction.WEST());
				this.glassify(block.getRelative(Direction.SOUTH()).getRelative(BlockFace.DOWN, 2), Direction.WEST());
			} else if (this.wall == BlockFace.UP && this.warzone.getVolume().isUpWallBlock(block)) {
				this.glassify(block.getRelative(Direction.EAST()), BlockFace.UP);
				this.glassify(block.getRelative(Direction.EAST(), 2), BlockFace.UP);
				this.glassify(block.getRelative(Direction.WEST()), BlockFace.UP);
				this.glassify(block.getRelative(Direction.WEST(), 2), BlockFace.UP);
				this.glassify(block.getRelative(Direction.NORTH()), BlockFace.UP);
				this.glassify(block.getRelative(Direction.NORTH()).getRelative(Direction.EAST()), BlockFace.UP);
				this.glassify(block.getRelative(Direction.NORTH()).getRelative(Direction.WEST()), BlockFace.UP);
				this.glassify(block.getRelative(Direction.NORTH(), 2), BlockFace.UP);
				this.glassify(block.getRelative(Direction.NORTH(), 2).getRelative(Direction.EAST()), BlockFace.UP);
				this.glassify(block.getRelative(Direction.NORTH(), 2).getRelative(Direction.WEST()), BlockFace.UP);
				this.glassify(block.getRelative(Direction.NORTH()).getRelative(Direction.EAST(), 2), BlockFace.UP);
				this.glassify(block.getRelative(Direction.NORTH()).getRelative(Direction.WEST(), 2), BlockFace.UP);
				this.glassify(block.getRelative(Direction.SOUTH()), BlockFace.UP);
				this.glassify(block.getRelative(Direction.SOUTH()).getRelative(Direction.EAST()), BlockFace.UP);
				this.glassify(block.getRelative(Direction.SOUTH()).getRelative(Direction.WEST()), BlockFace.UP);
				this.glassify(block.getRelative(Direction.SOUTH(), 2), BlockFace.UP);
				this.glassify(block.getRelative(Direction.SOUTH(), 2).getRelative(Direction.EAST()), BlockFace.UP);
				this.glassify(block.getRelative(Direction.SOUTH(), 2).getRelative(Direction.WEST()), BlockFace.UP);
				this.glassify(block.getRelative(Direction.SOUTH()).getRelative(BlockFace.UP, 2), BlockFace.UP);
				this.glassify(block.getRelative(Direction.SOUTH()).getRelative(BlockFace.DOWN, 2), BlockFace.UP);
			} else if (this.wall == BlockFace.DOWN && this.warzone.getVolume().isDownWallBlock(block)) {
				this.glassify(block.getRelative(Direction.EAST()), BlockFace.DOWN);
				this.glassify(block.getRelative(Direction.EAST(), 2), BlockFace.DOWN);
				this.glassify(block.getRelative(Direction.WEST()), BlockFace.DOWN);
				this.glassify(block.getRelative(Direction.WEST(), 2), BlockFace.DOWN);
				this.glassify(block.getRelative(Direction.NORTH()), BlockFace.DOWN);
				this.glassify(block.getRelative(Direction.NORTH()).getRelative(Direction.EAST()), BlockFace.DOWN);
				this.glassify(block.getRelative(Direction.NORTH()).getRelative(Direction.WEST()), BlockFace.DOWN);
				this.glassify(block.getRelative(Direction.NORTH(), 2), BlockFace.DOWN);
				this.glassify(block.getRelative(Direction.NORTH(), 2).getRelative(Direction.EAST()), BlockFace.DOWN);
				this.glassify(block.getRelative(Direction.NORTH(), 2).getRelative(Direction.WEST()), BlockFace.DOWN);
				this.glassify(block.getRelative(Direction.NORTH()).getRelative(Direction.EAST(), 2), BlockFace.DOWN);
				this.glassify(block.getRelative(Direction.NORTH()).getRelative(Direction.WEST(), 2), BlockFace.DOWN);
				this.glassify(block.getRelative(Direction.SOUTH()), BlockFace.DOWN);
				this.glassify(block.getRelative(Direction.SOUTH()).getRelative(Direction.EAST()), BlockFace.DOWN);
				this.glassify(block.getRelative(Direction.SOUTH()).getRelative(Direction.WEST()), BlockFace.DOWN);
				this.glassify(block.getRelative(Direction.SOUTH(), 2), BlockFace.DOWN);
				this.glassify(block.getRelative(Direction.SOUTH(), 2).getRelative(Direction.EAST()), BlockFace.DOWN);
				this.glassify(block.getRelative(Direction.SOUTH(), 2).getRelative(Direction.WEST()), BlockFace.DOWN);
				this.glassify(block.getRelative(Direction.SOUTH()).getRelative(BlockFace.DOWN, 2), BlockFace.DOWN);
				this.glassify(block.getRelative(Direction.SOUTH()).getRelative(BlockFace.DOWN, 2), BlockFace.DOWN);
			}
		}
	}

	private void glassify(Block block, BlockFace wall) {
		// face here means which wall we are working on

		if ((block.getTypeId() == Material.AIR.getId() || block.getTypeId() == Material.WATER.getId()) && (this.warzone.getLobby() == null || (this.warzone.getLobby() != null && !this.warzone.getLobby().blockIsAGateBlock(block, wall)))) {
			if (wall == Direction.NORTH()) {
				if (this.warzone.getVolume().isNorthWallBlock(block)) {
					this.glassified.add(new BlockInfo(block));
					block.setType(Material.GLASS);
				}
			} else if (wall == Direction.SOUTH()) {
				if (this.warzone.getVolume().isSouthWallBlock(block)) {
					this.glassified.add(new BlockInfo(block));
					block.setType(Material.GLASS);
				}
			} else if (wall == Direction.EAST()) {
				if (this.warzone.getVolume().isEastWallBlock(block)) {
					this.glassified.add(new BlockInfo(block));
					block.setType(Material.GLASS);
				}
			} else if (wall == Direction.WEST()) {
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
