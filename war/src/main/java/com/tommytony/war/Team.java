package com.tommytony.war;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.material.MaterialData;

import bukkit.tommytony.war.War;

import com.tommytony.war.utils.SignHelper;
import com.tommytony.war.volumes.Volume;

/**
 * 
 * @author tommytony
 *
 */
public class Team {
	private List<Player> players = new ArrayList<Player>();
	private Location teamSpawn = null;
	private Location teamFlag = null;
	private String name;
	private int remainingLives;
	private int points = 0;
	private Volume spawnVolume;
	private Volume flagVolume;
	private final Warzone warzone;
	private TeamKind kind;
	private War war;
	
	public Team(String name, TeamKind kind, Location teamSpawn, War war, Warzone warzone) {
		this.warzone = warzone;
		this.setName(name);
		this.teamSpawn = teamSpawn;
		this.war = war;
		this.setSpawnVolume(new Volume(name, war, warzone.getWorld()));
		this.kind = kind;
		this.setFlagVolume(null); // no flag at the start
	}
	
	public TeamKind getKind() {
		return kind;
	}
	
	private void setSpawnVolume() {
		if(spawnVolume.isSaved()) spawnVolume.resetBlocks();
		int x = teamSpawn.getBlockX();
		int y = teamSpawn.getBlockY();
		int z = teamSpawn.getBlockZ();
		if(warzone.getSpawnStyle().equals(TeamSpawnStyles.SMALL)) {
			this.spawnVolume.setCornerOne(warzone.getWorld().getBlockAt(x-1, y-1, z-1));
			this.spawnVolume.setCornerTwo(warzone.getWorld().getBlockAt(x+1, y+3, z+1));
		} else {
			// flat or big
			this.spawnVolume.setCornerOne(warzone.getWorld().getBlockAt(x-2, y-1, z-2));
			this.spawnVolume.setCornerTwo(warzone.getWorld().getBlockAt(x+2, y+3, z+2));
		}
	}
	
