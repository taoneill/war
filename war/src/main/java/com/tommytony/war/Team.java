package com.tommytony.war;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Block;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Player;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;

import com.tommytony.war.volumes.CenteredVolume;

public class Team {
	private List<Player> players = new ArrayList<Player>();
	private Location teamSpawn = null;
	private String name;
	private int remainingTickets;
	private int points = 0;
	private CenteredVolume volume;
	private final War war;
	private final Warzone warzone;
	
	public Team(String name, Location teamSpawn, War war, Warzone warzone) {
		this.war = war;
		this.warzone = warzone;
		this.setName(name);
		this.teamSpawn = teamSpawn;
		this.volume = new CenteredVolume(name, teamSpawn, 5, war, warzone);
	}
	
	public void setTeamSpawn(Location teamSpawn) {
		
		this.teamSpawn = teamSpawn;
		
		// this resets the block to old state
		volume.changeCenter(teamSpawn);
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
	
	public void addPoint() {
		points++;
	}

	public int getPoints() {
		return points;
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
