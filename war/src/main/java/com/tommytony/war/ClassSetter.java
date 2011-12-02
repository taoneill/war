package com.tommytony.war;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import com.tommytony.war.volumes.Volume;

/**
 * Defines almost everything having to do with the ClassSetter in war
 * 
 * @author grinning
 *
 */
public class ClassSetter {
   
	private Location location;     //loading some vars, ya know
    private Volume volume;
    private final String name;
    private Warzone warzone;
    
    public ClassSetter(String name, Warzone warzone, Location location) {
    	this.name = name;
    	this.warzone = warzone;
    	this.location = location;
    	this.volume = new Volume(name, warzone.getWorld());
    	this.setLocation(location);
    	this.addClassSetterBlocks();
    	
    }
    public void addClassSetterBlocks() {
    	this.volume.setToMaterial(Material.AIR);
    	int x = this.location.getBlockX();
    	int y = this.location.getBlockY();
    	int z = this.location.getBlockZ();
    	this.warzone.getWorld().getBlockAt(x, y - 1, z).getState().setType(Material.WATER); //the center block under your feet
    	this.warzone.getWorld().getBlockAt(x + 1, y - 1, z).getState().setType(Material.OBSIDIAN);
    	this.warzone.getWorld().getBlockAt(x - 1, y - 1, z).getState().setType(Material.NETHERRACK);
    	this.warzone.getWorld().getBlockAt(x, y - 1, z + 1).getState().setType(Material.OBSIDIAN);
    	this.warzone.getWorld().getBlockAt(x, y - 1, z - 1).getState().setType(Material.NETHERRACK);
     }
    public boolean isIn(Location playerLocation) {
    	int x = this.location.getBlockX();
    	int y = this.location.getBlockY();
    	int z = this.location.getBlockZ();
    	int playerX = playerLocation.getBlockX();
    	int playerY = playerLocation.getBlockY();
    	int playerZ = playerLocation.getBlockZ();
    	if (x == playerX && y == playerY && z == playerZ){
    		return true;
    	}
    	return false;
    }
    public Location getLocation(){
    	return this.location;
    }
    public String getName(){
    	return this.name;
    }
	public void setLocation(Location location) {
		Block locationBlock = this.warzone.getWorld().getBlockAt(location.getBlockX(), location.getBlockY(), location.getBlockZ());
		this.volume.setCornerOne(locationBlock.getFace(BlockFace.DOWN).getFace(BlockFace.EAST, 2).getFace(BlockFace.SOUTH, 2));
		this.volume.setCornerTwo(locationBlock.getFace(BlockFace.UP, 2).getFace(BlockFace.WEST, 2).getFace(BlockFace.NORTH, 2));
		this.volume.saveBlocks();
		this.location = location;
		this.addClassSetterBlocks();
	}
	public Volume getVolume() {
		return this.volume;
	}
	public void setVolume(Volume newVolume) {
		this.volume = newVolume;
	}
	
}
