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
		List<Block> nearestWallBlocks = warzone.getNearestWallBlocks(playerLocation);

		// add wall guard blocks
		for(Block block : nearestWallBlocks) {
			glassify(block, wall);
			if(this.wall != BlockFace.UP && this.wall != BlockFace.DOWN) {
				glassify(block.getFace(BlockFace.UP), wall);
				glassify(block.getFace(BlockFace.UP, 2), wall);
				glassify(block.getFace(BlockFace.DOWN), wall);
				glassify(block.getFace(BlockFace.DOWN, 2), wall);
			}
			if(this.wall == BlockFace.NORTH && warzone.getVolume().isNorthWallBlock(block)) {
				glassify(block.getFace(BlockFace.EAST), BlockFace.NORTH);
				glassify(block.getFace(BlockFace.EAST).getFace(BlockFace.UP), BlockFace.NORTH);
				glassify(block.getFace(BlockFace.EAST).getFace(BlockFace.DOWN), BlockFace.NORTH);
				glassify(block.getFace(BlockFace.EAST, 2), BlockFace.NORTH);
				glassify(block.getFace(BlockFace.EAST, 2).getFace(BlockFace.UP), BlockFace.NORTH);
				glassify(block.getFace(BlockFace.EAST, 2).getFace(BlockFace.DOWN), BlockFace.NORTH);
				glassify(block.getFace(BlockFace.EAST).getFace(BlockFace.UP, 2), BlockFace.NORTH);
				glassify(block.getFace(BlockFace.EAST).getFace(BlockFace.DOWN, 2), BlockFace.NORTH);
				glassify(block.getFace(BlockFace.WEST), BlockFace.NORTH);
				glassify(block.getFace(BlockFace.WEST).getFace(BlockFace.UP), BlockFace.NORTH);
				glassify(block.getFace(BlockFace.WEST).getFace(BlockFace.DOWN), BlockFace.NORTH);
				glassify(block.getFace(BlockFace.WEST, 2), BlockFace.NORTH);
				glassify(block.getFace(BlockFace.WEST, 2).getFace(BlockFace.UP), BlockFace.NORTH);
				glassify(block.getFace(BlockFace.WEST, 2).getFace(BlockFace.DOWN), BlockFace.NORTH);
				glassify(block.getFace(BlockFace.WEST).getFace(BlockFace.UP, 2), BlockFace.NORTH);
				glassify(block.getFace(BlockFace.WEST).getFace(BlockFace.DOWN, 2), BlockFace.NORTH);
			} else if(this.wall == BlockFace.SOUTH && warzone.getVolume().isSouthWallBlock(block)) {
				glassify(block.getFace(BlockFace.EAST), BlockFace.SOUTH);
				glassify(block.getFace(BlockFace.EAST).getFace(BlockFace.UP), BlockFace.SOUTH);
				glassify(block.getFace(BlockFace.EAST).getFace(BlockFace.DOWN), BlockFace.SOUTH);
				glassify(block.getFace(BlockFace.EAST, 2), BlockFace.SOUTH);
				glassify(block.getFace(BlockFace.EAST, 2).getFace(BlockFace.UP), BlockFace.SOUTH);
				glassify(block.getFace(BlockFace.EAST, 2).getFace(BlockFace.DOWN), BlockFace.SOUTH);
				glassify(block.getFace(BlockFace.EAST).getFace(BlockFace.UP, 2), BlockFace.SOUTH);
				glassify(block.getFace(BlockFace.EAST).getFace(BlockFace.DOWN, 2), BlockFace.SOUTH);
				glassify(block.getFace(BlockFace.WEST), BlockFace.SOUTH);
				glassify(block.getFace(BlockFace.WEST).getFace(BlockFace.UP), BlockFace.SOUTH);
				glassify(block.getFace(BlockFace.WEST).getFace(BlockFace.DOWN), BlockFace.SOUTH);
				glassify(block.getFace(BlockFace.WEST, 2), BlockFace.SOUTH);
				glassify(block.getFace(BlockFace.WEST, 2).getFace(BlockFace.UP), BlockFace.SOUTH);
				glassify(block.getFace(BlockFace.WEST, 2).getFace(BlockFace.DOWN), BlockFace.SOUTH);
				glassify(block.getFace(BlockFace.WEST).getFace(BlockFace.UP, 2), BlockFace.SOUTH);
				glassify(block.getFace(BlockFace.WEST).getFace(BlockFace.DOWN, 2), BlockFace.SOUTH);
			} else if(this.wall == BlockFace.EAST && warzone.getVolume().isEastWallBlock(block)) {
				glassify(block.getFace(BlockFace.NORTH), BlockFace.EAST);
				glassify(block.getFace(BlockFace.NORTH).getFace(BlockFace.UP), BlockFace.EAST);
				glassify(block.getFace(BlockFace.NORTH).getFace(BlockFace.DOWN), BlockFace.EAST);
				glassify(block.getFace(BlockFace.NORTH, 2), BlockFace.EAST);
				glassify(block.getFace(BlockFace.NORTH, 2).getFace(BlockFace.UP), BlockFace.EAST);
				glassify(block.getFace(BlockFace.NORTH, 2).getFace(BlockFace.DOWN), BlockFace.EAST);
				glassify(block.getFace(BlockFace.NORTH).getFace(BlockFace.UP, 2), BlockFace.EAST);
				glassify(block.getFace(BlockFace.NORTH).getFace(BlockFace.DOWN, 2), BlockFace.EAST);
				glassify(block.getFace(BlockFace.SOUTH), BlockFace.EAST);
				glassify(block.getFace(BlockFace.SOUTH).getFace(BlockFace.UP), BlockFace.EAST);
				glassify(block.getFace(BlockFace.SOUTH).getFace(BlockFace.DOWN), BlockFace.EAST);
				glassify(block.getFace(BlockFace.SOUTH, 2), BlockFace.EAST);
				glassify(block.getFace(BlockFace.SOUTH, 2).getFace(BlockFace.UP), BlockFace.EAST);
				glassify(block.getFace(BlockFace.SOUTH, 2).getFace(BlockFace.DOWN), BlockFace.EAST);
				glassify(block.getFace(BlockFace.SOUTH).getFace(BlockFace.UP, 2), BlockFace.EAST);
				glassify(block.getFace(BlockFace.SOUTH).getFace(BlockFace.DOWN, 2), BlockFace.EAST);
			} else if(this.wall == BlockFace.WEST && warzone.getVolume().isWestWallBlock(block)) {
				glassify(block.getFace(BlockFace.NORTH), BlockFace.WEST);
				glassify(block.getFace(BlockFace.NORTH).getFace(BlockFace.UP), BlockFace.WEST);
				glassify(block.getFace(BlockFace.NORTH).getFace(BlockFace.DOWN), BlockFace.WEST);
				glassify(block.getFace(BlockFace.NORTH, 2), BlockFace.WEST);
				glassify(block.getFace(BlockFace.NORTH, 2).getFace(BlockFace.UP), BlockFace.WEST);
				glassify(block.getFace(BlockFace.NORTH, 2).getFace(BlockFace.DOWN), BlockFace.WEST);
				glassify(block.getFace(BlockFace.NORTH).getFace(BlockFace.UP, 2), BlockFace.WEST);
				glassify(block.getFace(BlockFace.NORTH).getFace(BlockFace.DOWN, 2), BlockFace.WEST);
				glassify(block.getFace(BlockFace.SOUTH), BlockFace.WEST);
				glassify(block.getFace(BlockFace.SOUTH).getFace(BlockFace.UP), BlockFace.WEST);
				glassify(block.getFace(BlockFace.SOUTH).getFace(BlockFace.DOWN), BlockFace.WEST);
				glassify(block.getFace(BlockFace.SOUTH, 2), BlockFace.WEST);
				glassify(block.getFace(BlockFace.SOUTH, 2).getFace(BlockFace.UP), BlockFace.WEST);
				glassify(block.getFace(BlockFace.SOUTH, 2).getFace(BlockFace.DOWN), BlockFace.WEST);
				glassify(block.getFace(BlockFace.SOUTH).getFace(BlockFace.UP, 2), BlockFace.WEST);
				glassify(block.getFace(BlockFace.SOUTH).getFace(BlockFace.DOWN, 2), BlockFace.WEST);
			} else if(this.wall == BlockFace.UP && warzone.getVolume().isUpWallBlock(block)) {
				glassify(block.getFace(BlockFace.EAST), BlockFace.UP);
				glassify(block.getFace(BlockFace.EAST, 2), BlockFace.UP);
				glassify(block.getFace(BlockFace.WEST), BlockFace.UP);
				glassify(block.getFace(BlockFace.WEST, 2), BlockFace.UP);
				glassify(block.getFace(BlockFace.NORTH), BlockFace.UP);
				glassify(block.getFace(BlockFace.NORTH).getFace(BlockFace.EAST), BlockFace.UP);
				glassify(block.getFace(BlockFace.NORTH).getFace(BlockFace.WEST), BlockFace.UP);
				glassify(block.getFace(BlockFace.NORTH, 2), BlockFace.UP);
				glassify(block.getFace(BlockFace.NORTH, 2).getFace(BlockFace.EAST), BlockFace.UP);
				glassify(block.getFace(BlockFace.NORTH, 2).getFace(BlockFace.WEST), BlockFace.UP);
				glassify(block.getFace(BlockFace.NORTH).getFace(BlockFace.EAST, 2), BlockFace.UP);
				glassify(block.getFace(BlockFace.NORTH).getFace(BlockFace.WEST, 2), BlockFace.UP);
				glassify(block.getFace(BlockFace.SOUTH), BlockFace.UP);
				glassify(block.getFace(BlockFace.SOUTH).getFace(BlockFace.EAST), BlockFace.UP);
				glassify(block.getFace(BlockFace.SOUTH).getFace(BlockFace.WEST), BlockFace.UP);
				glassify(block.getFace(BlockFace.SOUTH, 2), BlockFace.UP);
				glassify(block.getFace(BlockFace.SOUTH, 2).getFace(BlockFace.EAST), BlockFace.UP);
				glassify(block.getFace(BlockFace.SOUTH, 2).getFace(BlockFace.WEST), BlockFace.UP);
				glassify(block.getFace(BlockFace.SOUTH).getFace(BlockFace.UP, 2), BlockFace.UP);
				glassify(block.getFace(BlockFace.SOUTH).getFace(BlockFace.DOWN, 2), BlockFace.UP);
			} else if (this.wall == BlockFace.DOWN && warzone.getVolume().isDownWallBlock(block)) {
				glassify(block.getFace(BlockFace.EAST), BlockFace.DOWN);
				glassify(block.getFace(BlockFace.EAST, 2), BlockFace.DOWN);
				glassify(block.getFace(BlockFace.WEST), BlockFace.DOWN);
				glassify(block.getFace(BlockFace.WEST, 2), BlockFace.DOWN);
				glassify(block.getFace(BlockFace.NORTH), BlockFace.DOWN);
				glassify(block.getFace(BlockFace.NORTH).getFace(BlockFace.EAST), BlockFace.DOWN);
				glassify(block.getFace(BlockFace.NORTH).getFace(BlockFace.WEST), BlockFace.DOWN);
				glassify(block.getFace(BlockFace.NORTH, 2), BlockFace.DOWN);
				glassify(block.getFace(BlockFace.NORTH, 2).getFace(BlockFace.EAST), BlockFace.DOWN);
				glassify(block.getFace(BlockFace.NORTH, 2).getFace(BlockFace.WEST), BlockFace.DOWN);
				glassify(block.getFace(BlockFace.NORTH).getFace(BlockFace.EAST, 2), BlockFace.DOWN);
				glassify(block.getFace(BlockFace.NORTH).getFace(BlockFace.WEST, 2), BlockFace.DOWN);
				glassify(block.getFace(BlockFace.SOUTH), BlockFace.DOWN);
				glassify(block.getFace(BlockFace.SOUTH).getFace(BlockFace.EAST), BlockFace.DOWN);
				glassify(block.getFace(BlockFace.SOUTH).getFace(BlockFace.WEST), BlockFace.DOWN);
				glassify(block.getFace(BlockFace.SOUTH, 2), BlockFace.DOWN);
				glassify(block.getFace(BlockFace.SOUTH, 2).getFace(BlockFace.EAST), BlockFace.DOWN);
				glassify(block.getFace(BlockFace.SOUTH, 2).getFace(BlockFace.WEST), BlockFace.DOWN);
				glassify(block.getFace(BlockFace.SOUTH).getFace(BlockFace.DOWN, 2), BlockFace.DOWN);
				glassify(block.getFace(BlockFace.SOUTH).getFace(BlockFace.DOWN, 2), BlockFace.DOWN);
			}
		}
	}
	
	private void glassify(Block block, BlockFace wall) {
		// face here means which wall we are working on

		if((block.getTypeId() == Material.AIR.getId() || block.getTypeId() == Material.WATER.getId()) &&
				(warzone.getLobby() == null || (warzone.getLobby() != null && !warzone.getLobby().blockIsAGateBlock(block, wall)))){
			if(wall == BlockFace.NORTH) {
				if(warzone.getVolume().isNorthWallBlock(block)) {
					glassified.add(new BlockInfo(block));
					block.setType(Material.GLASS);
				}
			} else if (wall == BlockFace.SOUTH) {
				if(warzone.getVolume().isSouthWallBlock(block)) {
					glassified.add(new BlockInfo(block));
					block.setType(Material.GLASS);
				}
			} else if (wall == BlockFace.EAST) {
				if(warzone.getVolume().isEastWallBlock(block)) {
					glassified.add(new BlockInfo(block));
					block.setType(Material.GLASS);
				}
			} else if (wall == BlockFace.WEST) {
				if(warzone.getVolume().isWestWallBlock(block)) {
					glassified.add(new BlockInfo(block));
					block.setType(Material.GLASS);
				}
			} else if (wall == BlockFace.UP) {
				if(warzone.getVolume().isUpWallBlock(block)) {
					glassified.add(new BlockInfo(block));
					block.setType(Material.GLASS);
				}
			} else if (wall == BlockFace.DOWN) {
				if(warzone.getVolume().isDownWallBlock(block)) {
					glassified.add(new BlockInfo(block));
					block.setType(Material.GLASS);
				}
			}
		}
	}
	
	public void updatePlayerPosition(Location location) {
		if(warzone.isNearWall(location)) {
			this.playerLocation = location;
			deactivate();
			activate();
		}
	}

	public void deactivate() {
		for(BlockInfo oldBlock : glassified) {
			// return to original
			Block glassifiedBlock = warzone.getWorld().getBlockAt(oldBlock.getX(), oldBlock.getY(), oldBlock.getZ());
			glassifiedBlock.setTypeId(oldBlock.getTypeId());
			glassifiedBlock.setData(oldBlock.getData());
		}
	}

	public Player getPlayer() {
		return player;
	}

	public BlockFace getWall() {
		return wall;
	}
}
