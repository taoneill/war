package com.tommytony.war;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import bukkit.tommytony.war.War;

import com.tommytony.war.mappers.VolumeMapper;
import com.tommytony.war.volumes.VerticalVolume;
import com.tommytony.war.volumes.Volume;

/**
 * 
 * @author tommytony
 *
 */
public class ZoneLobby {
	private final War war;
	private final Warzone warzone;
	private BlockFace wall;
	private Volume volume;
	Block lobbyMiddleWallBlock = null;	// on the zone wall, one above the zone lobby floor
	
	Block warHubLinkGate = null;
	
	Block diamondGate = null;
	Block ironGate = null;
	Block goldGate = null;
	Block autoAssignGate = null;
	
	Block zoneTeleportBlock = null;
	
	private final int lobbyHeight = 3;
	private final int lobbyHalfSide = 7;
	private final int lobbyDepth = 10;
	
	public ZoneLobby(War war, Warzone warzone, BlockFace wall) {
		this.war = war;
		this.warzone = warzone;
		this.changeWall(wall);
	}
	
	/**
	 * Convenience ctor when loading form disk.
	 * This figures out the middle wall block of the lobby from the volume instead 
	 * of the other way around.
	 */
	public ZoneLobby(War war, Warzone warzone, BlockFace wall, Volume volume) {
		this.war = war;
		this.warzone = warzone;
		Volume zoneVolume = warzone.getVolume();
		this.wall = wall;
		this.setVolume(volume);
		// we're setting the zoneVolume directly, so we need to figure out the lobbyMiddleWallBlock on our own
		if(wall == BlockFace.NORTH) {
			lobbyMiddleWallBlock = volume.getCornerOne().getFace(BlockFace.UP).getFace(BlockFace.EAST, lobbyHalfSide); 
		} else if (wall == BlockFace.EAST){
			lobbyMiddleWallBlock = volume.getCornerOne().getFace(BlockFace.UP).getFace(BlockFace.SOUTH, lobbyHalfSide);
 		} else if (wall == BlockFace.SOUTH){
 			lobbyMiddleWallBlock = volume.getCornerOne().getFace(BlockFace.UP).getFace(BlockFace.WEST, lobbyHalfSide);
		} else if (wall == BlockFace.WEST){
			lobbyMiddleWallBlock = volume.getCornerOne().getFace(BlockFace.UP).getFace(BlockFace.NORTH, lobbyHalfSide);
		}
	}
	
