package com.tommytony.war;

import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;

import com.tommytony.war.volumes.CenteredVolume;
import com.tommytony.war.volumes.Volume;

import java.util.ArrayList;
import java.util.List;

public class Team {
	private List<Player> players = new ArrayList<Player>();
	private Location teamSpawn = null;
	private String name;
	private int remainingTickets;
	private int startTickets;
	private int[] oldSpawnState = new int[10];
	private int points = 0;
	private CenteredVolume volume;
	private final War war;
	private final Warzone warzone;
	
	public Team(String name, Location teamSpawn, War war, Warzone warzone) {
		this.war = war;
		this.warzone = warzone;
		this.setName(name);
		this.teamSpawn = teamSpawn;
		this.volume = new CenteredVolume(name, teamSpawn, war, warzone);
	}
	
	public void setTeamSpawn(Location teamSpawn) {
		
		if(teamSpawn != null) volume.resetBlocks();
		this.teamSpawn = teamSpawn;
		Volume newTeamSpawn = new CenteredVolume(name, teamSpawn, war, warzone);
		volume.saveBlocks();
		
		// Set the spawn 
		int x = teamSpawn.getBlockX();
		int y = teamSpawn.getBlockY();
		int z = teamSpawn.getBlockZ();
		
		// first ring
		warzone.getWorld().getBlockAt(x+1, y-1, z+1).setType(Material.LightStone);
		warzone.getWorld().getBlockAt(x+1, y-1, z).setType(Material.LightStone);
		warzone.getWorld().getBlockAt(x+1, y-1, z-1).setType(Material.LightStone);
		warzone.getWorld().getBlockAt(x, y-1, z+1).setType(Material.LightStone);
		warzone.getWorld().getBlockAt(x, y-1, z).setType(Material.Stone);
		warzone.getWorld().getBlockAt(x, y-1, z-1).setType(Material.LightStone);
		warzone.getWorld().getBlockAt(x-1, y-1, z+1).setType(Material.LightStone);
		warzone.getWorld().getBlockAt(x-1, y-1, z).setType(Material.LightStone);
		warzone.getWorld().getBlockAt(x-1, y-1, z-1).setType(Material.LightStone);
		
		// outer ring
		//world.getBlockAt(x+2, y-1, z+2).setType(Material.Stone);
		warzone.getWorld().getBlockAt(x+2, y-1, z+1).setType(Material.Stone);
		warzone.getWorld().getBlockAt(x+2, y-1, z).setType(Material.Stone);
		warzone.getWorld().getBlockAt(x+2, y-1, z-1).setType(Material.Stone);
		//world.getBlockAt(x+2, y-1, z-2).setType(Material.Stone);
		
		warzone.getWorld().getBlockAt(x-1, y-1, z+2).setType(Material.Stone);
		warzone.getWorld().getBlockAt(x-1, y-1, z-2).setType(Material.Stone);
		
		warzone.getWorld().getBlockAt(x, y-1, z+2).setType(Material.Stone);
		warzone.getWorld().getBlockAt(x, y-1, z-2).setType(Material.Stone);
		
		warzone.getWorld().getBlockAt(x+1, y-1, z+2).setType(Material.Stone);
		warzone.getWorld().getBlockAt(x+1, y-1, z-2).setType(Material.Stone);
		
		//world.getBlockAt(x-2, y-1, z+2).setType(Material.Stone);
		warzone.getWorld().getBlockAt(x-2, y-1, z+1).setType(Material.Stone);
		warzone.getWorld().getBlockAt(x-2, y-1, z).setType(Material.Stone);
		warzone.getWorld().getBlockAt(x-2, y-1, z-1).setType(Material.Stone);
		//world.getBlockAt(x-2, y-1, z-2).setType(Material.Stone);
		
		resetSign();

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

	public int[] getOldSpawnState() {
		return oldSpawnState;
	}

	public void setOldSpawnState(int[] oldSpawnState) {
		this.oldSpawnState = oldSpawnState;
	}
	
	public void addPoint() {
		points++;
	}

	public int getPoints() {
		return points;
	}

	public boolean contains(Block block) {
		int x = teamSpawn.getBlockX();
		int y = teamSpawn.getBlockY();
		int z = teamSpawn.getBlockZ();
		int bx = block.getX();
		int by = block.getY();
		int bz = block.getZ();
		if((bx == x && by == y && bz == z) || 
				(bx == x+1 && by == y-1 && bz == z+1) ||
				(bx == x+1 && by == y-1 && bz == z) ||
				(bx == x+1 && by == y-1 && bz == z-1) ||
				(bx == x && by == y-1 && bz == z+1) ||
				(bx == x && by == y-1 && bz == z) ||
				(bx == x && by == y-1 && bz == z-1) ||
				(bx == x-1 && by == y-1 && bz == z+1) ||
				(bx == x-1 && by == y-1 && bz == z) ||
				(bx == x-1 && by == y-1 && bz == z-1) ) {
			return true;
		}
		return false;
	}

	public CenteredVolume getVolume() {
		
		return volume;
	}
	
	
	public void resetSign(){
		int x = teamSpawn.getBlockX();
		int y = teamSpawn.getBlockY();
		int z = teamSpawn.getBlockZ();
		
		Block block = warzone.getWorld().getBlockAt(x, y, z);
		block.setType(Material.SignPost);
		
		BlockState state = block.getState();
		Sign sign = (Sign) state; 
		sign.setLine(0, "Team");
		sign.setLine(1, name);
		sign.setLine(2, points + " pts");
		sign.setLine(3, remainingTickets + "/" + warzone.getLifePool() + " lives left");
	
	}

}
