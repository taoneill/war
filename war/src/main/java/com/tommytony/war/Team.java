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
	private String name;
	private int remainingTickets;
	private int points = 0;
	private Volume volume;
	private final Warzone warzone;
	private Material material;
	
	public Team(String name, Material material, Location teamSpawn, War war, Warzone warzone) {
		this.warzone = warzone;
		this.setName(name);
		this.teamSpawn = teamSpawn;
		this.setVolume(new Volume(name, war, warzone.getWorld()));
		this.material = material;
		
	}
	
	public Material getMaterial() {
		return material;
	}
	
	private void setVolume() {
		if(volume.isSaved()) volume.resetBlocks();
		int x = teamSpawn.getBlockX();
		int y = teamSpawn.getBlockY();
		int z = teamSpawn.getBlockZ();
		this.volume.setCornerOne(warzone.getWorld().getBlockAt(x-2, y-1, z-2));
		this.volume.setCornerTwo(warzone.getWorld().getBlockAt(x+2, y+5, z+2));
	}
	
	private void initializeTeamSpawn(Location teamSpawn) {
		// make air
		this.volume.setToMaterial(Material.AIR);
		
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
		
		resetSign();
	}
	
	public void setTeamSpawn(Location teamSpawn) {
		
		this.teamSpawn = teamSpawn;
		
		// this resets the block to old state
		this.setVolume();
		getVolume().saveBlocks();
		
		initializeTeamSpawn(teamSpawn);
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

	public void setRemainingTickets(int remainingTickets) {
		this.remainingTickets = remainingTickets;		
	}

	public int getRemainingTickets() {
		return remainingTickets;
	}
	
	public void addPoint() {
		points++;
	}

	public int getPoints() {
		return points;
	}

	public Volume getVolume() {
		
		return volume;
	}
	
	public void resetSign(){
		int x = teamSpawn.getBlockX();
		int y = teamSpawn.getBlockY();
		int z = teamSpawn.getBlockZ();
		
		Block block = warzone.getWorld().getBlockAt(x, y, z).getFace(BlockFace.SOUTH, 2).getFace(BlockFace.WEST, 2);
		if(block.getType() != Material.SIGN_POST) block.setType(Material.SIGN_POST);
		block.setData((byte)6);
		
		BlockState state = block.getState();
		if(state instanceof Sign) {
			Sign sign = (Sign) state;
			sign.setLine(0, "Team " + name);
			sign.setLine(1, remainingTickets + "/" + warzone.getLifePool() + " lives left");
			sign.setLine(2, points + "/" + warzone.getScoreCap() + " pts");
			sign.setLine(3, players.size() + "/" + warzone.getTeamCap() + " players");
			state.update(true);
		}
		
		if(warzone.getLobby() != null) {
			warzone.getLobby().resetTeamGateSign(this);
		}
	}

	public void setVolume(Volume volume) {
		this.volume = volume;
	}

}
