package com.tommytony.war;

import java.util.List;

import org.bukkit.Block;
import org.bukkit.BlockFace;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Player;

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
			block.setType(Material.GLASS);
			block.getFace(BlockFace.Up).setType(Material.GLASS);
			block.getFace(BlockFace.Down).setType(Material.GLASS);
			if(this.wall == BlockFace.North && warzone.getVolume().isNorthWallBlock(block)) {
				toGlass(block.getFace(BlockFace.East), BlockFace.North);
				toGlass(block.getFace(BlockFace.East).getFace(BlockFace.Up), BlockFace.North);
				toGlass(block.getFace(BlockFace.East).getFace(BlockFace.Down), BlockFace.North);
				toGlass(block.getFace(BlockFace.West), BlockFace.North);
				toGlass(block.getFace(BlockFace.West).getFace(BlockFace.Up), BlockFace.North);
				toGlass(block.getFace(BlockFace.West).getFace(BlockFace.Down), BlockFace.North);
			} else if(this.wall == BlockFace.South && warzone.getVolume().isSouthWallBlock(block)) {
				toGlass(block.getFace(BlockFace.East), BlockFace.South);
				toGlass(block.getFace(BlockFace.East).getFace(BlockFace.Up), BlockFace.South);
				toGlass(block.getFace(BlockFace.East).getFace(BlockFace.Down), BlockFace.South);
				toGlass(block.getFace(BlockFace.West), BlockFace.South);
				toGlass(block.getFace(BlockFace.West).getFace(BlockFace.Up), BlockFace.South);
				toGlass(block.getFace(BlockFace.West).getFace(BlockFace.Down), BlockFace.South);
			} else if(this.wall == BlockFace.East && warzone.getVolume().isEastWallBlock(block)) {
				toGlass(block.getFace(BlockFace.North), BlockFace.East);
				toGlass(block.getFace(BlockFace.North).getFace(BlockFace.Up), BlockFace.East);
				toGlass(block.getFace(BlockFace.North).getFace(BlockFace.Down), BlockFace.East);
				toGlass(block.getFace(BlockFace.South), BlockFace.West);
				toGlass(block.getFace(BlockFace.South).getFace(BlockFace.Up), BlockFace.West);
				toGlass(block.getFace(BlockFace.South).getFace(BlockFace.Down), BlockFace.West);
			} else if(this.wall == BlockFace.West && warzone.getVolume().isWestWallBlock(block)) {
				toGlass(block.getFace(BlockFace.North), BlockFace.West);
				toGlass(block.getFace(BlockFace.North).getFace(BlockFace.Up), BlockFace.West);
				toGlass(block.getFace(BlockFace.North).getFace(BlockFace.Down), BlockFace.West);
				toGlass(block.getFace(BlockFace.South), BlockFace.West);
				toGlass(block.getFace(BlockFace.South).getFace(BlockFace.Up), BlockFace.West);
				toGlass(block.getFace(BlockFace.South).getFace(BlockFace.Down), BlockFace.West);
			}
		}
	}
	
	private void toGlass(Block block, BlockFace wall) {
		// face here means which wall we are working on
		if(warzone.getLobby() == null || (warzone.getLobby() != null && !warzone.getLobby().blockIsAGateBlock(block, wall))){
			if(wall == BlockFace.North) {
				if(warzone.getVolume().isNorthWallBlock(block)) {
					block.setType(Material.GLASS);
				}
			} else if (wall == BlockFace.South) {
				if(warzone.getVolume().isSouthWallBlock(block)) {
					block.setType(Material.GLASS);
				}
			} else if (wall == BlockFace.East) {
				if(warzone.getVolume().isEastWallBlock(block)) {
					block.setType(Material.GLASS);
				}
			} else if (wall == BlockFace.West) {
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
