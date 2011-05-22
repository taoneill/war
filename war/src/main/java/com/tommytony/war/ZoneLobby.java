package com.tommytony.war;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import bukkit.tommytony.war.War;

import com.tommytony.war.utils.SignHelper;
import com.tommytony.war.volumes.BlockInfo;
import com.tommytony.war.volumes.Volume;
import com.tommytony.war.volumes.ZoneVolume;

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
	BlockInfo lobbyMiddleWallBlock = null;	// on the zone wall, one above the zone lobby floor
	
	BlockInfo warHubLinkGate = null;
	
	Map<String, BlockInfo> teamGateBlocks = new HashMap<String, BlockInfo>();  
	BlockInfo autoAssignGate = null;
	
	BlockInfo zoneTeleportBlock = null;
	
	private final int lobbyHeight = 3;
	private int lobbyHalfSide;
	private final int lobbyDepth = 10;
	
	/**
	 * Use this constructor with /setzonelobby <n/s/e/w>
	 * @param war
	 * @param warzone
	 * @param wall On which wall of the warzone will the lobby be stuck to at mid-weight
	 */
	public ZoneLobby(War war, Warzone warzone, BlockFace wall) {
		this.war = war;
		this.warzone = warzone;
		int lobbyWidth = warzone.getTeams().size() * 4 + 5;
		lobbyHalfSide = lobbyWidth / 2;
		if(lobbyHalfSide < 7) {
			lobbyHalfSide = 7;
		}
		this.setWall(wall);
	}
	
	/**
	 * Use this constructor with /setzonelobby <zonename>.
	 * Makes sure the lobby is not sticking inside the zone.
	 * @param war
	 * @param warzone
	 * @param wall On which wall of the warzone will the lobby be stuck to at mid-weight
	 */
	public ZoneLobby(War war, Warzone warzone, Location playerLocation) {
		this.war = war;
		this.warzone = warzone;
		int lobbyWidth = warzone.getTeams().size() * 4 + 5;
		lobbyHalfSide = lobbyWidth / 2;
		if(lobbyHalfSide < 7) {
			lobbyHalfSide = 7;
		}
		setLocation(playerLocation);
	}


	/**
	 * Convenience ctor when loading form disk.
	 * This figures out the middle wall block of the lobby from the volume instead 
	 * of the other way around.
	 */
	public ZoneLobby(War war, Warzone warzone, BlockFace wall, Volume volume) {
		this.war = war;
		this.warzone = warzone;
		int lobbyWidth = warzone.getTeams().size() * 4 + 5;
		lobbyHalfSide = lobbyWidth / 2;
		if(lobbyHalfSide < 7) {
			lobbyHalfSide = 7;
		}
		this.wall = wall;
		this.setVolume(volume);
		
		// we're setting the zoneVolume directly, so we need to figure out the lobbyMiddleWallBlock on our own
		if(wall == BlockFace.NORTH) {
			lobbyMiddleWallBlock = new BlockInfo(BlockInfo.getBlock(warzone.getWorld(), volume.getCornerOne()).getFace(BlockFace.UP).getFace(BlockFace.EAST, lobbyHalfSide)); 
		} else if (wall == BlockFace.EAST){
			lobbyMiddleWallBlock = new BlockInfo(BlockInfo.getBlock(warzone.getWorld(), volume.getCornerOne()).getFace(BlockFace.UP).getFace(BlockFace.SOUTH, lobbyHalfSide));
 		} else if (wall == BlockFace.SOUTH){
 			lobbyMiddleWallBlock = new BlockInfo(BlockInfo.getBlock(warzone.getWorld(), volume.getCornerOne()).getFace(BlockFace.UP).getFace(BlockFace.WEST, lobbyHalfSide));
		} else if (wall == BlockFace.WEST){
			lobbyMiddleWallBlock = new BlockInfo(BlockInfo.getBlock(warzone.getWorld(), volume.getCornerOne()).getFace(BlockFace.UP).getFace(BlockFace.NORTH, lobbyHalfSide));
		}
	}
	
	/**
	 * Changes the lobby's position. Orientation is determined from the player location.
	 * Creates volume or resets. Saves new lobby blocks.
	 * @param playerLocation
	 */
	public void setLocation(Location playerLocation) {
		createVolumeOrReset();
		
		// Lobby orientation
		int yaw = 0;
		if(playerLocation.getYaw() >= 0){
			yaw = (int)(playerLocation.getYaw() % 360);
		} else {
			yaw = (int)(360 + (playerLocation.getYaw() % 360));
		}
		BlockFace facing = null;
		BlockFace opposite = null;
		if((yaw >= 0 && yaw < 45) || (yaw >= 315 && yaw <= 360)) {
			facing = BlockFace.WEST;
			opposite = BlockFace.EAST;
		} else if(yaw >= 45 && yaw < 135) {
			facing = BlockFace.NORTH;
			opposite = BlockFace.SOUTH;
		} else if(yaw >= 135 && yaw < 225) {
			facing = BlockFace.EAST;
			opposite = BlockFace.WEST;
		} else if(yaw >= 225 && yaw < 315) {
			facing = BlockFace.SOUTH;
			opposite = BlockFace.NORTH;
		}
		
//		float yaw = playerLocation.getYaw() % 360;
//		if(yaw >= 45 && yaw < 135) {
//			facing = BlockFace.NORTH;
//			opposite = BlockFace.SOUTH;
//		} else if(yaw >= 135 && yaw < 225) {
//			facing = BlockFace.EAST;
//			opposite = BlockFace.WEST;
//		} else if(yaw >= 225 && yaw < 315) {
//			facing = BlockFace.SOUTH;
//			opposite = BlockFace.NORTH;
//		} else if(yaw >= 315 || yaw < 45) {
//			facing = BlockFace.WEST;
//			opposite = BlockFace.EAST;
//		} 
		this.wall = opposite;	// a player facing south places a lobby that looks just like a lobby stuck to the north wall 
		
		ZoneVolume zoneVolume = warzone.getVolume();
		calculateLobbyWidth();
		
		lobbyMiddleWallBlock = new BlockInfo(warzone.getWorld().getBlockAt(playerLocation.getBlockX(), 
																		playerLocation.getBlockY(), 
																		playerLocation.getBlockZ())
																	.getFace(facing, 6));
		
		Block corner1 = null;
		Block corner2 = null;
		int x = lobbyMiddleWallBlock.getX();
		int y = lobbyMiddleWallBlock.getY();
		int z = lobbyMiddleWallBlock.getZ();
		
		if(wall == BlockFace.NORTH) {
			//lobbyMiddleWallBlock = new BlockInfo(warzone.getWorld().getBlockAt(x, y, wallCenterPos));
			corner1 = warzone.getWorld().getBlockAt(x, y - 1, z + lobbyHalfSide);
			corner2 = warzone.getWorld().getBlockAt(x - lobbyDepth, y + 1 + lobbyHeight, z - lobbyHalfSide);
		} else if (wall == BlockFace.EAST){
			corner1 = warzone.getWorld().getBlockAt(x - lobbyHalfSide, y - 1, z);
			corner2 = warzone.getWorld().getBlockAt(x + lobbyHalfSide, y + 1 + lobbyHeight, z - lobbyDepth);
 		} else if (wall == BlockFace.SOUTH){
			corner1 = warzone.getWorld().getBlockAt(x, y -1 , z - lobbyHalfSide);
			corner2 = warzone.getWorld().getBlockAt(x + lobbyDepth, y + 1 + lobbyHeight, z + lobbyHalfSide);
		} else if (wall == BlockFace.WEST){
			corner1 = warzone.getWorld().getBlockAt(x + lobbyHalfSide, y - 1, z);
			corner2 = warzone.getWorld().getBlockAt(x - lobbyHalfSide, y + 1 + lobbyHeight, z + lobbyDepth);
		}
		
		saveLobbyBlocks(corner1, corner2);
	}
	
	/**
	 * Classic way of creating a lobby. Lobby position goes to middle of zone wall.
	 * Creates volume or resets. Saves new lobby blocks.
	 * @param newWall
	 */
	public void setWall(BlockFace newWall) {
		createVolumeOrReset();
		this.wall = newWall;
		
		ZoneVolume zoneVolume = warzone.getVolume();
		calculateLobbyWidth();
		
		Block corner1 = null;
		Block corner2 = null;
		
		if(wall == BlockFace.NORTH) {
			int wallStart = zoneVolume.getMinZ();
			int wallEnd = zoneVolume.getMaxZ();
			int x = zoneVolume.getMinX();
			int wallLength = wallEnd - wallStart + 1;
			int wallCenterPos = wallStart + wallLength / 2;
			int y = zoneVolume.getCenterY();
			lobbyMiddleWallBlock = new BlockInfo(warzone.getWorld().getBlockAt(x, y, wallCenterPos));
			corner1 = warzone.getWorld().getBlockAt(x, y - 1, wallCenterPos + lobbyHalfSide);
			corner2 = warzone.getWorld().getBlockAt(x - lobbyDepth, y + 1 + lobbyHeight, wallCenterPos - lobbyHalfSide);
		} else if (wall == BlockFace.EAST){
			int wallStart = zoneVolume.getMinX();
			int wallEnd = zoneVolume.getMaxX();
			int z = zoneVolume.getMinZ();
			int wallLength = wallEnd - wallStart + 1;
			int wallCenterPos = wallStart + wallLength / 2;
			int y = zoneVolume.getCenterY();
			lobbyMiddleWallBlock = new BlockInfo(warzone.getWorld().getBlockAt(wallCenterPos, y, z));
			corner1 = warzone.getWorld().getBlockAt(wallCenterPos - lobbyHalfSide, y - 1, z);
			corner2 = warzone.getWorld().getBlockAt(wallCenterPos + lobbyHalfSide, 
					y + 1 + lobbyHeight, z - lobbyDepth);
 		} else if (wall == BlockFace.SOUTH){
 			int wallStart = zoneVolume.getMinZ();
			int wallEnd = zoneVolume.getMaxZ();
			int x = zoneVolume.getMaxX();
			int wallLength = wallEnd - wallStart + 1;
			int wallCenterPos = wallStart + wallLength / 2;
			int y = zoneVolume.getCenterY();
			lobbyMiddleWallBlock = new BlockInfo(warzone.getWorld().getBlockAt(x, y, wallCenterPos));
			corner1 = warzone.getWorld().getBlockAt(x, y -1 , wallCenterPos - lobbyHalfSide);
			corner2 = warzone.getWorld().getBlockAt(x + lobbyDepth, 
					y + 1 + lobbyHeight, wallCenterPos + lobbyHalfSide);
		} else if (wall == BlockFace.WEST){
			int wallStart = zoneVolume.getMinX();
			int wallEnd = zoneVolume.getMaxX();
			int z = zoneVolume.getMaxZ();
			int wallLength = wallEnd - wallStart + 1;
			int wallCenterPos = wallStart + wallLength / 2;
			int y = zoneVolume.getCenterY();
			lobbyMiddleWallBlock = new BlockInfo(warzone.getWorld().getBlockAt(wallCenterPos, y, z));
			corner1 = warzone.getWorld().getBlockAt(wallCenterPos + lobbyHalfSide, y - 1, z);
			corner2 = warzone.getWorld().getBlockAt(wallCenterPos - lobbyHalfSide, y + 1 + lobbyHeight, z + lobbyDepth);
		}
		
		saveLobbyBlocks(corner1, corner2);
	}
	
	private void createVolumeOrReset() {
		if(volume == null) {
			// no previous wall
			this.volume = new Volume("lobby", war, warzone.getWorld());
		} else if(volume.isSaved()) {
			volume.resetBlocks();
		}
	}
	
	private void calculateLobbyWidth() {
		int lobbyWidth = warzone.getTeams().size() * 4 + 5;
		lobbyHalfSide = lobbyWidth / 2;
		if(lobbyHalfSide < 7) {
			lobbyHalfSide = 7;
		}
	}
	
	private void saveLobbyBlocks(Block corner1, Block corner2) {
		if(corner1 != null && corner2 != null) {
			// save the blocks, wide enough for three team gates, 3+1 high and 10 deep, extruding out from the zone wall.
			this.volume.setCornerOne(corner1);
			this.volume.setCornerTwo(corner2);
			this.volume.saveBlocks();
		}
	}
	
	public void initialize() {		
		// maybe the number of teams change, now reset the gate positions
		if(lobbyMiddleWallBlock != null && volume != null /*&& volume.isSaved()*/) {
			setGatePositions(BlockInfo.getBlock(warzone.getWorld(), lobbyMiddleWallBlock));
			// flatten the area (set all but floor to air, then replace any floor air blocks with glass)
			this.volume.clearBlocksThatDontFloat();
			this.volume.setToMaterial(Material.AIR);
			this.volume.setFaceMaterial(BlockFace.DOWN, Material.GLASS);	// beautiful
			
			// add war hub link gate
			if(war.getWarHub() != null) {
				Block linkGateBlock = BlockInfo.getBlock(warzone.getWorld(), warHubLinkGate);
				placeGate(linkGateBlock, Material.OBSIDIAN);
				// add warhub sign
				String[] lines = new String[4];
				lines[0] = "";
				lines[1] = "To War hub";
				lines[2] = "";
				lines[3] = "";
				resetGateSign(linkGateBlock, lines, false);
			}
			
			// add team gates or single auto assign gate
			placeAutoAssignGate();
			for(String teamName : teamGateBlocks.keySet()) {
				BlockInfo gateInfo = teamGateBlocks.get(teamName);
				placeGate(BlockInfo.getBlock(warzone.getWorld(), gateInfo), TeamKinds.teamKindFromString(teamName));
			}
			for(Team t : warzone.getTeams()) {
				resetTeamGateSign(t);
			}
			
			// set zone tp
			zoneTeleportBlock = new BlockInfo(BlockInfo.getBlock(warzone.getWorld(), lobbyMiddleWallBlock).getFace(wall, 6));
			int yaw = 0;
			if(wall == BlockFace.WEST) {
				yaw = 180;
			} else if (wall == BlockFace.SOUTH) {
				yaw = 90;
			} else if (wall == BlockFace.EAST) {
				yaw = 0;
			} else if (wall == BlockFace.NORTH) {
				yaw = 270;
			}
			warzone.setTeleport(new Location(warzone.getWorld(), zoneTeleportBlock.getX(), zoneTeleportBlock.getY(), zoneTeleportBlock.getZ(), yaw, 0));
			
			// set zone sign
			Block zoneSignBlock = BlockInfo.getBlock(warzone.getWorld(), lobbyMiddleWallBlock).getFace(wall, 4);
			byte data = 0;
			if(wall == BlockFace.NORTH) {
				data = (byte)4;
			} else if(wall == BlockFace.EAST) {
				data = (byte)8;
			} else if(wall == BlockFace.SOUTH) {
				data = (byte)12;
			} else if(wall == BlockFace.WEST) {
				data = (byte)0;
			}
			String[] lines = new String[4];
			lines[0] = "Warzone";
			lines[1] = warzone.getName();
			if(autoAssignGate != null) {
				lines[2] = "Walk in the";
				lines[3] = "auto-assign gate.";
			} else {
				lines[2] = "";
				lines[3] = "Pick your team.";
			}
			SignHelper.setToSign(war, zoneSignBlock, data, lines);
			
			// lets get some light in here
			if(wall == BlockFace.NORTH || wall == BlockFace.SOUTH) {
				BlockInfo.getBlock(warzone.getWorld(), lobbyMiddleWallBlock).getFace(BlockFace.DOWN).getFace(BlockFace.WEST, lobbyHalfSide - 1).getFace(wall, 9).setType(Material.GLOWSTONE);
				BlockInfo.getBlock(warzone.getWorld(), lobbyMiddleWallBlock).getFace(BlockFace.DOWN).getFace(BlockFace.EAST, lobbyHalfSide - 1).getFace(wall, 9).setType(Material.GLOWSTONE);
			} else {
				BlockInfo.getBlock(warzone.getWorld(), lobbyMiddleWallBlock).getFace(BlockFace.DOWN).getFace(BlockFace.NORTH, lobbyHalfSide - 1).getFace(wall, 9).setType(Material.GLOWSTONE);
				BlockInfo.getBlock(warzone.getWorld(), lobbyMiddleWallBlock).getFace(BlockFace.DOWN).getFace(BlockFace.SOUTH, lobbyHalfSide - 1).getFace(wall, 9).setType(Material.GLOWSTONE);
			}
		} else {
			war.logWarn("Failed to initalize zone lobby for zone " + warzone.getName());
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
		teamGateBlocks.clear();
		if(warzone.getAutoAssignOnly()){
			autoAssignGate = new BlockInfo(lobbyMiddleWallBlock);
		} else {
			autoAssignGate = null;
			for(int doorIndex = 0; doorIndex < warzone.getTeams().size(); doorIndex++) {
				// 0 at center, 1 to the left, 2 to the right, 3 to the left, etc
				Team team = warzone.getTeams().get(doorIndex);
				if(warzone.getTeams().size() % 2 == 0) {
					// even number of teams
					if(doorIndex % 2 == 0) {
						teamGateBlocks.put(team.getName(), new BlockInfo(lobbyMiddleWallBlock.getFace(rightSide, doorIndex * 2 + 2)));
					} else {
						teamGateBlocks.put(team.getName(), new BlockInfo(lobbyMiddleWallBlock.getFace(leftSide, doorIndex * 2)));
					}
					
				} else {
					if(doorIndex == 0) {
						teamGateBlocks.put(team.getName(), new BlockInfo(lobbyMiddleWallBlock));
					}
					else if(doorIndex % 2 == 0) {
						teamGateBlocks.put(team.getName(), new BlockInfo(lobbyMiddleWallBlock.getFace(rightSide, doorIndex * 2)));
					} else {
						teamGateBlocks.put(team.getName(), new BlockInfo(lobbyMiddleWallBlock.getFace(leftSide, doorIndex * 2 + 2)));
					}
				}				
			}
		}
		warHubLinkGate = new BlockInfo(lobbyMiddleWallBlock.getFace(wall, 9));
	}

	private void placeGate(Block block,
			TeamKind teamKind) {
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
			block.getFace(BlockFace.DOWN).setType(Material.GLOWSTONE);
			setBlock(block.getFace(leftSide), teamKind);
			setBlock(block.getFace(rightSide).getFace(BlockFace.UP), teamKind);
			setBlock(block.getFace(leftSide).getFace(BlockFace.UP).getFace(BlockFace.UP), teamKind);
			setBlock(block.getFace(rightSide), teamKind);
			setBlock(block.getFace(leftSide).getFace(BlockFace.UP), teamKind);
			setBlock(block.getFace(rightSide).getFace(BlockFace.UP).getFace(BlockFace.UP), teamKind);
			setBlock(block.getFace(BlockFace.UP).getFace(BlockFace.UP), teamKind);
		}
	}
	
	private void placeGate(Block block,
			Material material) {
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
			block.getFace(BlockFace.DOWN).setType(Material.GLOWSTONE);
			setBlock(block.getFace(leftSide), material);
			setBlock(block.getFace(rightSide).getFace(BlockFace.UP), material);
			setBlock(block.getFace(leftSide).getFace(BlockFace.UP).getFace(BlockFace.UP), material);
			setBlock(block.getFace(rightSide), material);
			setBlock(block.getFace(leftSide).getFace(BlockFace.UP), material);
			setBlock(block.getFace(rightSide).getFace(BlockFace.UP).getFace(BlockFace.UP), material);
			setBlock(block.getFace(BlockFace.UP).getFace(BlockFace.UP), material);
		}
	}
	
	private void setBlock(Block block, TeamKind kind) {
		block.setType(kind.getMaterial());
		block.setData(kind.getData());
	}
	
	private void setBlock(Block block, Material material) {
		block.setType(material);
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
			List<Team> teams = warzone.getTeams();
			Block autoAssignGateBlock = BlockInfo.getBlock(warzone.getWorld(), autoAssignGate);
			setBlock(autoAssignGateBlock.getFace(BlockFace.DOWN), (Material.GLOWSTONE));
			int size = teams.size();
			int index = 0;
			TeamKind kind = null; 
			if(index >= size) kind = TeamKinds.teamKindFromString("diamond");
			else kind = teams.get(index).getKind();
			setBlock(autoAssignGateBlock.getFace(leftSide), kind);
			index++;
			if(index >= size) kind = TeamKinds.teamKindFromString("iron");
			else kind = teams.get(index).getKind();
			setBlock(autoAssignGateBlock.getFace(leftSide).getFace(BlockFace.UP), kind);
			index++;
			if(index >= size) kind = TeamKinds.teamKindFromString("gold");
			else kind = teams.get(index).getKind();
			setBlock(autoAssignGateBlock.getFace(leftSide).getFace(BlockFace.UP).getFace(BlockFace.UP), kind);
			index++;
			if(index >= size) kind = TeamKinds.teamKindFromString("diamond");
			else kind = teams.get(index).getKind();
			setBlock(autoAssignGateBlock.getFace(BlockFace.UP).getFace(BlockFace.UP), kind);
			index++;
			if(index >= size) kind = TeamKinds.teamKindFromString("iron");
			else kind = teams.get(index).getKind();
			setBlock(autoAssignGateBlock.getFace(rightSide).getFace(BlockFace.UP).getFace(BlockFace.UP), kind);
			index++;
			if(index >= size) kind = TeamKinds.teamKindFromString("gold");
			else kind = teams.get(index).getKind();
			setBlock(autoAssignGateBlock.getFace(rightSide).getFace(BlockFace.UP), kind);
			index++;
			if(index >= size) kind = TeamKinds.teamKindFromString("diamond");
			else kind = teams.get(index).getKind();
			setBlock(autoAssignGateBlock.getFace(rightSide), kind);			
		}
	}

	public boolean isInTeamGate(Team team, Location location) {
		 BlockInfo info = teamGateBlocks.get(team.getName());
		 if(info != null) {
			 if(location.getBlockX() == info.getX()
						&& location.getBlockY() == info.getY()
						&& location.getBlockZ() == info.getZ()) { 
				 return true;
			 }
		 }
		return false;
	}
	
	public boolean isAutoAssignGate(Location location) {
		if(autoAssignGate != null
				&& (location.getBlockX() == autoAssignGate.getX()
					&& location.getBlockY() == autoAssignGate.getY()
					&& location.getBlockZ() == autoAssignGate.getZ()) )
				{
				return true;
			}
		return false;
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
			for(String teamName: teamGateBlocks.keySet()) {
				BlockInfo gateInfo = teamGateBlocks.get(teamName); 
				if(isPartOfGate(BlockInfo.getBlock(warzone.getWorld(), gateInfo), block)) {
					return true;
				}
			}
			if(autoAssignGate != null && isPartOfGate(BlockInfo.getBlock(warzone.getWorld(), autoAssignGate), block)) {
				// auto assign
				return true;
			}
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
						&& block.getZ() == gateBlock.getFace(rightSide).getZ())
					||
					(block.getX() == gateBlock.getX()
							&& block.getY() == gateBlock.getY() - 1
							&& block.getZ() == gateBlock.getZ())
					;
		}
		return false;
	}

	public Warzone getZone() {
		return this.warzone;
	}

	public void resetTeamGateSign(Team team) {
		BlockInfo info = teamGateBlocks.get(team.getName());
		if(info != null) {
			resetTeamGateSign(team, BlockInfo.getBlock(warzone.getWorld(), info));
		}
	}

	private void resetTeamGateSign(Team team, Block gate) {
		if(gate != null) {
			String[] lines = new String[4];
			lines[0] =  "Team " + team.getName();
			lines[1] = team.getPlayers().size() + "/" + warzone.getTeamCap() + " players";
			lines[2] = team.getPoints() + "/" + warzone.getScoreCap() + " pts";
			if(warzone.getLifePool() == -1) {
				lines[3] = "unlimited lives";
			} else {
				lines[3] = team.getRemainingLifes() + "/" + warzone.getLifePool() + " lives left";
			}
			resetGateSign(gate, lines, true);
		}
	}
	
	private void resetGateSign(Block gate, String[] lines, boolean awayFromWall) {
		Block block = null;
		BlockFace direction = null;
		if(awayFromWall) {
			direction = wall;
		} else if (wall == BlockFace.NORTH) {
			direction = BlockFace.SOUTH;
		} else if (wall == BlockFace.EAST) {
			direction = BlockFace.WEST;
		} else if (wall == BlockFace.SOUTH) {
			direction = BlockFace.NORTH;
		} else if (wall == BlockFace.WEST) {
			direction = BlockFace.EAST;
		}
		byte data = 0;
		if(wall == BlockFace.NORTH) {
			block = gate.getFace(direction).getFace(BlockFace.EAST);
			if(awayFromWall) data = (byte)4;
			else data = (byte)12;
		} else if(wall == BlockFace.EAST) {
			block = gate.getFace(direction).getFace(BlockFace.SOUTH);
			if(awayFromWall) data = (byte)8;
			else data = (byte)0;
		} else if(wall == BlockFace.SOUTH) {
			block = gate.getFace(direction).getFace(BlockFace.WEST);
			if(awayFromWall) data = (byte)12;
			else data = (byte)4;
		} else if(wall == BlockFace.WEST) {
			block = gate.getFace(direction).getFace(BlockFace.NORTH);
			if(awayFromWall) data = (byte)0;
			else data = (byte)8;
		}		
	
		SignHelper.setToSign(war, block, data, lines);
	}
	
	public boolean isLeavingZone(Location location) {
		
		BlockFace inside = null;
		BlockFace left = null;
		BlockFace right = null;
		if (wall == BlockFace.NORTH) {
			inside = BlockFace.SOUTH;
			left = BlockFace.WEST;
			right = BlockFace.EAST;
		} else if (wall == BlockFace.EAST) {
			inside = BlockFace.WEST;
			left = BlockFace.NORTH;
			right = BlockFace.SOUTH;
		} else if (wall == BlockFace.SOUTH) {
			inside = BlockFace.NORTH;
			left = BlockFace.EAST;
			right = BlockFace.WEST;
		} else if (wall == BlockFace.WEST) {
			inside = BlockFace.EAST;
			left = BlockFace.SOUTH;
			right = BlockFace.NORTH;
		}		
		if(autoAssignGate != null){
			if(leaving(location, BlockInfo.getBlock(warzone.getWorld(), autoAssignGate), inside, left, right)) return true;
		}
		for(String teamName : teamGateBlocks.keySet()) {
			
			BlockInfo info = teamGateBlocks.get(teamName);
			if(leaving(location, BlockInfo.getBlock(warzone.getWorld(), info), inside, left, right)) return true;
		}
		return false;
	}

	private boolean leaving(Location location, Block gate, BlockFace inside,
			BlockFace left, BlockFace right) {
//		int x = location.getBlockX();
//		int y = location.getBlockY();
//		int z = location.getBlockZ();
//		
		// 3x4x1 deep
		Volume gateExitVolume = new Volume("tempGateExit", 	war, location.getWorld());
		Block out = gate.getFace(inside);
		gateExitVolume.setCornerOne(out.getFace(left).getFace(BlockFace.DOWN));
		gateExitVolume.setCornerTwo(gate.getFace(right, 1).getFace(BlockFace.UP, 3));
		
		if(gateExitVolume.contains(location)) {
			return true;
		}
		
		// 1 block thick arrow like detection grid:
//		Block out = gate.getFace(inside);
//		Block outL = out.getFace(left);
//		Block outLL = out.getFace(left, 2);
//		Block outR = out.getFace(right);
//		Block outRR = out.getFace(right, 2);
//		Block out2 = gate.getFace(inside, 2);
//		Block out2L = out2.getFace(left);
//		Block out2R = out2.getFace(right);
//		Block out3 = gate.getFace(inside, 3);
//		if(out.getX() == x && out.getY() == y && out.getZ() == z) {
//			return true;
//		} else if(outL.getX() == x && outL.getY() == y && outL.getZ() == z) {
//			return true;
//		} else if(outR.getX() == x && outR.getY() == y && outR.getZ() == z) {
//			return true;
//		} else if(outLL.getX() == x && outLL.getY() == y && outLL.getZ() == z) {
//			return true;
//		} else if(outRR.getX() == x && outRR.getY() == y && outRR.getZ() == z) {
//			return true;
//		} else if(out2.getX() == x && out2.getY() == y && out2.getZ() == z) {
//			return true;
//		} else if(out2L.getX() == x && out2L.getY() == y && out2L.getZ() == z) {
//			return true;
//		} else if(out2R.getX() == x && out2R.getY() == y && out2R.getZ() == z) {
//			return true;
//		} else if(out3.getX() == x && out3.getY() == y && out3.getZ() == z) {
//			return true;
//		}
		return false;
	}
}
