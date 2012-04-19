package com.tommytony.war.structure;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;


import com.tommytony.war.Team;
import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import com.tommytony.war.config.TeamConfig;
import com.tommytony.war.config.WarzoneConfig;
import com.tommytony.war.utility.SignHelper;
import com.tommytony.war.volume.BlockInfo;
import com.tommytony.war.volume.Volume;

/**
 *
 * @author tommytony, Tim DÃ¼sterhus
 * @package com.tommytony.war
 */
public class WarHub {
	private Location location;
	private Volume volume;
	private Map<String, Block> zoneGateBlocks = new HashMap<String, Block>();
	private BlockFace orientation;

	public WarHub(Location location, String hubOrientation) {
		int yaw = 0;
		if (hubOrientation.equals("south")) {
			yaw = 270;
			this.setOrientation(BlockFace.SOUTH);
		} else if (hubOrientation.equals("north")) {
			yaw = 90;
			this.setOrientation(BlockFace.NORTH);
		} else if (hubOrientation.equals("east")) {
			yaw = 180;
			this.setOrientation(BlockFace.EAST);
		} else {
			yaw = 0;
			this.setOrientation(BlockFace.WEST);
		}

		this.location = new Location(location.getWorld(),
								location.getX(),
								location.getY(),
								location.getZ(),
								yaw, 0);
		this.volume = new Volume("warhub", location.getWorld());
	}

	// Use when creating from player location (with yaw)
	public WarHub(Location location) {
		this.location = location;
		this.volume = new Volume("warhub", location.getWorld());

		this.setLocation(location);
	}

	public Volume getVolume() {
		return this.volume;
	}

	public void setLocation(Location loc) {
		this.location = loc;
		// Lobby orientation
		int yaw = 0;
		if (this.location.getYaw() >= 0) {
			yaw = (int) (this.location.getYaw() % 360);
		} else {
			yaw = (int) (360 + (this.location.getYaw() % 360));
		}

		BlockFace facing = null;
		if ((yaw >= 0 && yaw < 45) || (yaw >= 315 && yaw <= 360)) {
			facing = BlockFace.WEST;
		} else if (yaw >= 45 && yaw < 135) {
			facing = BlockFace.NORTH;
		} else if (yaw >= 135 && yaw < 225) {
			facing = BlockFace.EAST;
		} else if (yaw >= 225 && yaw < 315) {
			facing = BlockFace.SOUTH;
		}
		this.setOrientation(facing);
	}

	public Location getLocation() {
		return this.location;
	}

	public Warzone getDestinationWarzoneForLocation(Location playerLocation) {
		Warzone zone = null;
		for (String zoneName : this.zoneGateBlocks.keySet()) {
			Block gate = this.zoneGateBlocks.get(zoneName);
			if (gate.getX() == playerLocation.getBlockX() && gate.getY() == playerLocation.getBlockY() && gate.getZ() == playerLocation.getBlockZ()) {
				zone = War.war.findWarzone(zoneName);
			}
		}
		return zone;
	}

