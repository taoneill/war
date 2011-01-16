package com.tommytony.war;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

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
	private final War war; 
	private BlockFace wall; 
	
	private final int radius = 3;
	
	
	public ZoneWallGuard(Player player, War war, Warzone warzone, BlockFace wall) {
		this.player = player;
		this.war = war;
		this.wall = wall;
		this.playerLocation = player.getLocation();
		this.warzone = warzone;
		this.activate();
	}
	
	private void activate() {
		List<Block> nearestWallBlocks = warzone.getNearestWallBlocks(playerLocation);

		// add wall guard blocks
		for(Block block : nearestWallBlocks) {
			toGlass(block, wall);
			toGlass(block.getFace(BlockFace.UP), wall);
			toGlass(block.getFace(BlockFace.DOWN), wall);
			if(this.wall == BlockFace.NORTH && warzone.getVolume().isNorthWallBlock(block)) {
				toGlass(block.getFace(BlockFace.EAST), BlockFace.NORTH);
				toGlass(block.getFace(BlockFace.EAST).getFace(BlockFace.UP), BlockFace.NORTH);
				toGlass(block.getFace(BlockFace.EAST).getFace(BlockFace.DOWN), BlockFace.NORTH);
				toGlass(block.getFace(BlockFace.WEST), BlockFace.NORTH);
				toGlass(block.getFace(BlockFace.WEST).getFace(BlockFace.UP), BlockFace.NORTH);
				toGlass(block.getFace(BlockFace.WEST).getFace(BlockFace.DOWN), BlockFace.NORTH);
			} else if(this.wall == BlockFace.SOUTH && warzone.getVolume().isSouthWallBlock(block)) {
				toGlass(block.getFace(BlockFace.EAST), BlockFace.SOUTH);
				toGlass(block.getFace(BlockFace.EAST).getFace(BlockFace.UP), BlockFace.SOUTH);
				toGlass(block.getFace(BlockFace.EAST).getFace(BlockFace.DOWN), BlockFace.SOUTH);
				toGlass(block.getFace(BlockFace.WEST), BlockFace.SOUTH);
				toGlass(block.getFace(BlockFace.WEST).getFace(BlockFace.UP), BlockFace.SOUTH);
				toGlass(block.getFace(BlockFace.WEST).getFace(BlockFace.DOWN), BlockFace.SOUTH);
			} else if(this.wall == BlockFace.EAST && warzone.getVolume().isEastWallBlock(block)) {
				toGlass(block.getFace(BlockFace.NORTH), BlockFace.EAST);
				toGlass(block.getFace(BlockFace.NORTH).getFace(BlockFace.UP), BlockFace.EAST);
				toGlass(block.getFace(BlockFace.NORTH).getFace(BlockFace.DOWN), BlockFace.EAST);
				toGlass(block.getFace(BlockFace.SOUTH), BlockFace.WEST);
				toGlass(block.getFace(BlockFace.SOUTH).getFace(BlockFace.UP), BlockFace.WEST);
				toGlass(block.getFace(BlockFace.SOUTH).getFace(BlockFace.DOWN), BlockFace.WEST);
			} else if(this.wall == BlockFace.WEST && warzone.getVolume().isWestWallBlock(block)) {
				toGlass(block.getFace(BlockFace.NORTH), BlockFace.WEST);
				toGlass(block.getFace(BlockFace.NORTH).getFace(BlockFace.UP), BlockFace.WEST);
				toGlass(block.getFace(BlockFace.NORTH).getFace(BlockFace.DOWN), BlockFace.WEST);
				toGlass(block.getFace(BlockFace.SOUTH), BlockFace.WEST);
				toGlass(block.getFace(BlockFace.SOUTH).getFace(BlockFace.UP), BlockFace.WEST);
				toGlass(block.getFace(BlockFace.SOUTH).getFace(BlockFace.DOWN), BlockFace.WEST);
			}
		}
	}
	
	private void toGlass(Block block, BlockFace wall) {
		// face here means which wall we are working on
		if(warzone.getLobby() == null || (warzone.getLobby() != null && !warzone.getLobby().blockIsAGateBlock(block, wall))){
			if(wall == BlockFace.NORTH) {
				if(warzone.getVolume().isNorthWallBlock(block)) {
					block.setType(Material.GLASS);
				}
			} else if (wall == BlockFace.SOUTH) {
				if(warzone.getVolume().isSouthWallBlock(block)) {
					block.setType(Material.GLASS);
				}
			} else if (wall == BlockFace.EAST) {
				if(warzone.getVolume().isEastWallBlock(block)) {
					block.setType(Material.GLASS);
				}
			} else if (wall == BlockFace.WEST) {
				if(warzone.getVolume().isWestWallBlock(block)) {
					block.setType(Material.GLASS);
				}
			}
		}
	}
	
	public void updatePlayerPosition(Location location) {
		if(warzone.isNearWall(location)) {
			this.playerLocation = location;
			activate();
		}
	}

	public Player getPlayer() {
		return player;
	}

	public BlockFace getWall() {
		return wall;
	}
}