	public void initializeTeamSpawn() {
		// make air
		this.spawnVolume.setToMaterial(Material.AIR);
		
		// Set the spawn 
		int x = teamSpawn.getBlockX();
		int y = teamSpawn.getBlockY();
		int z = teamSpawn.getBlockZ();
		
		// first ring
		setBlock(x+1, y-1, z+1, kind);
		setBlock(x+1, y-1, z, kind);
		setBlock(x+1, y-1, z-1, kind);
		setBlock(x, y-1, z+1, kind);
		warzone.getWorld().getBlockAt(x, y-1, z).setType(Material.GLOWSTONE);
		setBlock(x, y-1, z-1, kind);
		setBlock(x-1, y-1, z+1, kind);
		setBlock(x-1, y-1, z, kind);
		setBlock(x-1, y-1, z-1, kind);
		
		// Orientation
		int yaw = 0;
		if(teamSpawn.getYaw() >= 0){
			yaw = (int)(teamSpawn.getYaw() % 360);
		} else {
			yaw = (int)(360 + (teamSpawn.getYaw() % 360));
		}
		Block signBlock = null;
		int signData = 0;
		
		if(warzone.getSpawnStyle().equals(TeamSpawnStyles.SMALL)){
			// SMALL style
			if(yaw >= 0 && yaw < 90) {
				signData = 10;
				signBlock = warzone.getWorld().getBlockAt(x, y, z).getFace(BlockFace.NORTH).getFace(BlockFace.WEST);
			}else if(yaw >= 90 && yaw <= 180) {
				signData = 14;
				signBlock = warzone.getWorld().getBlockAt(x, y, z).getFace(BlockFace.NORTH).getFace(BlockFace.EAST);
			} else if(yaw >= 180 && yaw < 270) {
				signData = 2;
				signBlock = warzone.getWorld().getBlockAt(x, y, z).getFace(BlockFace.SOUTH).getFace(BlockFace.EAST);
			} else if(yaw >= 270 && yaw <= 360) {
				signData = 6;
				signBlock = warzone.getWorld().getBlockAt(x, y, z).getFace(BlockFace.SOUTH).getFace(BlockFace.WEST);
			}
		} else {
			// outer ring (FLAT or BIG)
			setBlock(x+2, y-1, z+2, kind);
			setBlock(x+2, y-1, z+1, kind);
			setBlock(x+2, y-1, z, kind);
			setBlock(x+2, y-1, z-1, kind);
			setBlock(x+2, y-1, z-2, kind);
			
			setBlock(x-1, y-1, z+2, kind);
			setBlock(x-1, y-1, z-2, kind);
			
			setBlock(x, y-1, z+2, kind);
			setBlock(x, y-1, z-2, kind);
			
			setBlock(x+1, y-1, z+2, kind);
			setBlock(x+1, y-1, z-2, kind);
			
			setBlock(x-2, y-1, z+2, kind);
			setBlock(x-2, y-1, z+1, kind);
			setBlock(x-2, y-1, z, kind);
			setBlock(x-2, y-1, z-1, kind);
			setBlock(x-2, y-1, z-2, kind);
			
			BlockFace facing = null;
			BlockFace opposite = null;
			if(yaw >= 0 && yaw < 90) {
				facing = BlockFace.NORTH_WEST;
				opposite = BlockFace.SOUTH_EAST;
				signData = 10;
				signBlock = warzone.getWorld().getBlockAt(x, y, z).getFace(BlockFace.NORTH, 2).getFace(BlockFace.WEST, 2);
				
				if(warzone.getSpawnStyle().equals(TeamSpawnStyles.BIG)) {
					// rim
					setBlock(x-2, y, z-1, kind);
					setBlock(x-2, y, z-2, kind);
					setBlock(x-1, y, z-2, kind);
					setBlock(x, y, z-2, kind);
					setBlock(x+1, y, z-2, kind);
					setBlock(x+2, y, z-2, kind);
					setBlock(x+2, y, z-1, kind);
					setBlock(x+2, y, z, kind);
					setBlock(x+2, y, z+1, kind);
					setBlock(x+2, y, z+2, kind);
					setBlock(x+1, y, z+2, kind);
					
					// tower
					setBlock(x, y+1, z-2, kind);
					setBlock(x+1, y+1, z-2, kind);
					setBlock(x+2, y+1, z-2, kind);
					setBlock(x+2, y+1, z-1, kind);
					setBlock(x+2, y+1, z, kind);
					
					setBlock(x+1, y+2, z-2, kind);
					setBlock(x+2, y+2, z-2, kind);
					setBlock(x+2, y+2, z-1, kind);
					
					setBlock(x+2, y+3, z-2, kind);
				}
			} else if(yaw >= 90 && yaw <= 180) {
				facing = BlockFace.NORTH_EAST;
				opposite = BlockFace.SOUTH_WEST;
				signData = 14;
				signBlock = warzone.getWorld().getBlockAt(x, y, z).getFace(BlockFace.NORTH, 2).getFace(BlockFace.EAST, 2);
				if(warzone.getSpawnStyle().equals(TeamSpawnStyles.BIG)) {
					// rim
					setBlock(x+1, y, z-2, kind);
					setBlock(x+2, y, z-2, kind);
					setBlock(x+2, y, z-1, kind);
					setBlock(x+2, y, z, kind);
					setBlock(x+2, y, z+1, kind);
					setBlock(x+2, y, z+2, kind);
					setBlock(x+1, y, z+2, kind);
					setBlock(x, y, z+2, kind);
					setBlock(x-1, y, z+2, kind);
					setBlock(x-2, y, z+2, kind);
					setBlock(x-2, y, z+1, kind);
					
					// tower
					setBlock(x+2, y+1, z, kind);
					setBlock(x+2, y+1, z+1, kind);
					setBlock(x+2, y+1, z+2, kind);
					setBlock(x+1, y+1, z+2, kind);
					setBlock(x, y+1, z+2, kind);
					
					setBlock(x+2, y+2, z+1, kind);
					setBlock(x+2, y+2, z+2, kind);
					setBlock(x+1, y+2, z+2, kind);
					
					setBlock(x+2, y+3, z+2, kind);
				}
			} else if(yaw >= 180 && yaw < 270) {
				facing = BlockFace.SOUTH_EAST;
				opposite = BlockFace.NORTH_WEST;
				signData = 2;
				signBlock = warzone.getWorld().getBlockAt(x, y, z).getFace(BlockFace.SOUTH, 2).getFace(BlockFace.EAST, 2);
				if(warzone.getSpawnStyle().equals(TeamSpawnStyles.BIG)) {
					// rim
					setBlock(x+2, y, z+1, kind);
					setBlock(x+2, y, z+2, kind);
					setBlock(x+1, y, z+2, kind);
					setBlock(x, y, z+2, kind);
					setBlock(x-1, y, z+2, kind);
					setBlock(x-2, y, z+2, kind);
					setBlock(x-2, y, z+1, kind);
					setBlock(x-2, y, z, kind);
					setBlock(x-2, y, z-1, kind);
					setBlock(x-2, y, z-2, kind);
					setBlock(x-1, y, z-2, kind);
					
					// tower
					setBlock(x, y+1, z+2, kind);
					setBlock(x-1, y+1, z+2, kind);
					setBlock(x-2, y+1, z+2, kind);
					setBlock(x-2, y+1, z+1, kind);
					setBlock(x-2, y+1, z, kind);
					
					setBlock(x-1, y+2, z+2, kind);
					setBlock(x-2, y+2, z+2, kind);
					setBlock(x-2, y+2, z+1, kind);
					
					setBlock(x-2, y+3, z+2, kind);
				}
			} else if(yaw >= 270 && yaw <= 360) {
				facing = BlockFace.SOUTH_WEST;
				opposite = BlockFace.NORTH_EAST;
				signData = 6;
				signBlock = warzone.getWorld().getBlockAt(x, y, z).getFace(BlockFace.SOUTH, 2).getFace(BlockFace.WEST, 2);
				if(warzone.getSpawnStyle().equals(TeamSpawnStyles.BIG)) {
					// rim
					setBlock(x-1, y, z+2, kind);
					setBlock(x-2, y, z+2, kind);
					setBlock(x-2, y, z+1, kind);
					setBlock(x-2, y, z, kind);
					setBlock(x-2, y, z-1, kind);
					setBlock(x-2, y, z-2, kind);
					setBlock(x-1, y, z-2, kind);
					setBlock(x, y, z-2, kind);
					setBlock(x+1, y, z-2, kind);
					setBlock(x+2, y, z-2, kind);
					setBlock(x+2, y, z-1, kind);
					
					// tower
					setBlock(x-2, y+1, z, kind);
					setBlock(x-2, y+1, z-1, kind);
					setBlock(x-2, y+1, z-2, kind);
					setBlock(x-1, y+1, z-2, kind);
					setBlock(x, y+1, z-2, kind);
					
					setBlock(x-2, y+2, z-1, kind);
					setBlock(x-2, y+2, z-2, kind);
					setBlock(x-1, y+2, z-2, kind);
					
					setBlock(x-2, y+3, z-2, kind);
				}
			} 
		}	
			
		if(signBlock != null) {
//			if(signBlock.getType() != Material.SIGN_POST) { 
//				signBlock.setType(Material.SIGN_POST);
//			} 
//			else {
//				// already a signpost, gotta delete it and create a new one
//				signBlock.setType(Material.AIR);
//				signBlock.setType(Material.SIGN_POST);
//			}
			
			String[] lines = new String[4];
			lines[0] = "Team " + name;
			lines[1] = remainingLives + "/" + warzone.getLifePool() + " lives left";
			lines[2] = points + "/" + warzone.getScoreCap() + " pts";
			lines[3] = players.size() + "/" + warzone.getTeamCap() + " players";
			SignHelper.setToSign(war, signBlock, (byte)signData, lines);
		}
	}
	