	public void initialize() {
		// for now, draw the wall of gates to the west
		this.zoneGateBlocks.clear();
		int disabled = 0;
		for (Warzone zone : War.war.getWarzones()) {
			if (zone.getWarzoneConfig().getBoolean(WarzoneConfig.DISABLED)) {
				disabled++;
			}
		}
		int noOfWarzones = War.war.getWarzones().size() - disabled;
		if (noOfWarzones > 0) {
			int hubWidth = noOfWarzones * 4 + 2;
			int halfHubWidth = hubWidth / 2;
			int hubDepth = 6;
			int hubHeigth = 4;

			BlockFace left;
			BlockFace right;
			BlockFace front = this.getOrientation();
			BlockFace back;
			byte data;
			if (this.getOrientation() == BlockFace.SOUTH) {
				data = (byte) 4;
				left = BlockFace.EAST;
				right = BlockFace.WEST;
				back = BlockFace.NORTH;
			} else if (this.getOrientation() == BlockFace.NORTH) {
				data = (byte) 12;
				left = BlockFace.WEST;
				right = BlockFace.EAST;
				back = BlockFace.SOUTH;
			} else if (this.getOrientation() == BlockFace.EAST) {
				data = (byte) 0;
				left = BlockFace.NORTH;
				right = BlockFace.SOUTH;
				back = BlockFace.WEST;
			} else {
				data = (byte) 8;
				left = BlockFace.SOUTH;
				right = BlockFace.NORTH;
				back = BlockFace.EAST;
			}

			Block locationBlock = this.location.getWorld().getBlockAt(this.location.getBlockX(), this.location.getBlockY(), this.location.getBlockZ());

			this.volume.setWorld(this.location.getWorld());
			this.volume.setCornerOne(locationBlock.getRelative(back).getRelative(left, halfHubWidth).getRelative(BlockFace.DOWN));
			this.volume.setCornerTwo(locationBlock.getRelative(right, halfHubWidth).getRelative(front, hubDepth).getRelative(BlockFace.UP, hubHeigth));
			this.volume.saveBlocks();

			// glass floor
			this.volume.clearBlocksThatDontFloat();
			this.volume.setToMaterial(Material.AIR);
			this.volume.setFaceMaterial(BlockFace.DOWN, Material.GLASS);

			// draw gates
			Block currentGateBlock = BlockInfo.getBlock(this.location.getWorld(), this.volume.getCornerOne()).getRelative(BlockFace.UP).getRelative(front, hubDepth).getRelative(right, 2);

			for (Warzone zone : War.war.getWarzones()) { // gonna use the index to find it again
				if (!zone.getWarzoneConfig().getBoolean(WarzoneConfig.DISABLED)) {
					this.zoneGateBlocks.put(zone.getName(), currentGateBlock);
					currentGateBlock.getRelative(BlockFace.DOWN).setType(Material.GLOWSTONE);
					currentGateBlock.getRelative(left).setType(Material.OBSIDIAN);
					currentGateBlock.getRelative(right).getRelative(BlockFace.UP).setType(Material.OBSIDIAN);
					currentGateBlock.getRelative(left).getRelative(BlockFace.UP).getRelative(BlockFace.UP).setType(Material.OBSIDIAN);
					currentGateBlock.getRelative(right).setType(Material.OBSIDIAN);
					currentGateBlock.getRelative(left).getRelative(BlockFace.UP).setType(Material.OBSIDIAN);
					currentGateBlock.getRelative(right).getRelative(BlockFace.UP).getRelative(BlockFace.UP).setType(Material.OBSIDIAN);
					currentGateBlock.getRelative(BlockFace.UP).getRelative(BlockFace.UP).setType(Material.OBSIDIAN);
					currentGateBlock = currentGateBlock.getRelative(right, 4);

				}
			}

			// War hub sign
			Block signBlock = locationBlock.getRelative(front);

			String[] lines = new String[4];
			lines[0] = "¤cWar Hub";
			lines[1] = "¤1(/warhub)";
			lines[2] = "¤aChoose Your";
			lines[3] = "¤aBattle!";
			SignHelper.setToSign(War.war, signBlock, data, lines);

			// Warzone signs
			for (Warzone zone : War.war.getWarzones()) {
				if (!zone.getWarzoneConfig().getBoolean(WarzoneConfig.DISABLED) && zone.ready()) {
					this.resetZoneSign(zone);
				}
			}
		}
	}

	/**
	 * Resets the sign of the given warzone
	 *
	 * @param Warzone
	 *                zone
	 */
	public void resetZoneSign(Warzone zone) {
		BlockFace left;
		BlockFace back;
		byte data;
		if (this.getOrientation() == BlockFace.SOUTH) {
			data = (byte) 4;
			left = BlockFace.EAST;
			back = BlockFace.NORTH;
		} else if (this.getOrientation() == BlockFace.NORTH) {
			data = (byte) 12;
			left = BlockFace.WEST;
			back = BlockFace.SOUTH;
		} else if (this.getOrientation() == BlockFace.EAST) {
			data = (byte) 0;
			left = BlockFace.NORTH;
			back = BlockFace.WEST;
		} else {
			data = (byte) 8;
			left = BlockFace.SOUTH;
			back = BlockFace.EAST;
		}

		Block zoneGate = this.zoneGateBlocks.get(zone.getName());
		if (zoneGate != null) {
			Block block = zoneGate.getRelative(left).getRelative(back, 1);
			if (block.getType() != Material.SIGN_POST) {
				block.setType(Material.SIGN_POST);
			}
			block.setData(data);
	
			int zoneCap = 0;
			int zonePlayers = 0;
			for (Team t : zone.getTeams()) {
				zonePlayers += t.getPlayers().size();
				zoneCap += t.getTeamConfig().resolveInt(TeamConfig.TEAMSIZE);
			}
			String[] lines = new String[4];
			lines[0] = "Warzone";
			lines[1] = zone.getName();
			lines[2] = zonePlayers + "/" + zoneCap + " players";
			lines[3] = zone.getTeams().size() + " teams";
			SignHelper.setToSign(War.war, block, data, lines);
		} else {
			War.war.log("Failed to find warhub gate for " + zone.getName() + " warzone.", Level.WARNING);
		}
	}

	public void setVolume(Volume vol) {
		this.volume = vol;
	}

	public void setOrientation(BlockFace orientation) {
		this.orientation = orientation;
	}

	public BlockFace getOrientation() {
		return this.orientation;
	}

}
