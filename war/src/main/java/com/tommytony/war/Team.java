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
	private Material material;
	private War war;
	
	public Team(String name, Material material, Location teamSpawn, War war, Warzone warzone) {
		this.warzone = warzone;
		this.setName(name);
		this.teamSpawn = teamSpawn;
		this.war = war;
		this.setSpawnVolume(new Volume(name, war, warzone.getWorld()));
		this.material = material;
		this.setFlagVolume(null); // no flag at the start
	}
	
	public Material getMaterial() {
		return material;
	}
	
	private void setSpawnVolume() {
		if(spawnVolume.isSaved()) spawnVolume.resetBlocks();
		int x = teamSpawn.getBlockX();
		int y = teamSpawn.getBlockY();
		int z = teamSpawn.getBlockZ();
		this.spawnVolume.setCornerOne(warzone.getWorld().getBlockAt(x-2, y-1, z-2));
		this.spawnVolume.setCornerTwo(warzone.getWorld().getBlockAt(x+2, y+5, z+2));
	}
	
	public void initializeTeamSpawn() {
		// make air
		this.spawnVolume.setToMaterial(Material.AIR);
		
		// Set the spawn 
		int x = teamSpawn.getBlockX();
		int y = teamSpawn.getBlockY();
		int z = teamSpawn.getBlockZ();
		
		// first ring
		warzone.getWorld().getBlockAt(x+1, y-1, z+1).setType(material);
		warzone.getWorld().getBlockAt(x+1, y-1, z).setType(material);
		warzone.getWorld().getBlockAt(x+1, y-1, z-1).setType(material);
		warzone.getWorld().getBlockAt(x, y-1, z+1).setType(material);
		warzone.getWorld().getBlockAt(x, y-1, z).setType(Material.GLOWSTONE);
		warzone.getWorld().getBlockAt(x, y-1, z-1).setType(material);
		warzone.getWorld().getBlockAt(x-1, y-1, z+1).setType(material);
		warzone.getWorld().getBlockAt(x-1, y-1, z).setType(material);
		warzone.getWorld().getBlockAt(x-1, y-1, z-1).setType(material);
		
		// outer ring
		warzone.getWorld().getBlockAt(x+2, y-1, z+2).setType(material);
		warzone.getWorld().getBlockAt(x+2, y-1, z+1).setType(material);
		warzone.getWorld().getBlockAt(x+2, y-1, z).setType(material);
		warzone.getWorld().getBlockAt(x+2, y-1, z-1).setType(material);
		warzone.getWorld().getBlockAt(x+2, y-1, z-2).setType(material);
		
		warzone.getWorld().getBlockAt(x-1, y-1, z+2).setType(material);
		warzone.getWorld().getBlockAt(x-1, y-1, z-2).setType(material);
		
		warzone.getWorld().getBlockAt(x, y-1, z+2).setType(material);
		warzone.getWorld().getBlockAt(x, y-1, z-2).setType(material);
		
		warzone.getWorld().getBlockAt(x+1, y-1, z+2).setType(material);
		warzone.getWorld().getBlockAt(x+1, y-1, z-2).setType(material);
		
		warzone.getWorld().getBlockAt(x-2, y-1, z+2).setType(material);
		warzone.getWorld().getBlockAt(x-2, y-1, z+1).setType(material);
		warzone.getWorld().getBlockAt(x-2, y-1, z).setType(material);
		warzone.getWorld().getBlockAt(x-2, y-1, z-1).setType(material);
		warzone.getWorld().getBlockAt(x-2, y-1, z-2).setType(material);
		
		// rim
		warzone.getWorld().getBlockAt(x-1, y, z+2).setType(material);
		warzone.getWorld().getBlockAt(x-2, y, z+2).setType(material);
		warzone.getWorld().getBlockAt(x-2, y, z+1).setType(material);
		warzone.getWorld().getBlockAt(x-2, y, z).setType(material);
		warzone.getWorld().getBlockAt(x-2, y, z-1).setType(material);
		warzone.getWorld().getBlockAt(x-2, y, z-2).setType(material);
		warzone.getWorld().getBlockAt(x-1, y, z-2).setType(material);
		warzone.getWorld().getBlockAt(x, y, z-2).setType(material);
		warzone.getWorld().getBlockAt(x+1, y, z-2).setType(material);
		warzone.getWorld().getBlockAt(x+2, y, z-2).setType(material);
		warzone.getWorld().getBlockAt(x+2, y, z-1).setType(material);
		
		// tower
		//warzone.getWorld().getBlockAt(x-2, y+1, z+1).setType(material);
		warzone.getWorld().getBlockAt(x-2, y+1, z).setType(material);
		warzone.getWorld().getBlockAt(x-2, y+1, z-1).setType(material);
		warzone.getWorld().getBlockAt(x-2, y+1, z-2).setType(material);
		warzone.getWorld().getBlockAt(x-1, y+1, z-2).setType(material);
		warzone.getWorld().getBlockAt(x, y+1, z-2).setType(material);
		//warzone.getWorld().getBlockAt(x+1, y+1, z-2).setType(material);
		
		//warzone.getWorld().getBlockAt(x-2, y+2, z).setType(material);
		warzone.getWorld().getBlockAt(x-2, y+2, z-1).setType(material);
		warzone.getWorld().getBlockAt(x-2, y+2, z-2).setType(material);
		warzone.getWorld().getBlockAt(x-1, y+2, z-2).setType(material);
		//warzone.getWorld().getBlockAt(x, y+2, z-2).setType(material);
		
		//warzone.getWorld().getBlockAt(x-2, y+3, z-2).setType(Material.LightStone);
		warzone.getWorld().getBlockAt(x-2, y+3, z-2).setType(material);
		
		// BUKKIT
		//resetSign();
		
		Block block = warzone.getWorld().getBlockAt(x, y, z).getFace(BlockFace.SOUTH, 2).getFace(BlockFace.WEST, 2);
		if(block.getType() != Material.SIGN_POST) { 
			block.setType(Material.SIGN_POST);
		} 
		else {
			// already a signpost, gotta delete it and create a new one
			block.setType(Material.AIR);
			block.setType(Material.SIGN_POST);
		}
		block.setData((byte)6);
		
		BlockState state = block.getState();
		if(state instanceof Sign) {
			Sign sign = (Sign) state;
			sign.setType(Material.SIGN_POST);
			sign.setData(new MaterialData(Material.SIGN_POST, (byte)6));
			sign.setLine(0, "Team " + name);
			sign.setLine(1, remainingLives + "/" + warzone.getLifePool() + " lives left");
			sign.setLine(2, points + "/" + warzone.getScoreCap() + " pts");
			sign.setLine(3, players.size() + "/" + warzone.getTeamCap() + " players");
			state.update(true);
		}
		
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
			player.sendMessage(message);
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
		points++;
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
		this.points = score;
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
			
		// flag post
		warzone.getWorld().getBlockAt(x, y, z-1).setType(Material.FENCE);
		warzone.getWorld().getBlockAt(x, y+1, z-1).setType(Material.FENCE);
		warzone.getWorld().getBlockAt(x, y+2, z-1).setType(Material.FENCE);
		warzone.getWorld().getBlockAt(x, y+2, z).setType(Material.FENCE);
		warzone.getWorld().getBlockAt(x, y+1, z).setType(material);
		
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