	public void changeWall(BlockFace newWall) {
		if(this.wall != newWall) {
			if(volume == null) {
				// no previous wall
				this.volume = new Volume("lobby", war, warzone.getWorld());
			} else {
				// move the lobby
				this.volume.resetBlocks();
			}
			
			this.wall = newWall;
			// find center of the wall and set the new volume corners
			VerticalVolume zoneVolume = warzone.getVolume();
			
			Block corner1 = null;
			Block corner2 = null;
			
			if(wall == BlockFace.NORTH) {
				int wallStart = zoneVolume.getMinZ();
				int wallEnd = zoneVolume.getMaxZ();
				int x = zoneVolume.getMinX();
				int wallLength = wallEnd - wallStart + 1;
				int wallCenterPos = wallStart + wallLength / 2;
				int highestNonAirBlockAtCenter = warzone.getWorld().getHighestBlockYAt(x, wallCenterPos);
				lobbyMiddleWallBlock = warzone.getWorld().getBlockAt(x, highestNonAirBlockAtCenter+1, wallCenterPos);
				corner1 = warzone.getWorld().getBlockAt(x, highestNonAirBlockAtCenter, wallCenterPos + lobbyHalfSide);
				corner2 = warzone.getWorld().getBlockAt(x - lobbyDepth, 
						highestNonAirBlockAtCenter + 1 + lobbyHeight, wallCenterPos - lobbyHalfSide);
			} else if (wall == BlockFace.EAST){
				int wallStart = zoneVolume.getMinX();
				int wallEnd = zoneVolume.getMaxX();
				int z = zoneVolume.getMinZ();
				int wallLength = wallEnd - wallStart + 1;
				int wallCenterPos = wallStart + wallLength / 2;
				int highestNonAirBlockAtCenter = warzone.getWorld().getHighestBlockYAt(wallCenterPos, z);
				lobbyMiddleWallBlock = warzone.getWorld().getBlockAt(wallCenterPos, highestNonAirBlockAtCenter + 1, z);
				corner1 = warzone.getWorld().getBlockAt(wallCenterPos - lobbyHalfSide, highestNonAirBlockAtCenter, z);
				corner2 = warzone.getWorld().getBlockAt(wallCenterPos + lobbyHalfSide, 
						highestNonAirBlockAtCenter + 1 + lobbyHeight, z - lobbyDepth);
	 		} else if (wall == BlockFace.SOUTH){
	 			int wallStart = zoneVolume.getMinZ();
				int wallEnd = zoneVolume.getMaxZ();
				int x = zoneVolume.getMaxX();
				int wallLength = wallEnd - wallStart + 1;
				int wallCenterPos = wallStart + wallLength / 2;
				int highestNonAirBlockAtCenter = warzone.getWorld().getHighestBlockYAt(x, wallCenterPos);
				lobbyMiddleWallBlock = warzone.getWorld().getBlockAt(x, highestNonAirBlockAtCenter + 1, wallCenterPos);
				corner1 = warzone.getWorld().getBlockAt(x, highestNonAirBlockAtCenter, wallCenterPos - lobbyHalfSide);
				corner2 = warzone.getWorld().getBlockAt(x + lobbyDepth, 
						highestNonAirBlockAtCenter + 1 + lobbyHeight, wallCenterPos + lobbyHalfSide);
			} else if (wall == BlockFace.WEST){
				int wallStart = zoneVolume.getMinX();
				int wallEnd = zoneVolume.getMaxX();
				int z = zoneVolume.getMaxZ();
				int wallLength = wallEnd - wallStart + 1;
				int wallCenterPos = wallStart + wallLength / 2;
				int highestNonAirBlockAtCenter = warzone.getWorld().getHighestBlockYAt(wallCenterPos, z);
				lobbyMiddleWallBlock = warzone.getWorld().getBlockAt(wallCenterPos, highestNonAirBlockAtCenter + 1, z);
				corner1 = warzone.getWorld().getBlockAt(wallCenterPos + lobbyHalfSide, highestNonAirBlockAtCenter, z);
				corner2 = warzone.getWorld().getBlockAt(wallCenterPos - lobbyHalfSide, highestNonAirBlockAtCenter + 1 + lobbyHeight, z + lobbyDepth);
			}
			
			if(corner1 != null && corner2 != null) {
				// save the blocks, wide enough for three team gates, 3+1 high and 10 deep, extruding out from the zone wall.
				this.volume.setCornerOne(corner1);
				this.volume.setCornerTwo(corner2);
				this.volume.saveBlocks();
				VolumeMapper.save(volume, warzone.getName(), war);
			}
		}
	}
	
	public void initialize() {
		// maybe the number of teams change, now reset the gate positions
		setGatePositions(lobbyMiddleWallBlock);

		if(lobbyMiddleWallBlock != null && volume != null && volume.isSaved()) {
			// flatten the area (set all but floor to air, then replace any floor air blocks with glass)
			this.volume.setToMaterial(Material.AIR);
			this.volume.setFaceMaterial(BlockFace.DOWN, Material.GLASS);	// beautiful
			
			// add war hub link gate
			if(war.getWarHub() != null) {
				placeGate(warHubLinkGate, Material.OBSIDIAN);
			}
			
			// add team gates or single auto assign gate
			placeAutoAssignGate();
			placeGate(diamondGate, TeamMaterials.TEAMDIAMOND);
			placeGate(ironGate, TeamMaterials.TEAMIRON);
			placeGate(goldGate, TeamMaterials.TEAMGOLD);
			
			// set zone tp
			zoneTeleportBlock = lobbyMiddleWallBlock.getFace(wall, 6);
			warzone.setTeleport(new Location(warzone.getWorld(), zoneTeleportBlock.getX(), zoneTeleportBlock.getY(), zoneTeleportBlock.getZ()));
		} else {
			war.getLogger().warning("Failed to initalize zone " + warzone.getName());
		}
	}

