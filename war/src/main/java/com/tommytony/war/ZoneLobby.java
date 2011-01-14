package com.tommytony.war;

import org.bukkit.Block;
import org.bukkit.BlockFace;
import org.bukkit.Location;
import org.bukkit.Material;

import com.tommytony.war.mappers.VolumeMapper;
import com.tommytony.war.volumes.VerticalVolume;
import com.tommytony.war.volumes.Volume;

import bukkit.tommytony.war.War;

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
	
	Block warHubLinkGate = null;
	
	Block diamondGate = null;
	Block ironGate = null;
	Block goldGate = null;
	Block autoAssignGate = null;
	
	Block zoneTeleportBlock = null;
	
	public ZoneLobby(War war, Warzone warzone, BlockFace wall) {
		this.war = war;
		this.warzone = warzone;
		this.setWall(wall);
		this.volume = new Volume("lobby", war, warzone.getWorld());
	}
	
	public void initialize() {
		// find center of the wall and position of all elements
		VerticalVolume zoneVolume = warzone.getVolume();
		Location nw = warzone.getNorthwest();
		Block nwBlock = warzone.getWorld().getBlockAt(nw.getBlockX(), nw.getBlockY(), nw.getBlockZ());
		Location se = warzone.getSoutheast();
		Block seBlock = warzone.getWorld().getBlockAt(se.getBlockX(), se.getBlockY(), se.getBlockZ());
		Block lobbyMiddleWallBlock = null;
		Block corner1 = null;
		Block corner2 = null;
		
		int lobbyHeight = 3;
		int lobbyHalfSide = 7;
		int lobbyDepth = 10;
		if(wall == BlockFace.North) {
			int wallStart = zoneVolume.getMinZ();
			int wallEnd = zoneVolume.getMaxZ();
			int x = zoneVolume.getMinX();
			int wallLength = wallEnd - wallStart + 1;
			int wallCenterPos = wallStart + wallLength / 2;
			int highestNonAirBlockAtCenter = warzone.getWorld().getHighestBlockYAt(x, wallCenterPos);
			lobbyMiddleWallBlock = warzone.getWorld().getBlockAt(x, highestNonAirBlockAtCenter+1, wallCenterPos);
			corner1 = warzone.getWorld().getBlockAt(x, highestNonAirBlockAtCenter+1, wallCenterPos + lobbyHalfSide);
			corner2 = warzone.getWorld().getBlockAt(x - lobbyDepth, highestNonAirBlockAtCenter + 1 + lobbyHeight, wallCenterPos - lobbyHalfSide);
			setGatePositions(lobbyMiddleWallBlock);
		} else if (wall == BlockFace.East){
			int wallStart = zoneVolume.getMinX();
			int wallEnd = zoneVolume.getMaxX();
			int z = zoneVolume.getMinZ();
			int wallLength = wallEnd - wallStart + 1;
			int wallCenterPos = wallStart + wallLength / 2;
			int highestNonAirBlockAtCenter = warzone.getWorld().getHighestBlockYAt(wallCenterPos, z);
			lobbyMiddleWallBlock = warzone.getWorld().getBlockAt(wallCenterPos, highestNonAirBlockAtCenter + 1, z);
			corner1 = warzone.getWorld().getBlockAt(wallCenterPos - lobbyHalfSide, highestNonAirBlockAtCenter+1, z);
			corner2 = warzone.getWorld().getBlockAt(wallCenterPos + lobbyHalfSide, highestNonAirBlockAtCenter + 1 + lobbyHeight, z - lobbyDepth);
			setGatePositions(lobbyMiddleWallBlock);
 		} else if (wall == BlockFace.South){
 			int wallStart = zoneVolume.getMinZ();
			int wallEnd = zoneVolume.getMaxZ();
			int x = zoneVolume.getMaxX();
			int wallLength = wallEnd - wallStart + 1;
			int wallCenterPos = wallStart + wallLength / 2;
			int highestNonAirBlockAtCenter = warzone.getWorld().getHighestBlockYAt(x, wallCenterPos);
			lobbyMiddleWallBlock = warzone.getWorld().getBlockAt(x, highestNonAirBlockAtCenter + 1, wallCenterPos);
			corner1 = warzone.getWorld().getBlockAt(x, highestNonAirBlockAtCenter+1, wallCenterPos - lobbyHalfSide);
			corner2 = warzone.getWorld().getBlockAt(x + lobbyDepth, highestNonAirBlockAtCenter + 1 + lobbyHeight, wallCenterPos + lobbyHalfSide);
			setGatePositions(lobbyMiddleWallBlock);
		} else if (wall == BlockFace.West){
			int wallStart = zoneVolume.getMinX();
			int wallEnd = zoneVolume.getMaxX();
			int z = zoneVolume.getMaxZ();
			int wallLength = wallEnd - wallStart + 1;
			int wallCenterPos = wallStart + wallLength / 2;
			int highestNonAirBlockAtCenter = warzone.getWorld().getHighestBlockYAt(wallCenterPos, z);
			lobbyMiddleWallBlock = warzone.getWorld().getBlockAt(wallCenterPos, highestNonAirBlockAtCenter + 1, z);
			corner1 = warzone.getWorld().getBlockAt(wallCenterPos + lobbyHalfSide, highestNonAirBlockAtCenter+1, z);
			corner2 = warzone.getWorld().getBlockAt(wallCenterPos - lobbyHalfSide, highestNonAirBlockAtCenter + 1 + lobbyHeight, z + lobbyDepth);
			setGatePositions(lobbyMiddleWallBlock);
		}
		
		if(lobbyMiddleWallBlock != null && corner1 != null && corner2 != null) {
			// save the blocks, wide enough for three team gates, 3+1 high and 10 deep, extruding out from the zone wall.
			this.volume.setCornerOne(corner1);
			this.volume.setCornerTwo(corner2);
			this.volume.saveBlocks();
			VolumeMapper.save(volume, warzone.getName(), war);
			
			// flatten the area (set all but floor to air, then replace any floor air blocks with glass)
			this.volume.setToMaterial(Material.AIR);
			this.volume.setFaceMaterial(BlockFace.Down, Material.AIR);	// beautiful
			
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
		}
	}

	private void setGatePositions(Block lobbyMiddleWallBlock) {
		BlockFace leftSide = null;	// look at the zone
		BlockFace rightSide = null;
		if(wall == BlockFace.North) {
			leftSide = BlockFace.East;
			rightSide = BlockFace.West;
		} else if(wall == BlockFace.East) {
			leftSide = BlockFace.South;
			rightSide = BlockFace.North;
		} else if(wall == BlockFace.South) {
			leftSide = BlockFace.West;
			rightSide = BlockFace.East;
		} else if(wall == BlockFace.West) {
			leftSide = BlockFace.North;
			rightSide = BlockFace.South;
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
				ironGate = lobbyMiddleWallBlock.getFace(BlockFace.West, 2);
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
			if(wall == BlockFace.North) {
				leftSide = BlockFace.East;
				rightSide = BlockFace.West;
			} else if(wall == BlockFace.East) {
				leftSide = BlockFace.South;
				rightSide = BlockFace.North;
			} else if(wall == BlockFace.South) {
				leftSide = BlockFace.West;
				rightSide = BlockFace.East;
			} else if(wall == BlockFace.West) {
				leftSide = BlockFace.North;
				rightSide = BlockFace.South;
			}  
			block.setType(Material.PORTAL);
			block.getFace(BlockFace.Up).setType(Material.PORTAL);
			block.getFace(leftSide).setType(teamMaterial);
			block.getFace(rightSide).getFace(BlockFace.Up).setType(teamMaterial);
			block.getFace(leftSide).getFace(BlockFace.Up).getFace(BlockFace.Up).setType(teamMaterial);
			block.getFace(rightSide).setType(teamMaterial);
			block.getFace(leftSide).getFace(BlockFace.Up).setType(teamMaterial);
			block.getFace(rightSide).getFace(BlockFace.Up).getFace(BlockFace.Up).setType(teamMaterial);
			block.getFace(BlockFace.Up).getFace(BlockFace.Up).setType(teamMaterial);
		}
	}
	
	private void placeAutoAssignGate() {
		if(autoAssignGate != null) {
			BlockFace leftSide = null;	// look at the zone
			BlockFace rightSide = null;
			if(wall == BlockFace.North) {
				leftSide = BlockFace.East;
				rightSide = BlockFace.West;
			} else if(wall == BlockFace.East) {
				leftSide = BlockFace.South;
				rightSide = BlockFace.North;
			} else if(wall == BlockFace.South) {
				leftSide = BlockFace.West;
				rightSide = BlockFace.East;
			} else if(wall == BlockFace.West) {
				leftSide = BlockFace.North;
				rightSide = BlockFace.South;
			}  
			
			Team diamondTeam = warzone.getTeamByMaterial(TeamMaterials.TEAMDIAMOND);
			Team ironTeam = warzone.getTeamByMaterial(TeamMaterials.TEAMIRON);
			Team goldTeam = warzone.getTeamByMaterial(TeamMaterials.TEAMGOLD);
			autoAssignGate.setType(Material.PORTAL);
			autoAssignGate.getFace(BlockFace.Up).setType(Material.PORTAL);
			if(diamondTeam != null && ironTeam != null && goldTeam != null) {
				autoAssignGate.getFace(leftSide).setType(TeamMaterials.TEAMDIAMOND);
				autoAssignGate.getFace(leftSide).getFace(BlockFace.Up).setType(TeamMaterials.TEAMIRON);
				autoAssignGate.getFace(leftSide).getFace(BlockFace.Up).getFace(BlockFace.Up).setType(TeamMaterials.TEAMGOLD);
				autoAssignGate.getFace(rightSide).getFace(BlockFace.Up).setType(TeamMaterials.TEAMDIAMOND);
				autoAssignGate.getFace(rightSide).getFace(BlockFace.Up).getFace(BlockFace.Up).setType(TeamMaterials.TEAMIRON);
				autoAssignGate.getFace(rightSide).getFace(BlockFace.Up).setType(TeamMaterials.TEAMGOLD);
				autoAssignGate.getFace(rightSide).setType(TeamMaterials.TEAMDIAMOND);			
			} else if (diamondTeam != null && ironTeam != null) {
				autoAssignGate.getFace(leftSide).setType(TeamMaterials.TEAMDIAMOND);
				autoAssignGate.getFace(leftSide).getFace(BlockFace.Up).setType(TeamMaterials.TEAMIRON);
				autoAssignGate.getFace(leftSide).getFace(BlockFace.Up).getFace(BlockFace.Up).setType(TeamMaterials.TEAMDIAMOND);
				autoAssignGate.getFace(rightSide).getFace(BlockFace.Up).setType(TeamMaterials.TEAMIRON);
				autoAssignGate.getFace(rightSide).getFace(BlockFace.Up).getFace(BlockFace.Up).setType(TeamMaterials.TEAMDIAMOND);
				autoAssignGate.getFace(rightSide).getFace(BlockFace.Up).setType(TeamMaterials.TEAMIRON);
				autoAssignGate.getFace(rightSide).setType(TeamMaterials.TEAMDIAMOND);	
			} else if (ironTeam != null && goldTeam != null) {
				autoAssignGate.getFace(leftSide).setType(TeamMaterials.TEAMIRON);
				autoAssignGate.getFace(leftSide).getFace(BlockFace.Up).setType(TeamMaterials.TEAMGOLD);
				autoAssignGate.getFace(leftSide).getFace(BlockFace.Up).getFace(BlockFace.Up).setType(TeamMaterials.TEAMIRON);
				autoAssignGate.getFace(rightSide).getFace(BlockFace.Up).setType(TeamMaterials.TEAMGOLD);
				autoAssignGate.getFace(rightSide).getFace(BlockFace.Up).getFace(BlockFace.Up).setType(TeamMaterials.TEAMIRON);
				autoAssignGate.getFace(rightSide).getFace(BlockFace.Up).setType(TeamMaterials.TEAMGOLD);
				autoAssignGate.getFace(rightSide).setType(TeamMaterials.TEAMIRON);	
			} else if (diamondTeam != null && goldTeam != null) {
				autoAssignGate.getFace(leftSide).setType(TeamMaterials.TEAMDIAMOND);
				autoAssignGate.getFace(leftSide).getFace(BlockFace.Up).setType(TeamMaterials.TEAMGOLD);
				autoAssignGate.getFace(leftSide).getFace(BlockFace.Up).getFace(BlockFace.Up).setType(TeamMaterials.TEAMDIAMOND);
				autoAssignGate.getFace(rightSide).getFace(BlockFace.Up).setType(TeamMaterials.TEAMGOLD);
				autoAssignGate.getFace(rightSide).getFace(BlockFace.Up).getFace(BlockFace.Up).setType(TeamMaterials.TEAMDIAMOND);
				autoAssignGate.getFace(rightSide).getFace(BlockFace.Up).setType(TeamMaterials.TEAMGOLD);
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

	public void setWall(BlockFace wall) {
		this.wall = wall;
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
}
