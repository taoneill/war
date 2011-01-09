package com.tommytony.war;

import org.bukkit.*;

import com.tommytony.war.volumes.CenteredVolume;
import com.tommytony.war.volumes.Volume;

public class Monument {
	private Location location;
	private int[] initialState = new int[26];
	private CenteredVolume volume;
	
	private Team ownerTeam = null;
	private final String name;
	private Warzone warzone;
	
	public Monument(String name, War war, Warzone warzone, Location location) {
		this.name = name;
		this.location = location;
		this.warzone = warzone;
		int x = location.getBlockX();
		int y = location.getBlockY();
		int z = location.getBlockZ();
		volume = new CenteredVolume("name", location, war, warzone);
		volume.setSideSize(5);
		volume.saveBlocks();
		
		this.addMonumentBlocks();
	}
	
	public void addMonumentBlocks() {
		// TODO Auto-generated method stub
		this.ownerTeam = null;
		int x = location.getBlockX();
		int y = location.getBlockY();
		int z = location.getBlockZ();
		
		// center
		warzone.getWorld().getBlockAt(x, y, z).setType(Material.Air);
		warzone.getWorld().getBlockAt(x, y-1, z).setType(Material.Soil);
		
		// inner ring
		warzone.getWorld().getBlockAt(x+1, y-1, z+1).setType(Material.CoalOre);
		warzone.getWorld().getBlockAt(x+1, y-1, z).setType(Material.CoalOre);
		warzone.getWorld().getBlockAt(x+1, y-1, z-1).setType(Material.CoalOre);
		
		warzone.getWorld().getBlockAt(x, y-1, z+1).setType(Material.CoalOre);		
		warzone.getWorld().getBlockAt(x, y-1, z-1).setType(Material.CoalOre);
		
		warzone.getWorld().getBlockAt(x-1, y-1, z+1).setType(Material.CoalOre);
		warzone.getWorld().getBlockAt(x-1, y-1, z).setType(Material.CoalOre);
		warzone.getWorld().getBlockAt(x-1, y-1, z-1).setType(Material.CoalOre);
	}

	public boolean isNear(Location playerLocation) {
		int x = location.getBlockX();
		int y = location.getBlockY();
		int z = location.getBlockZ();
		int playerX = (int)playerLocation.getBlockX();
		int playerY = (int)playerLocation.getBlockY();
		int playerZ = (int)playerLocation.getBlockZ();
		int diffX = Math.abs(playerX - x);
		int diffY = Math.abs(playerY - y);
		int diffZ = Math.abs(playerZ - z);
		if(diffX < 6 && diffY < 6 && diffZ < 6) {
			return true;
		}
		return false;
	}
	
	public boolean isOwner(Team team) {
		if(team == ownerTeam) {
			return true;
		}
		return false;
	}
	
	public boolean hasOwner() {
		return ownerTeam != null;
	}
	
	public void ignite(Team team) {
		ownerTeam = team;
		int x = location.getBlockX();
		int y = location.getBlockY();
		int z = location.getBlockZ();
		
		warzone.getWorld().getBlockAt(x+1, y-1, z+1).setType(Material.GlowingRedstoneOre);
		warzone.getWorld().getBlockAt(x+1, y-1, z).setType(Material.GlowingRedstoneOre);
		warzone.getWorld().getBlockAt(x+1, y-1, z-1).setType(Material.GlowingRedstoneOre);
		
		warzone.getWorld().getBlockAt(x, y-1, z+1).setType(Material.GlowingRedstoneOre);		
		warzone.getWorld().getBlockAt(x, y-1, z-1).setType(Material.GlowingRedstoneOre);
		
		warzone.getWorld().getBlockAt(x-1, y-1, z+1).setType(Material.GlowingRedstoneOre);
		warzone.getWorld().getBlockAt(x-1, y-1, z).setType(Material.GlowingRedstoneOre);
		warzone.getWorld().getBlockAt(x-1, y-1, z-1).setType(Material.GlowingRedstoneOre);
	}
	
	public void smother() {
		ownerTeam = null;
		int x = location.getBlockX();
		int y = location.getBlockY();
		int z = location.getBlockZ();
		warzone.getWorld().getBlockAt(x+1, y-1, z+1).setType(Material.CoalOre);
		warzone.getWorld().getBlockAt(x+1, y-1, z).setType(Material.CoalOre);
		warzone.getWorld().getBlockAt(x+1, y-1, z-1).setType(Material.CoalOre);
		
		warzone.getWorld().getBlockAt(x, y-1, z+1).setType(Material.CoalOre);		
		warzone.getWorld().getBlockAt(x, y-1, z-1).setType(Material.CoalOre);
		
		warzone.getWorld().getBlockAt(x-1, y-1, z+1).setType(Material.CoalOre);
		warzone.getWorld().getBlockAt(x-1, y-1, z).setType(Material.CoalOre);
		warzone.getWorld().getBlockAt(x-1, y-1, z-1).setType(Material.CoalOre);
		
	}
	
	public void remove() {
		volume.resetBlocks();
	}

	public Location getLocation() {
		return location;
	}

	public void setOwnerTeam(Team team) {
		this.ownerTeam = team;
		
	}

	public String getName() {
		return name;
	}

	public void setLocation(Location location) {
		this.location = location;
		int x = location.getBlockX();
		int y = location.getBlockY();
		int z = location.getBlockZ();
		
		((CenteredVolume)volume).setCenter(warzone.getWorld().getBlockAt(x, y, z)); // resets the volume blocks
		this.addMonumentBlocks();
	}

	public Volume getVolume() {
		// TODO Auto-generated method stub
		return volume;
	}

	
}