	private void setGatePositions(Block lobbyMiddleWallBlock) {
		BlockFace leftSide = null;	// look at the zone
		BlockFace rightSide = null;
		if(wall == BlockFace.NORTH) {
			leftSide = BlockFace.EAST;
			rightSide = BlockFace.WEST;
		} else if(wall == BlockFace.EAST) {
			leftSide = BlockFace.SOUTH;
			rightSide = BlockFace.NORTH;
		} else if(wall == BlockFace.SOUTH) {
			leftSide = BlockFace.WEST;
			rightSide = BlockFace.EAST;
		} else if(wall == BlockFace.WEST) {
			leftSide = BlockFace.NORTH;
			rightSide = BlockFace.SOUTH;
		}  
		if(warzone.getAutoAssignOnly()){
			autoAssignGate = lobbyMiddleWallBlock;
		} else if(warzone.getTeams().size() == 1) { 
			if(warzone.getTeamByMaterial(TeamMaterials.TEAMDIAMOND) != null) {
				diamondGate = lobbyMiddleWallBlock;
			} else if (warzone.getTeamByMaterial(TeamMaterials.TEAMIRON) != null) {
				ironGate = lobbyMiddleWallBlock;
			} else if (warzone.getTeamByMaterial(TeamMaterials.TEAMGOLD) != null) {
				goldGate = lobbyMiddleWallBlock;
			}
		} else if(warzone.getTeams().size() == 2) {
			if(warzone.getTeamByMaterial(TeamMaterials.TEAMDIAMOND) != null 
					&& warzone.getTeamByMaterial(TeamMaterials.TEAMIRON) != null) {
				diamondGate = lobbyMiddleWallBlock.getFace(leftSide, 2);
				ironGate = lobbyMiddleWallBlock.getFace(rightSide, 2);
			} else if (warzone.getTeamByMaterial(TeamMaterials.TEAMIRON) != null
					&& warzone.getTeamByMaterial(TeamMaterials.TEAMGOLD) != null) {
				ironGate = lobbyMiddleWallBlock.getFace(leftSide, 2);
				goldGate = lobbyMiddleWallBlock.getFace(rightSide, 2);
			}
			if (warzone.getTeamByMaterial(TeamMaterials.TEAMDIAMOND) != null 
					&& warzone.getTeamByMaterial(TeamMaterials.TEAMGOLD) != null) {
				diamondGate = lobbyMiddleWallBlock.getFace(leftSide, 2);
				goldGate = lobbyMiddleWallBlock.getFace(rightSide, 2);
			}
		} else if(warzone.getTeams().size() == 3) {
			diamondGate = lobbyMiddleWallBlock.getFace(leftSide, 4);
			ironGate = lobbyMiddleWallBlock;
			goldGate = lobbyMiddleWallBlock.getFace(rightSide, 4);
		}
		warHubLinkGate = lobbyMiddleWallBlock.getFace(wall, 8);
	}