	private void setBlock(int x, int y, int z, TeamKind kind) {
		Block block = warzone.getWorld().getBlockAt(x, y, z);
		block.setType(kind.getMaterial());
		block.setData(kind.getData());		
	}
	
	public void setTeamSpawn(Location teamSpawn) {
		
		this.teamSpawn = teamSpawn;
		
		// this resets the block to old state
		this.setSpawnVolume();
		getSpawnVolume().saveBlocks();
		
		initializeTeamSpawn();
	}
	
	public Location getTeamSpawn() {
		return teamSpawn;
	}
	
	public void addPlayer(Player player) {
		this.players.add(player);
	}
	
	public List<Player> getPlayers() {
		return players;
	}
	
	public void teamcast(String message) {
		for(Player player : players) {
			war.msg(player, message);
		}
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public boolean removePlayer(String name) {
		Player thePlayer = null;
		for(Player player : players) {
			if(player.getName().equals(name)) {
				thePlayer = player; 
			}
		}
		if(thePlayer != null) {
			players.remove(thePlayer);
			return true;
		}
		return false;
	}

	public void setRemainingLives(int remainingLives) {
		this.remainingLives = remainingLives;		
	}

	public int getRemainingLifes() {
		return remainingLives;
	}
	
	public void addPoint() {
		if (players.size()!=0) points++;
	}

	public int getPoints() {
		return points;
	}

	public Volume getSpawnVolume() {
		
		return spawnVolume;
	}
	
	public void resetSign(){
		this.getSpawnVolume().resetBlocks();
		this.initializeTeamSpawn(); // reset everything instead of just sign
		
		// BUKKIT
//		int x = teamSpawn.getBlockX();
//		int y = teamSpawn.getBlockY();
//		int z = teamSpawn.getBlockZ();
//		
//		Block block = warzone.getWorld().getBlockAt(x, y, z).getFace(BlockFace.SOUTH, 2).getFace(BlockFace.WEST, 2);
//		if(block.getType() != Material.SIGN_POST) { 
//			block.setType(Material.SIGN_POST);
//		} 
////		else {
////			// already a signpost, gotta delete it and create a new one
////			block.setType(Material.AIR);
////			block.setType(Material.SIGN_POST);
////		}
////		block.setData((byte)6);
//		
//		BlockState state = block.getState();
//		if(state instanceof Sign) {
//			Sign sign = (Sign) state;
//			sign.setType(Material.SIGN_POST);
//			sign.setData(new MaterialData(Material.SIGN_POST, (byte)6));
//			sign.setLine(0, "Team " + name);
//			sign.setLine(1, remainingTickets + "/" + warzone.getLifePool() + " lives left");
//			sign.setLine(2, points + "/" + warzone.getScoreCap() + " pts");
//			sign.setLine(3, players.size() + "/" + warzone.getTeamCap() + " players");
//			state.update(true);
//		}
		
		if(warzone.getLobby() != null) {
			warzone.getLobby().resetTeamGateSign(this);
		}
	}

	public void setSpawnVolume(Volume volume) {
		this.spawnVolume = volume;
	}

	public void setPoints(int score) {
		if (players.size()!=0) this.points = score;
	}

	public void setFlagVolume(Volume flagVolume) {
		this.flagVolume = flagVolume;
	}

	public Volume getFlagVolume() {
		return flagVolume;
	}
	
	private void setFlagVolume() {
		if(flagVolume == null) flagVolume = new Volume(getName() + "flag", war, warzone.getWorld());
		if(flagVolume.isSaved()) flagVolume.resetBlocks();
		int x = teamFlag.getBlockX();
		int y = teamFlag.getBlockY();
		int z = teamFlag.getBlockZ();
		this.flagVolume.setCornerOne(warzone.getWorld().getBlockAt(x-1, y-1, z-1));
		this.flagVolume.setCornerTwo(warzone.getWorld().getBlockAt(x+1, y+3, z+1));
	}

	public void initializeTeamFlag() {
		// make air
		this.flagVolume.setToMaterial(Material.AIR);
		
		// Set the flag blocks
		int x = teamFlag.getBlockX();
		int y = teamFlag.getBlockY();
		int z = teamFlag.getBlockZ();
		
		// first ring
		warzone.getWorld().getBlockAt(x+1, y-1, z+1).setType(Material.OBSIDIAN);
		warzone.getWorld().getBlockAt(x+1, y-1, z).setType(Material.OBSIDIAN);
		warzone.getWorld().getBlockAt(x+1, y-1, z-1).setType(Material.OBSIDIAN);
		warzone.getWorld().getBlockAt(x, y-1, z+1).setType(Material.OBSIDIAN);
		warzone.getWorld().getBlockAt(x, y-1, z).setType(Material.GLOWSTONE);
		warzone.getWorld().getBlockAt(x, y-1, z-1).setType(Material.OBSIDIAN);
		warzone.getWorld().getBlockAt(x-1, y-1, z+1).setType(Material.OBSIDIAN);
		warzone.getWorld().getBlockAt(x-1, y-1, z).setType(Material.OBSIDIAN);
		warzone.getWorld().getBlockAt(x-1, y-1, z-1).setType(Material.OBSIDIAN);
		
		// flag
		warzone.getWorld().getBlockAt(x, y+1, z).setType(kind.getMaterial());
		warzone.getWorld().getBlockAt(x, y+1, z).setData(kind.getData());
		warzone.getWorld().getBlockAt(x, y+2, z).setType(Material.FENCE);
		
		// Flag post using Orientation
		int yaw = 0;
		if(teamFlag.getYaw() >= 0){
			yaw = (int)(teamFlag.getYaw() % 360);
		} else {
			yaw = (int)(360 + (teamFlag.getYaw() % 360));
		}
		BlockFace facing = null;
		BlockFace opposite = null;
		if((yaw >= 0 && yaw < 45) || (yaw >= 315 && yaw <= 360)) {
			facing = BlockFace.WEST;
			opposite = BlockFace.EAST;
			warzone.getWorld().getBlockAt(x, y, z-1).setType(Material.FENCE);
			warzone.getWorld().getBlockAt(x, y+1, z-1).setType(Material.FENCE);
			warzone.getWorld().getBlockAt(x, y+2, z-1).setType(Material.FENCE);
		} else if(yaw >= 45 && yaw < 135) {
			facing = BlockFace.NORTH;
			opposite = BlockFace.SOUTH;
			warzone.getWorld().getBlockAt(x+1, y, z).setType(Material.FENCE);
			warzone.getWorld().getBlockAt(x+1, y+1, z).setType(Material.FENCE);
			warzone.getWorld().getBlockAt(x+1, y+2, z).setType(Material.FENCE);
		} else if(yaw >= 135 && yaw < 225) {
			facing = BlockFace.EAST;
			opposite = BlockFace.WEST;
			warzone.getWorld().getBlockAt(x, y, z+1).setType(Material.FENCE);
			warzone.getWorld().getBlockAt(x, y+1, z+1).setType(Material.FENCE);
			warzone.getWorld().getBlockAt(x, y+2, z+1).setType(Material.FENCE);
		} else if(yaw >= 225 && yaw < 315) {
			facing = BlockFace.SOUTH;
			opposite = BlockFace.NORTH;
			warzone.getWorld().getBlockAt(x-1, y, z).setType(Material.FENCE);
			warzone.getWorld().getBlockAt(x-1, y+1, z).setType(Material.FENCE);
			warzone.getWorld().getBlockAt(x-1, y+2, z).setType(Material.FENCE);
		}
	}
	
	public void setTeamFlag(Location teamFlag) {
		
		this.teamFlag = teamFlag;
		
		// this resets the block to old state
		this.setFlagVolume();
		getFlagVolume().saveBlocks();
		
		initializeTeamFlag();
	}
	
	public boolean isTeamFlagBlock(Block block) {
		if(teamFlag != null) {
			int flagX = teamFlag.getBlockX();
			int flagY = teamFlag.getBlockY() + 1;
			int flagZ = teamFlag.getBlockZ();
			if(block.getX() == flagX
					&& block.getY() == flagY
					&& block.getZ() == flagZ) {
				return true;
			}
		}
		return false;
	}

	public Location getTeamFlag() {
		
		return teamFlag;
	}
}
