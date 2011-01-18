package com.tommytony.war;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;

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
			int hubDepth = 6;
			int hubHeigth = 4;
			
			Block locationBlock = location.getWorld().getBlockAt(location.getBlockX(), location.getBlockY(), location.getBlockZ());
			volume.setCornerOne(locationBlock.getFace(BlockFace.EAST).getFace(BlockFace.SOUTH, halfHubWidth).getFace(BlockFace.DOWN));
			volume.setCornerTwo(locationBlock.getFace(BlockFace.NORTH, halfHubWidth).getFace(BlockFace.WEST, hubDepth).getFace(BlockFace.UP, hubHeigth));
			volume.saveBlocks();
			
			// glass floor
			volume.setToMaterial(Material.AIR);
			volume.setFaceMaterial(BlockFace.DOWN, Material.GLASS);
			
			// draw gates
			Block currentGateBlock = volume.getCornerOne().getFace(BlockFace.UP).getFace(BlockFace.WEST, hubDepth).getFace(BlockFace.NORTH, 2);
			
			for(int i = 0;i < war.getWarzones().size(); i++) {	// gonna use the index to find it again
				zoneGateBlocks.add(0, currentGateBlock);
				currentGateBlock.getFace(BlockFace.DOWN).setType(Material.GLOWSTONE);
				currentGateBlock.setType(Material.PORTAL);
				currentGateBlock.getFace(BlockFace.UP).setType(Material.PORTAL);
				currentGateBlock.getFace(BlockFace.SOUTH).setType(Material.OBSIDIAN);
				currentGateBlock.getFace(BlockFace.NORTH).getFace(BlockFace.UP).setType(Material.OBSIDIAN);
				currentGateBlock.getFace(BlockFace.SOUTH).getFace(BlockFace.UP).getFace(BlockFace.UP).setType(Material.OBSIDIAN);
				currentGateBlock.getFace(BlockFace.NORTH).setType(Material.OBSIDIAN);
				currentGateBlock.getFace(BlockFace.SOUTH).getFace(BlockFace.UP).setType(Material.OBSIDIAN);
				currentGateBlock.getFace(BlockFace.NORTH).getFace(BlockFace.UP).getFace(BlockFace.UP).setType(Material.OBSIDIAN);
				currentGateBlock.getFace(BlockFace.UP).getFace(BlockFace.UP).setType(Material.OBSIDIAN);
				currentGateBlock = currentGateBlock.getFace(BlockFace.NORTH, 4);
			}
			
			// War hub sign
			Block signBlock = locationBlock.getFace(BlockFace.WEST);
			if(signBlock.getType() != Material.SIGN_POST) signBlock.setType(Material.SIGN_POST);
			signBlock.setData((byte)8);
			BlockState state = signBlock.getState();
			Sign sign = (Sign) state;
			sign.setLine(0, "War hub");
			sign.setLine(1, "");
			sign.setLine(2, "Pick you battle!");
			sign.setLine(3, "");
			state.update(true);
		}
	}
	
	public void resetZoneSign(Warzone zone) {
		int i = 0;
		for(i = 0; i < war.getWarzones().size(); i++) {
			if(zone.getName() == war.getWarzones().get(i).getName()) break;
		}
		
		Block zoneGate = zoneGateBlocks.get(i);
		Block block = zoneGate.getFace(BlockFace.SOUTH).getFace(BlockFace.EAST, 1);
		if(block.getType() != Material.SIGN_POST) block.setType(Material.SIGN_POST);
		block.setData((byte)8);
		
		int zoneCap = 0;
		int zonePlayers = 0;
		for(Team t : zone.getTeams()) {
			zonePlayers += t.getPlayers().size();
			zoneCap += zone.getTeamCap();
		}
		
		BlockState state = block.getState();
		Sign sign = (Sign) state;
		sign.setLine(0, "Warzone");
		sign.setLine(1, zone.getName());
		sign.setLine(2, zonePlayers + "/" + zoneCap + " players");
		sign.setLine(3, zone.getTeams().size() + " teams");
		state.update(true);
	}

	public void setVolume(Volume vol) {
		this.volume = vol;
	}

}