	private void placeGate(Block block,
			Material teamMaterial) {
		if(block != null) {
			BlockFace leftSide = null;	// look at the zone
			BlockFace rightSide = null;
			if(wall == BlockFace.NORTH) {
				leftSide = BlockFace.EAST;
				rightSide = BlockFace.WEST;
			} else if(wall == BlockFace.EAST) {
				leftSide = BlockFace.SOUTH;
				rightSide = BlockFace.NORTH;
			} else if(wall == BlockFace.SOUTH) {
				leftSide = BlockFace.WEST;
				rightSide = BlockFace.EAST;
			} else if(wall == BlockFace.WEST) {
				leftSide = BlockFace.NORTH;
				rightSide = BlockFace.SOUTH;
			}  
			block.setType(Material.PORTAL);
			block.getFace(BlockFace.UP).setType(Material.PORTAL);
			block.getFace(leftSide).setType(teamMaterial);
			block.getFace(rightSide).getFace(BlockFace.UP).setType(teamMaterial);
			block.getFace(leftSide).getFace(BlockFace.UP).getFace(BlockFace.UP).setType(teamMaterial);
			block.getFace(rightSide).setType(teamMaterial);
			block.getFace(leftSide).getFace(BlockFace.UP).setType(teamMaterial);
			block.getFace(rightSide).getFace(BlockFace.UP).getFace(BlockFace.UP).setType(teamMaterial);
			block.getFace(BlockFace.UP).getFace(BlockFace.UP).setType(teamMaterial);
		}
	}
	
	private void placeAutoAssignGate() {
		if(autoAssignGate != null) {
			BlockFace leftSide = null;	// look at the zone
			BlockFace rightSide = null;
			if(wall == BlockFace.NORTH) {
				leftSide = BlockFace.EAST;
				rightSide = BlockFace.WEST;
			} else if(wall == BlockFace.EAST) {
				leftSide = BlockFace.SOUTH;
				rightSide = BlockFace.NORTH;
			} else if(wall == BlockFace.SOUTH) {
				leftSide = BlockFace.WEST;
				rightSide = BlockFace.EAST;
			} else if(wall == BlockFace.WEST) {
				leftSide = BlockFace.NORTH;
				rightSide = BlockFace.SOUTH;
			}  
			
			Team diamondTeam = warzone.getTeamByMaterial(TeamMaterials.TEAMDIAMOND);
			Team ironTeam = warzone.getTeamByMaterial(TeamMaterials.TEAMIRON);
			Team goldTeam = warzone.getTeamByMaterial(TeamMaterials.TEAMGOLD);
			autoAssignGate.setType(Material.PORTAL);
			autoAssignGate.getFace(BlockFace.UP).setType(Material.PORTAL);
			if(diamondTeam != null && ironTeam != null && goldTeam != null) {
				autoAssignGate.getFace(leftSide).setType(TeamMaterials.TEAMDIAMOND);
				autoAssignGate.getFace(leftSide).getFace(BlockFace.UP).setType(TeamMaterials.TEAMIRON);
				autoAssignGate.getFace(leftSide).getFace(BlockFace.UP).getFace(BlockFace.UP).setType(TeamMaterials.TEAMGOLD);
				autoAssignGate.getFace(rightSide).getFace(BlockFace.UP).setType(TeamMaterials.TEAMDIAMOND);
				autoAssignGate.getFace(rightSide).getFace(BlockFace.UP).getFace(BlockFace.UP).setType(TeamMaterials.TEAMIRON);
				autoAssignGate.getFace(rightSide).getFace(BlockFace.UP).setType(TeamMaterials.TEAMGOLD);
				autoAssignGate.getFace(rightSide).setType(TeamMaterials.TEAMDIAMOND);			
			} else if (diamondTeam != null && ironTeam != null) {
				autoAssignGate.getFace(leftSide).setType(TeamMaterials.TEAMDIAMOND);
				autoAssignGate.getFace(leftSide).getFace(BlockFace.UP).setType(TeamMaterials.TEAMIRON);
				autoAssignGate.getFace(leftSide).getFace(BlockFace.UP).getFace(BlockFace.UP).setType(TeamMaterials.TEAMDIAMOND);
				autoAssignGate.getFace(rightSide).getFace(BlockFace.UP).setType(TeamMaterials.TEAMIRON);
				autoAssignGate.getFace(rightSide).getFace(BlockFace.UP).getFace(BlockFace.UP).setType(TeamMaterials.TEAMDIAMOND);
				autoAssignGate.getFace(rightSide).getFace(BlockFace.UP).setType(TeamMaterials.TEAMIRON);
				autoAssignGate.getFace(rightSide).setType(TeamMaterials.TEAMDIAMOND);	
			} else if (ironTeam != null && goldTeam != null) {
				autoAssignGate.getFace(leftSide).setType(TeamMaterials.TEAMIRON);
				autoAssignGate.getFace(leftSide).getFace(BlockFace.UP).setType(TeamMaterials.TEAMGOLD);
				autoAssignGate.getFace(leftSide).getFace(BlockFace.UP).getFace(BlockFace.UP).setType(TeamMaterials.TEAMIRON);
				autoAssignGate.getFace(rightSide).getFace(BlockFace.UP).setType(TeamMaterials.TEAMGOLD);
				autoAssignGate.getFace(rightSide).getFace(BlockFace.UP).getFace(BlockFace.UP).setType(TeamMaterials.TEAMIRON);
				autoAssignGate.getFace(rightSide).getFace(BlockFace.UP).setType(TeamMaterials.TEAMGOLD);
				autoAssignGate.getFace(rightSide).setType(TeamMaterials.TEAMIRON);	
			} else if (diamondTeam != null && goldTeam != null) {
				autoAssignGate.getFace(leftSide).setType(TeamMaterials.TEAMDIAMOND);
				autoAssignGate.getFace(leftSide).getFace(BlockFace.UP).setType(TeamMaterials.TEAMGOLD);
				autoAssignGate.getFace(leftSide).getFace(BlockFace.UP).getFace(BlockFace.UP).setType(TeamMaterials.TEAMDIAMOND);
				autoAssignGate.getFace(rightSide).getFace(BlockFace.UP).setType(TeamMaterials.TEAMGOLD);
				autoAssignGate.getFace(rightSide).getFace(BlockFace.UP).getFace(BlockFace.UP).setType(TeamMaterials.TEAMDIAMOND);
				autoAssignGate.getFace(rightSide).getFace(BlockFace.UP).setType(TeamMaterials.TEAMGOLD);
				autoAssignGate.getFace(rightSide).setType(TeamMaterials.TEAMDIAMOND);	
			}
		}
	}

