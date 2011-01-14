package com.tommytony.war;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Block;
import org.bukkit.BlockFace;
import org.bukkit.Location;
import org.bukkit.Material;

import bukkit.tommytony.war.War;

import com.tommytony.war.volumes.Volume;

/**
 * 
 * @author tommytony
 *
 */
public class WarHub {
	private final War war;
	private Location location;
	private Volume volume;
	private List<Block> zoneGateBlocks = new ArrayList<Block>();
	
	public WarHub(War war, Location location) {
		this.war = war;
		this.location = location;
		this.volume = new Volume("warHub", war, location.getWorld());
	}

	public Volume getVolume() {
		return volume;
	}

	public void setLocation(Location loc) {
		this.location = loc;
	}
	
	public Location getLocation() {
		return this.location;
	}
	
	public Warzone getDestinationWarzoneForLocation(Location playerLocation) {
		Warzone zone = null;
		for(Block gate : zoneGateBlocks) {
			if(gate.getX() == playerLocation.getBlockX()
					&& gate.getY() == playerLocation.getBlockY()
					&& gate.getZ() == playerLocation.getBlockZ()) {
				int zoneIndex = zoneGateBlocks.indexOf(gate);
				zone = war.getWarzones().get(zoneIndex);
			}
		}
		return zone;
	}

	public void initialize() {
		// for now, draw the wall of gates to the west
		zoneGateBlocks.clear();
		int noOfWarzones = war.getWarzones().size();
		if(noOfWarzones > 0) {
			int hubWidth = noOfWarzones * 4 + 1;
			int halfHubWidth = hubWidth / 2;
			int hubDepth = 5;
			int hubHeigth = 4;
			
			Block locationBlock = location.getWorld().getBlockAt(location.getBlockX(), location.getBlockY(), location.getBlockZ());
			volume.setCornerOne(locationBlock.getFace(BlockFace.South, halfHubWidth).getFace(BlockFace.Down));
			volume.setCornerTwo(locationBlock.getFace(BlockFace.North, halfHubWidth).getFace(BlockFace.West, hubDepth).getFace(BlockFace.North, hubHeigth));
			volume.saveBlocks();
			
			// draw gates
			Block currentGateBlock = locationBlock.getFace(BlockFace.South, halfHubWidth - 2).getFace(BlockFace.West, hubDepth);
			for(Warzone zone : war.getWarzones()) {	// gonna use the index to find it again
				zoneGateBlocks.add(currentGateBlock);
				currentGateBlock.setType(Material.PORTAL);
				currentGateBlock.getFace(BlockFace.Up).setType(Material.PORTAL);
				currentGateBlock.getFace(BlockFace.South).setType(Material.OBSIDIAN);
				currentGateBlock.getFace(BlockFace.North).getFace(BlockFace.Up).setType(Material.OBSIDIAN);
				currentGateBlock.getFace(BlockFace.South).getFace(BlockFace.Up).getFace(BlockFace.Up).setType(Material.OBSIDIAN);
				currentGateBlock.getFace(BlockFace.North).setType(Material.OBSIDIAN);
				currentGateBlock.getFace(BlockFace.South).getFace(BlockFace.Up).setType(Material.OBSIDIAN);
				currentGateBlock.getFace(BlockFace.North).getFace(BlockFace.Up).getFace(BlockFace.Up).setType(Material.OBSIDIAN);
				currentGateBlock.getFace(BlockFace.Up).getFace(BlockFace.Up).setType(Material.OBSIDIAN);
				currentGateBlock = currentGateBlock.getFace(BlockFace.North, 4);
			}
		}
	}
	
	public void resetSigns() {
		// TODO Signs
	}

	public void setVolume(Volume vol) {
		this.volume = vol;
	}

}
