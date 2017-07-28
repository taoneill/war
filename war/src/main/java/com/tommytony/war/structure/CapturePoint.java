package com.tommytony.war.structure;

import com.tommytony.war.Warzone;
import com.tommytony.war.config.TeamKind;
import com.tommytony.war.config.WarzoneConfig;
import com.tommytony.war.volume.Volume;
import org.apache.commons.lang.Validate;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.util.Vector;

/**
 * Capture points
 *
 * @author Connor Monahan
 */
public class CapturePoint {
	private static int[][][] structure = {
			{
					{1, 1, 1},
					{1, 2, 1},
					{1, 1, 1}
			},
			{
					{0, 0, 0},
					{0, 3, 0},
					{0, 0, 0}
			},
			{
					{0, 0, 0},
					{0, 3, 0},
					{0, 0, 0}
			},
			{
					{0, 0, 0},
					{0, 3, 0},
					{0, 0, 0}
			},
			{
					{0, 0, 0},
					{0, 3, 0},
					{0, 0, 0}
			}
	};

	private final String name;
	private Volume volume;
	private Location location;
	private TeamKind controller, defaultController;
	private int strength, controlTime;
	private Warzone warzone;

	public CapturePoint(String name, Location location, TeamKind defaultController, int strength, Warzone warzone) {
		this.name = name;
		this.defaultController = defaultController;
		this.controller = defaultController;
		this.strength = strength;
		this.controlTime = 0;
		this.warzone = warzone;
		this.volume = new Volume("cp-" + name, warzone.getWorld());
		this.setLocation(location);
	}

	private Location getOrigin() {
		return location.clone().subtract(1, 1, 1).getBlock().getLocation();
	}

	private void updateBlocks() {
		Validate.notNull(location);
		// Set origin to back left corner
		Location origin = this.getOrigin();
		// Build structure
		for (int y = 0; y < structure.length; y++) {
			for (int z = 0; z < structure[0].length; z++) {
				for (int x = 0; x < structure[0][0].length; x++) {
					BlockState state = origin.clone().add(x, y, z).getBlock().getState();
					switch (structure[y][z][x]) {
						case 0:
							state.setType(Material.AIR);
							break;
						case 1:
							state.setType(this.warzone.getWarzoneMaterials().getMainBlock().getType());
							state.setData(this.warzone.getWarzoneMaterials().getMainBlock().getData());
							break;
						case 2:
							state.setType(this.warzone.getWarzoneMaterials().getLightBlock().getType());
							state.setData(this.warzone.getWarzoneMaterials().getLightBlock().getData());
							break;
						case 3:
							state.setType(this.warzone.getWarzoneMaterials().getStandBlock().getType());
							state.setData(this.warzone.getWarzoneMaterials().getStandBlock().getData());
							break;
						default:
							throw new IllegalStateException("Invalid structure");
					}
					state.update(true);
				}
			}
		}
		// Add flag block
		if (strength > 0 && controller != null) {
			// Make flag point direction of player when setting the capture point
			int flagHeight = (int) (strength / (getMaxStrength() / 4.0));
			Vector dir = new Vector(1 + -Math.round(Math.sin(Math.toRadians(location.getYaw()))), flagHeight,
					1 + Math.round(Math.cos(Math.toRadians(location.getYaw()))));
			BlockState state = origin.clone().add(dir).getBlock().getState();
			state.setType(controller.getMaterial());
			state.setData(controller.getBlockData());
			state.update(true);
		}
	}

	public String getName() {
		return name;
	}

	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = new Location(location.getWorld(), location.getBlockX(), location.getBlockY(),
				location.getBlockZ(), location.getYaw(), 0);
		this.volume.setCornerOne(this.getOrigin());
		this.volume.setCornerTwo(this.getOrigin().add(structure[0][0].length, structure.length, structure[0].length));
		this.volume.saveBlocks();
		this.updateBlocks();
	}

	public TeamKind getDefaultController() {
		return defaultController;
	}

	public TeamKind getController() {
		return controller;
	}

	public void setController(TeamKind controller) {
		this.controller = controller;
		if (strength > 0) {
			this.updateBlocks();
		}
	}

	public int getStrength() {
		return strength;
	}

	public void setStrength(int strength) {
		Validate.isTrue(strength <= getMaxStrength());
		this.strength = strength;
		this.updateBlocks();
	}

	public int getControlTime() {
		return controlTime;
	}

	public void setControlTime(int controlTime) {
		this.controlTime = controlTime;
	}

	public Volume getVolume() {
		return volume;
	}

	public void setVolume(Volume volume) {
		this.volume = volume;
	}

	public void reset() {
		this.controller = defaultController;
		if (this.controller != null) {
			this.strength = 4;
		} else {
			this.strength = 0;
		}
		this.updateBlocks();
	}

	private long lastMessage = 0;
	public boolean antiChatSpam() {
		long now = System.currentTimeMillis();
		if (now - lastMessage > 3000) {
			lastMessage = now;
			return true;
		}
		return false;
	}

	public int getMaxStrength() {
		return warzone.getWarzoneConfig().getInt(WarzoneConfig.CAPTUREPOINTTIME);
	}
}