	public boolean isInTeamGate(Material team, Location location) {
		if(team == TeamMaterials.TEAMDIAMOND && diamondGate != null
				&& location.getBlockX() == diamondGate.getX()
				&& location.getBlockY() == diamondGate.getY()
				&& location.getBlockZ() == diamondGate.getZ()) {
			return true;
		} else if(team == TeamMaterials.TEAMIRON && ironGate != null
				&& location.getBlockX() == ironGate.getX()
				&& location.getBlockY() == ironGate.getY()
				&& location.getBlockZ() == ironGate.getZ()) {
			return true;
		} else if(team == TeamMaterials.TEAMGOLD && goldGate != null
				&& location.getBlockX() == goldGate.getX()
				&& location.getBlockY() == goldGate.getY()
				&& location.getBlockZ() == goldGate.getZ()) {
			return true;
		} 
		return false;
	}
	
	public boolean isAutoAssignGate(Location location) {
		if(autoAssignGate != null
				&& location.getBlockX() == autoAssignGate.getX()
				&& location.getBlockY() == autoAssignGate.getY()
				&& location.getBlockZ() == autoAssignGate.getZ()) {
			return true;
		} 
		return false;
	}
	
	public void resetSigns() {
		// TODO Signs
	}
	
	public Volume getVolume() {
		return this.volume;
	}
	
	public void setVolume(Volume volume) {
		this.volume = volume;
	}
	

	public BlockFace getWall() {
		return wall;
	}

	public boolean isInWarHubLinkGate(Location location) {
		if(warHubLinkGate != null
				&& location.getBlockX() == warHubLinkGate.getX()
				&& location.getBlockY() == warHubLinkGate.getY()
				&& location.getBlockZ() == warHubLinkGate.getZ()) {
			return true;
		} 
		return false;
	}

