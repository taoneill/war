package com.tommytony.war;

import org.bukkit.*;
import java.util.ArrayList;
import java.util.List;

public class Team {
	private List<Player> players = new ArrayList<Player>();
	private Location teamSpawn = null;
	private String name;
	private int remainingTickets;
	private int[] oldSpawnState = new int[10];
	private int points = 0;
	
	public Team(String name, Location teamSpawn) {
		this.setName(name);
		this.teamSpawn = teamSpawn;
	}
	
	public void setTeamSpawn(Location teamSpawn) {
		this.teamSpawn = teamSpawn;
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

}
