package com.tommytony.war.structure;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.material.Sign;

import com.tommytony.war.Team;
import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import com.tommytony.war.config.TeamConfig;
import com.tommytony.war.config.WarzoneConfig;
import com.tommytony.war.utility.Direction;
import com.tommytony.war.volume.Volume;

/**
 *
 * @author tommytony, Tim Düsterhus
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
			this.setOrientation(Direction.SOUTH());
		} else if (hubOrientation.equals("north")) {
			yaw = 90;
			this.setOrientation(Direction.NORTH());
		} else if (hubOrientation.equals("east")) {
			yaw = 180;
			this.setOrientation(Direction.EAST());
		} else {
			yaw = 0;
			this.setOrientation(Direction.WEST());
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
			facing = Direction.WEST();
		} else if (yaw >= 45 && yaw < 135) {
			facing = Direction.NORTH();
		} else if (yaw >= 135 && yaw < 225) {
			facing = Direction.EAST();
		} else if (yaw >= 225 && yaw < 315) {
			facing = Direction.SOUTH();
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
			if (gate.getX() == playerLocation.getBlockX()
					&& gate.getY() == playerLocation.getBlockY()
					&& gate.getZ() == playerLocation.getBlockZ()) {
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
			if (this.getOrientation() == Direction.SOUTH()) {
				left = Direction.EAST();
				right = Direction.WEST();
				back = Direction.NORTH();
			} else if (this.getOrientation() == Direction.NORTH()) {
				left = Direction.WEST();
				right = Direction.EAST();
				back = Direction.SOUTH();
			} else if (this.getOrientation() == Direction.EAST()) {
				left = Direction.NORTH();
				right = Direction.SOUTH();
				back = Direction.WEST();
			} else {
				left = Direction.SOUTH();
				right = Direction.NORTH();
				back = Direction.EAST();
			}

			Block locationBlock = this.location.getWorld().getBlockAt(this.location.getBlockX(), this.location.getBlockY(), this.location.getBlockZ());

			this.volume.setWorld(this.location.getWorld());
			this.volume.setCornerOne(locationBlock.getRelative(back).getRelative(left, halfHubWidth).getRelative(BlockFace.DOWN));
			this.volume.setCornerTwo(locationBlock.getRelative(right, halfHubWidth).getRelative(front, hubDepth).getRelative(BlockFace.UP, hubHeigth));
			this.volume.saveBlocks();

			// glass floor
			if (!War.war.getWarhubMaterials().getFloorBlock().getType().equals(Material.AIR)) {
				// If air, don't set floor to air, just leave original ground. Otherwise apply material.
				this.volume.setFaceMaterial(BlockFace.DOWN, War.war.getWarhubMaterials().getFloorBlock());
			}
			
			if (!War.war.getWarhubMaterials().getOutlineBlock().getType().equals(Material.AIR)) {
				// If air, leave original blocks.
				this.volume.setFloorOutline(War.war.getWarhubMaterials().getOutlineBlock());
			}
			
			// clear minimal path around warhub tp
			Volume warhubTpVolume = new Volume("warhubtp", this.location.getWorld());
			warhubTpVolume.setCornerOne(locationBlock.getRelative(back).getRelative(left));
			warhubTpVolume.setCornerTwo(locationBlock.getRelative(front, 2).getRelative(right).getRelative(BlockFace.UP));
			warhubTpVolume.setToMaterial(Material.AIR);
		
			// draw gates
			Block currentGateBlock = this.volume.getCornerOne().getBlock().getRelative(BlockFace.UP).getRelative(front, hubDepth).getRelative(right, 2);

			for (Warzone zone : War.war.getWarzones()) { // gonna use the index to find it again
				if (!zone.getWarzoneConfig().getBoolean(WarzoneConfig.DISABLED)) {
					this.zoneGateBlocks.put(zone.getName(), currentGateBlock);
					
					// minimal air path
					Volume gateAirVolume = new Volume("gateAir", currentGateBlock.getWorld());
					gateAirVolume.setCornerOne(currentGateBlock.getRelative(right));
					gateAirVolume.setCornerTwo(currentGateBlock.getRelative(left).getRelative(back, 2).getRelative(BlockFace.UP, 2));
					gateAirVolume.setToMaterial(Material.AIR);
					
					currentGateBlock.getRelative(back, 2).getRelative(right, 2).setType(Material.AIR);
					currentGateBlock.getRelative(back, 2).getRelative(right, 2).getRelative(BlockFace.UP).setType(Material.AIR);
					currentGateBlock.getRelative(back, 2).getRelative(right, 2).getRelative(BlockFace.UP, 2).setType(Material.AIR);
					currentGateBlock.getRelative(back, 2).getRelative(left, 2).setType(Material.AIR);
					currentGateBlock.getRelative(back, 2).getRelative(left, 2).getRelative(BlockFace.UP).setType(Material.AIR);
					currentGateBlock.getRelative(back, 2).getRelative(left, 2).getRelative(BlockFace.UP, 2).setType(Material.AIR);
					
					BlockState cgbdl = currentGateBlock.getRelative(BlockFace.DOWN).getState();
					cgbdl.setType(War.war.getWarhubMaterials().getLightBlock().getType());
					cgbdl.setData(War.war.getWarhubMaterials().getLightBlock().getData());
					cgbdl.update(true);
					// gate blocks
					Block[] gateBlocks = {
							currentGateBlock.getRelative(left),
							currentGateBlock.getRelative(right).getRelative(BlockFace.UP),
							currentGateBlock.getRelative(left).getRelative(BlockFace.UP).getRelative(BlockFace.UP),
							currentGateBlock.getRelative(right),
							currentGateBlock.getRelative(left).getRelative(BlockFace.UP),
							currentGateBlock.getRelative(right).getRelative(BlockFace.UP).getRelative(BlockFace.UP),
							currentGateBlock.getRelative(BlockFace.UP).getRelative(BlockFace.UP)
							};
					for (Block gateBlock : gateBlocks) {
						BlockState gb = gateBlock.getState();
						gb.setType(War.war.getWarhubMaterials().getGateBlock().getType());
						gb.setData(War.war.getWarhubMaterials().getGateBlock().getData());
						gb.update(true);
					}
					currentGateBlock = currentGateBlock.getRelative(right, 4);
				}
			}

			// War hub sign
			locationBlock.getRelative(front, 2).setType(Material.SIGN_POST);
			String[] lines = War.war.getString("sign.warhub").split("\n");
			org.bukkit.block.Sign locationBlockFront = (org.bukkit.block.Sign) locationBlock.getRelative(front, 2).getState();
			for (int i = 0; i < 4; i++) {
				locationBlockFront.setLine(i, lines[i]);
			}
			org.bukkit.material.Sign sign = (Sign) locationBlockFront.getData();
			sign.setFacingDirection(orientation.getOppositeFace());
			locationBlockFront.setData(sign);
			locationBlockFront.update(true);
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
		if (this.getOrientation() == Direction.SOUTH()) {
			left = Direction.EAST();
			back = Direction.NORTH();
		} else if (this.getOrientation() == Direction.NORTH()) {
			left = Direction.WEST();
			back = Direction.SOUTH();
		} else if (this.getOrientation() == Direction.EAST()) {
			left = Direction.NORTH();
			back = Direction.WEST();
		} else {
			left = Direction.SOUTH();
			back = Direction.EAST();
		}

		Block zoneGate = this.zoneGateBlocks.get(zone.getName());
		if (zoneGate != null) {
			zoneGate.getRelative(BlockFace.UP, 2).getRelative(back, 1).setType(Material.WALL_SIGN);
			org.bukkit.block.Sign block = (org.bukkit.block.Sign) zoneGate.getRelative(BlockFace.UP, 2).getRelative(back, 1).getState();
			org.bukkit.material.Sign data = (Sign) block.getData();
			data.setFacingDirection(this.getOrientation().getOppositeFace());
			block.setData(data);
			int zoneCap = 0;
			int zonePlayers = 0;
			for (Team t : zone.getTeams()) {
				zonePlayers += t.getPlayers().size();
				zoneCap += t.getTeamConfig().resolveInt(TeamConfig.TEAMSIZE);
			}
			String[] lines = MessageFormat.format(
					War.war.getString("sign.warzone"),
					zone.getName(), zonePlayers, zoneCap,
					zone.getTeams().size()).split("\n");
			for (int i = 0; i < 4; i++) {
				block.setLine(i, lines[i]);
			}
			block.update(true);
			
			if (zonePlayers > 0) {
				// add redstone blocks and torches to gate if there are players in it (to highlight active zones)
				zoneGate.getRelative(BlockFace.UP, 2).getRelative(left).setType(Material.REDSTONE_BLOCK);
				zoneGate.getRelative(BlockFace.UP, 2).getRelative(left.getOppositeFace()).setType(Material.REDSTONE_BLOCK);
				zoneGate.getRelative(BlockFace.UP, 2).getRelative(left).getRelative(back, 1).setType(Material.AIR);
				zoneGate.getRelative(BlockFace.UP, 2).getRelative(left.getOppositeFace()).getRelative(back, 1).setType(Material.AIR);
			} else {
				zoneGate.getRelative(BlockFace.UP, 2).getRelative(left).getRelative(back, 1).setType(Material.AIR);
				zoneGate.getRelative(BlockFace.UP, 2).getRelative(left.getOppositeFace()).getRelative(back, 1).setType(Material.AIR);
				
				BlockState topLeftGateBlock = zoneGate.getRelative(BlockFace.UP, 2).getRelative(left).getState();
				topLeftGateBlock.setType(War.war.getWarhubMaterials().getGateBlock().getType());
				topLeftGateBlock.setData(War.war.getWarhubMaterials().getGateBlock().getData());
				topLeftGateBlock.update(true);
				
				BlockState topRightGateBlock = zoneGate.getRelative(BlockFace.UP, 2).getRelative(left.getOppositeFace()).getState();
				topRightGateBlock.setType(War.war.getWarhubMaterials().getGateBlock().getType());
				topRightGateBlock.setData(War.war.getWarhubMaterials().getGateBlock().getData());
				topRightGateBlock.update(true);
			}
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
