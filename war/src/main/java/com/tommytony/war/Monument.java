package com.tommytony.war;

import org.bukkit.Location;
import org.bukkit.Material;

import bukkit.tommytony.war.War;

import com.tommytony.war.volumes.CenteredVolume;
import com.tommytony.war.volumes.Volume;

/**
 * 
 * @author tommytony
 *
 */
public class Monument {
	private Location location;
	private CenteredVolume volume;
	
	private Team ownerTeam = null;
	private final String name;
	private Warzone warzone;
	
	public Monument(String name, War war, Warzone warzone, Location location) {
		this.name = name;
		this.location = location;
		this.warzone = warzone;
		volume = new CenteredVolume("name", 
						warzone.getWorld().getBlockAt(location.getBlockX(), 
												location.getBlockY() + 2, 
												location.getBlockZ()), 
						7, war, warzone);
		volume.saveBlocks();
		this.addMonumentBlocks();
	}
	
	public void addMonumentBlocks() {
		this.ownerTeam = null;
		int x = location.getBlockX();
		int y = location.getBlockY();
		int z = location.getBlockZ();
		
		// center
		warzone.getWorld().getBlockAt(x, y-1, z).getState().setType(Material.Air);
		warzone.getWorld().getBlockAt(x, y-2, z).setType(Material.LightStone);
		
		// inner ring
		warzone.getWorld().getBlockAt(x+1, y-1, z+1).setType(Material.LightStone);
		warzone.getWorld().getBlockAt(x+1, y-1, z).setType(Material.Obsidian);
		warzone.getWorld().getBlockAt(x+1, y-1, z-1).setType(Material.LightStone);
		
		warzone.getWorld().getBlockAt(x, y-1, z+1).setType(Material.Obsidian);
		warzone.getWorld().getBlockAt(x, y-1, z-1).setType(Material.Obsidian);
		
		warzone.getWorld().getBlockAt(x-1, y-1, z+1).setType(Material.LightStone);
		warzone.getWorld().getBlockAt(x-1, y-1, z).setType(Material.Obsidian);
		warzone.getWorld().getBlockAt(x-1, y-1, z-1).setType(Material.LightStone);
		
		// outer ring
		
		warzone.getWorld().getBlockAt(x+2, y-1, z+2).setType(Material.Obsidian);
		warzone.getWorld().getBlockAt(x+2, y-1, z+1).setType(Material.Obsidian);
		warzone.getWorld().getBlockAt(x+2, y-1, z).setType(Material.Obsidian);
		warzone.getWorld().getBlockAt(x+2, y-1, z-1).setType(Material.Obsidian);
		warzone.getWorld().getBlockAt(x+2, y-1, z-2).setType(Material.Obsidian);
		
		warzone.getWorld().getBlockAt(x-1, y-1, z+2).setType(Material.Obsidian);
		warzone.getWorld().getBlockAt(x-1, y-1, z-2).setType(Material.Obsidian);
		
		warzone.getWorld().getBlockAt(x, y-1, z+2).setType(Material.Obsidian);
		warzone.getWorld().getBlockAt(x, y-1, z-2).setType(Material.Obsidian);
		
		warzone.getWorld().getBlockAt(x+1, y-1, z+2).setType(Material.Obsidian);
		warzone.getWorld().getBlockAt(x+1, y-1, z-2).setType(Material.Obsidian);
		
		warzone.getWorld().getBlockAt(x-2, y-1, z+2).setType(Material.Obsidian);
		warzone.getWorld().getBlockAt(x-2, y-1, z+1).setType(Material.Obsidian);
		warzone.getWorld().getBlockAt(x-2, y-1, z).setType(Material.Obsidian);
		warzone.getWorld().getBlockAt(x-2, y-1, z-1).setType(Material.Obsidian);
		warzone.getWorld().getBlockAt(x-2, y-1, z-2).setType(Material.Obsidian);
		
		// towers
		warzone.getWorld().getBlockAt(x-2, y, z-1).setType(Material.Obsidian);
		warzone.getWorld().getBlockAt(x-2, y, z-2).setType(Material.Obsidian);
		warzone.getWorld().getBlockAt(x-1, y, z-2).setType(Material.Obsidian);
		warzone.getWorld().getBlockAt(x-2, y+1, z-2).setType(Material.Obsidian);
		
		warzone.getWorld().getBlockAt(x+2, y, z+1).setType(Material.Obsidian);
		warzone.getWorld().getBlockAt(x+2, y, z+2).setType(Material.Obsidian);
		warzone.getWorld().getBlockAt(x+1, y, z+2).setType(Material.Obsidian);
		warzone.getWorld().getBlockAt(x+2, y+1, z+2).setType(Material.Obsidian);
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
	
	public void capture(Team team) {
		ownerTeam = team;
		warzone.getWorld().getBlockAt(location.getBlockX(), location.getBlockY(), location.getBlockZ()).setType(team.getMaterial());
	}
	
	public void uncapture() {
		ownerTeam = null;
		warzone.getWorld().getBlockAt(location.getBlockX(), location.getBlockY(), location.getBlockZ()).setType(Material.Obsidian);
		
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
		volume.changeCenter(location);
		this.addMonumentBlocks();
	}

	public Volume getVolume() {
		return volume;
	}

	
}
