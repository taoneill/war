package com.tommytony.war;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import bukkit.tommytony.war.War;

import com.tommytony.war.utils.SignHelper;
import com.tommytony.war.volumes.BlockInfo;
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
	private Map<String, Block> zoneGateBlocks = new HashMap<String, Block>();

	public WarHub(War war, Location location) {
		this.war = war;
		this.location = location;
		this.volume = new Volume("warhub", war, location.getWorld());
	}

	public Volume getVolume() {
		return this.volume;
	}

	public void setLocation(Location loc) {
		this.location = loc;
	}

	public Location getLocation() {
		return this.location;
	}

	public Warzone getDestinationWarzoneForLocation(Location playerLocation) {
		Warzone zone = null;
		for (String zoneName : this.zoneGateBlocks.keySet()) {
			Block gate = this.zoneGateBlocks.get(zoneName);
			if (gate.getX() == playerLocation.getBlockX() && gate.getY() == playerLocation.getBlockY() && gate.getZ() == playerLocation.getBlockZ()) {
				zone = this.war.findWarzone(zoneName);
			}
		}
		return zone;
	}

	public void initialize() {
		// for now, draw the wall of gates to the west
		this.zoneGateBlocks.clear();
		int disabled = 0;
		for (Warzone zone : this.war.getWarzones()) {
			if (zone.isDisabled()) {
				disabled++;
			}
		}
		int noOfWarzones = this.war.getWarzones().size() - disabled;
		if (noOfWarzones > 0) {
			int hubWidth = noOfWarzones * 4 + 2;
			int halfHubWidth = hubWidth / 2;
			int hubDepth = 6;
			int hubHeigth = 4;

			Block locationBlock = this.location.getWorld().getBlockAt(this.location.getBlockX(), this.location.getBlockY(), this.location.getBlockZ());
			this.volume.setCornerOne(locationBlock.getFace(BlockFace.EAST).getFace(BlockFace.SOUTH, halfHubWidth).getFace(BlockFace.DOWN));
			this.volume.setCornerTwo(locationBlock.getFace(BlockFace.NORTH, halfHubWidth).getFace(BlockFace.WEST, hubDepth).getFace(BlockFace.UP, hubHeigth));
			this.volume.saveBlocks();

			// glass floor
			this.volume.clearBlocksThatDontFloat();
			this.volume.setToMaterial(Material.AIR);
			this.volume.setFaceMaterial(BlockFace.DOWN, Material.GLASS);

			// draw gates
			Block currentGateBlock = BlockInfo.getBlock(this.location.getWorld(), this.volume.getCornerOne()).getFace(BlockFace.UP).getFace(BlockFace.WEST, hubDepth).getFace(BlockFace.NORTH, 2);

			for (Warzone zone : this.war.getWarzones()) { // gonna use the index to find it again
				if (!zone.isDisabled()) {
					this.zoneGateBlocks.put(zone.getName(), currentGateBlock);
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
			}

			// War hub sign
			Block signBlock = locationBlock.getFace(BlockFace.WEST);

			String[] lines = new String[4];
			lines[0] = "War hub";
			lines[1] = "(/warhub)";
			lines[2] = "Pick your";
			lines[3] = "battle!";
			SignHelper.setToSign(this.war, signBlock, (byte) 8, lines);

			// Warzone signs
			for (Warzone zone : this.war.getWarzones()) {
				if (!zone.isDisabled() && zone.ready()) {
					this.resetZoneSign(zone);
				}
			}
		}
	}

	public void resetZoneSign(Warzone zone) {

		Block zoneGate = this.zoneGateBlocks.get(zone.getName());
		Block block = zoneGate.getFace(BlockFace.SOUTH).getFace(BlockFace.EAST, 1);
		if (block.getType() != Material.SIGN_POST) {
			block.setType(Material.SIGN_POST);
		}
		block.setData((byte) 8);

		int zoneCap = 0;
		int zonePlayers = 0;
		for (Team t : zone.getTeams()) {
			zonePlayers += t.getPlayers().size();
			zoneCap += zone.getTeamCap();
		}
		String[] lines = new String[4];
		lines[0] = "Warzone";
		lines[1] = zone.getName();
		lines[2] = zonePlayers + "/" + zoneCap + " players";
		lines[3] = zone.getTeams().size() + " teams";
		SignHelper.setToSign(this.war, block, (byte) 8, lines);
	}

	public void setVolume(Volume vol) {
		this.volume = vol;
	}

}
