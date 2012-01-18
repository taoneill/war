package com.tommytony.war;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import com.tommytony.war.volumes.Volume;

/**
 *
 * @author tommytony
 *
 */
public class Cake {
	private Location location;
	private Volume volume;

	private final String name;
	private Warzone warzone;
	private Player capturer;

	public Cake(String name, Warzone warzone, Location location) {
		this.name = name;
		this.location = location;
		this.warzone = warzone;
		this.volume = new Volume("cake-" + name, warzone.getWorld());
		this.setLocation(location);
	}

	public void addCakeBlocks() {
		this.volume.setToMaterial(Material.AIR);

		int x = this.location.getBlockX();
		int y = this.location.getBlockY();
		int z = this.location.getBlockZ();

		// center
		this.warzone.getWorld().getBlockAt(x, y - 1, z).getState().setType(Material.OBSIDIAN);

		// inner ring
		this.warzone.getWorld().getBlockAt(x + 1, y - 1, z + 1).setType(Material.OBSIDIAN);
		this.warzone.getWorld().getBlockAt(x + 1, y - 1, z).setType(Material.OBSIDIAN);
		this.warzone.getWorld().getBlockAt(x + 1, y - 1, z - 1).setType(Material.OBSIDIAN);

		this.warzone.getWorld().getBlockAt(x, y - 1, z + 1).setType(Material.OBSIDIAN);
		this.warzone.getWorld().getBlockAt(x, y - 1, z).setType(Material.GLOWSTONE);
		this.warzone.getWorld().getBlockAt(x, y - 1, z - 1).setType(Material.OBSIDIAN);

		this.warzone.getWorld().getBlockAt(x - 1, y - 1, z + 1).setType(Material.OBSIDIAN);
		this.warzone.getWorld().getBlockAt(x - 1, y - 1, z).setType(Material.OBSIDIAN);
		this.warzone.getWorld().getBlockAt(x - 1, y - 1, z - 1).setType(Material.OBSIDIAN);

		// block holder
		this.warzone.getWorld().getBlockAt(x, y, z).setType(Material.GLASS);
		Block cakeBlock = this.warzone.getWorld().getBlockAt(x, y + 1, z);
		cakeBlock.setType(Material.CAKE_BLOCK);
	}

	public boolean isCakeBlock(Location otherLocation) {
		int x = this.location.getBlockX();
		int y = this.location.getBlockY() + 1;
		int z = this.location.getBlockZ();
		int otherX = otherLocation.getBlockX();
		int otherY = otherLocation.getBlockY();
		int otherZ = otherLocation.getBlockZ();
		
		return x == otherX
			&& y == otherY
			&& z == otherZ;
	}

	public void capture(Player capturer) {
		this.capturer = capturer;
	}
	
	public boolean isCaptured() {
		return this.capturer != null;
	}

	public void uncapture() {
		this.capturer = null;
	}

	public Location getLocation() {
		return this.location;
	}

	public String getName() {
		return this.name;
	}

	public void setLocation(Location location) {
		Block locationBlock = this.warzone.getWorld().getBlockAt(location.getBlockX(), location.getBlockY(), location.getBlockZ());
		this.volume.setCornerOne(locationBlock.getRelative(BlockFace.DOWN).getRelative(BlockFace.EAST, 1).getRelative(BlockFace.SOUTH, 1));
		this.volume.setCornerTwo(locationBlock.getRelative(BlockFace.UP, 2).getRelative(BlockFace.WEST, 1).getRelative(BlockFace.NORTH, 1));
		this.volume.saveBlocks();
		this.location = location;
		this.addCakeBlocks();
	}

	public Volume getVolume() {
		return this.volume;
	}

	public void setVolume(Volume newVolume) {
		this.volume = newVolume;

	}
}