	public boolean blockIsAGateBlock(Block block, BlockFace blockWall) {
		if(blockWall == wall) {
			return isPartOfGate(diamondGate, block)
					|| isPartOfGate(ironGate, block)
					|| isPartOfGate(goldGate, block);
		}
		return false;
	}

	private boolean isPartOfGate(Block gateBlock, Block block) {
		if(gateBlock != null) {
			BlockFace leftSide = null;	// look at the zone
			BlockFace rightSide = null;
			if(wall == BlockFace.NORTH) {
				leftSide = BlockFace.EAST;
				rightSide = BlockFace.WEST;
			} else if(wall == BlockFace.EAST) {
				leftSide = BlockFace.SOUTH;
				rightSide = BlockFace.NORTH;
			} else if(wall == BlockFace.SOUTH) {
				leftSide = BlockFace.WEST;
				rightSide = BlockFace.EAST;
			} else if(wall == BlockFace.WEST) {
				leftSide = BlockFace.NORTH;
				rightSide = BlockFace.SOUTH;
			}
			return (block.getX() == gateBlock.getX()
						&& block.getY() == gateBlock.getY()
						&& block.getZ() == gateBlock.getZ())
					||
					(block.getX() == gateBlock.getFace(BlockFace.UP).getX()
						&& block.getY() == gateBlock.getFace(BlockFace.UP).getY()
						&& block.getZ() == gateBlock.getFace(BlockFace.UP).getZ())
					||
					(block.getX() == gateBlock.getFace(leftSide).getX()
						&& block.getY() == gateBlock.getFace(leftSide).getY()
						&& block.getZ() == gateBlock.getFace(leftSide).getZ())
					||
					(block.getX() == gateBlock.getFace(leftSide).getFace(BlockFace.UP).getX()
						&& block.getY() == gateBlock.getFace(leftSide).getFace(BlockFace.UP).getY()
						&& block.getZ() == gateBlock.getFace(leftSide).getFace(BlockFace.UP).getZ())
					||
					(block.getX() == gateBlock.getFace(leftSide).getFace(BlockFace.UP).getFace(BlockFace.UP).getX()
						&& block.getY() == gateBlock.getFace(leftSide).getFace(BlockFace.UP).getFace(BlockFace.UP).getY()
						&& block.getZ() == gateBlock.getFace(leftSide).getFace(BlockFace.UP).getFace(BlockFace.UP).getZ())
					||
					(block.getX() == gateBlock.getFace(BlockFace.UP).getFace(BlockFace.UP).getX()
						&& block.getY() == gateBlock.getFace(BlockFace.UP).getFace(BlockFace.UP).getY()
						&& block.getZ() == gateBlock.getFace(BlockFace.UP).getFace(BlockFace.UP).getZ())
					||
					(block.getX() == gateBlock.getFace(rightSide).getFace(BlockFace.UP).getX()
						&& block.getY() == gateBlock.getFace(rightSide).getFace(BlockFace.UP).getY()
						&& block.getZ() == gateBlock.getFace(rightSide).getFace(BlockFace.UP).getZ())
					||
					(block.getX() == gateBlock.getFace(rightSide).getFace(BlockFace.UP).getFace(BlockFace.UP).getX()
						&& block.getY() == gateBlock.getFace(rightSide).getFace(BlockFace.UP).getFace(BlockFace.UP).getY()
						&& block.getZ() == gateBlock.getFace(rightSide).getFace(BlockFace.UP).getFace(BlockFace.UP).getZ())
					||
					(block.getX() == gateBlock.getFace(rightSide).getX()
						&& block.getY() == gateBlock.getFace(rightSide).getY()
						&& block.getZ() == gateBlock.getFace(rightSide).getZ());
		}
		return false;
	}

	public Warzone getZone() {
		return this.warzone;
	}
}
