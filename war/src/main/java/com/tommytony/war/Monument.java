package com.tommytony.war;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import bukkit.tommytony.war.War;

import com.tommytony.war.volumes.Volume;

/**
 * 
 * @author tommytony
 * 
 */
public class Monument {
	private Location location;
	private Volume volume;

	private Team ownerTeam = null;
	private final String name;
	private Warzone warzone;

	public Monument(String name, War war, Warzone warzone, Location location) {
		this.name = name;
		this.location = location;
		this.warzone = warzone;
		this.volume = new Volume(name, war, warzone.getWorld());
		this.setLocation(location);

		this.addMonumentBlocks();
	}

	public void addMonumentBlocks() {
		this.volume.setToMaterial(Material.AIR);

		this.ownerTeam = null;
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
		this.warzone.getWorld().getBlockAt(x, y - 1, z - 1).setType(Material.OBSIDIAN);

		this.warzone.getWorld().getBlockAt(x - 1, y - 1, z + 1).setType(Material.OBSIDIAN);
		this.warzone.getWorld().getBlockAt(x - 1, y - 1, z).setType(Material.OBSIDIAN);
		this.warzone.getWorld().getBlockAt(x - 1, y - 1, z - 1).setType(Material.OBSIDIAN);

		// outer ring

		this.warzone.getWorld().getBlockAt(x + 2, y - 1, z + 2).setType(Material.GLOWSTONE);
		this.warzone.getWorld().getBlockAt(x + 2, y - 1, z + 1).setType(Material.OBSIDIAN);
		this.warzone.getWorld().getBlockAt(x + 2, y - 1, z).setType(Material.OBSIDIAN);
		this.warzone.getWorld().getBlockAt(x + 2, y - 1, z - 1).setType(Material.OBSIDIAN);
		this.warzone.getWorld().getBlockAt(x + 2, y - 1, z - 2).setType(Material.GLOWSTONE);

		this.warzone.getWorld().getBlockAt(x - 1, y - 1, z + 2).setType(Material.OBSIDIAN);
		this.warzone.getWorld().getBlockAt(x - 1, y - 1, z - 2).setType(Material.OBSIDIAN);

		this.warzone.getWorld().getBlockAt(x, y - 1, z + 2).setType(Material.OBSIDIAN);
		this.warzone.getWorld().getBlockAt(x, y - 1, z - 2).setType(Material.OBSIDIAN);

		this.warzone.getWorld().getBlockAt(x + 1, y - 1, z + 2).setType(Material.OBSIDIAN);
		this.warzone.getWorld().getBlockAt(x + 1, y - 1, z - 2).setType(Material.OBSIDIAN);

		this.warzone.getWorld().getBlockAt(x - 2, y - 1, z + 2).setType(Material.GLOWSTONE);
		this.warzone.getWorld().getBlockAt(x - 2, y - 1, z + 1).setType(Material.OBSIDIAN);
		this.warzone.getWorld().getBlockAt(x - 2, y - 1, z).setType(Material.OBSIDIAN);
		this.warzone.getWorld().getBlockAt(x - 2, y - 1, z - 1).setType(Material.OBSIDIAN);
		this.warzone.getWorld().getBlockAt(x - 2, y - 1, z - 2).setType(Material.GLOWSTONE);

		// block holder
		this.warzone.getWorld().getBlockAt(x, y, z).setType(Material.OBSIDIAN);
		this.warzone.getWorld().getBlockAt(x, y, z - 1).setType(Material.OBSIDIAN);
		this.warzone.getWorld().getBlockAt(x, y, z + 1).setType(Material.OBSIDIAN);

		this.warzone.getWorld().getBlockAt(x, y + 1, z - 1).setType(Material.OBSIDIAN);
		this.warzone.getWorld().getBlockAt(x, y + 1, z + 1).setType(Material.OBSIDIAN);

		this.warzone.getWorld().getBlockAt(x, y + 2, z).setType(Material.OBSIDIAN);
		this.warzone.getWorld().getBlockAt(x, y + 2, z - 1).setType(Material.OBSIDIAN);
		this.warzone.getWorld().getBlockAt(x, y + 2, z + 1).setType(Material.OBSIDIAN);

	}

	public boolean isNear(Location playerLocation) {
		int x = this.location.getBlockX();
		int y = this.location.getBlockY();
		int z = this.location.getBlockZ();
		int playerX = playerLocation.getBlockX();
		int playerY = playerLocation.getBlockY();
		int playerZ = playerLocation.getBlockZ();
		int diffX = Math.abs(playerX - x);
		int diffY = Math.abs(playerY - y);
		int diffZ = Math.abs(playerZ - z);
		if (diffX < 6 && diffY < 6 && diffZ < 6) {
			return true;
		}
		return false;
	}

	public boolean isOwner(Team team) {
		if (team == this.ownerTeam) {
			return true;
		}
		return false;
	}

	public boolean hasOwner() {
		return this.ownerTeam != null;
	}

	public void capture(Team team) {
		this.ownerTeam = team;
	}

	public void uncapture() {
		this.ownerTeam = null;
	}

	public Location getLocation() {
		return this.location;
	}

	public void setOwnerTeam(Team team) {
		this.ownerTeam = team;

	}

	public String getName() {
		return this.name;
	}

	public void setLocation(Location location) {
		Block locationBlock = this.warzone.getWorld().getBlockAt(location.getBlockX(), location.getBlockY(), location.getBlockZ());
		this.volume.setCornerOne(locationBlock.getFace(BlockFace.DOWN).getFace(BlockFace.EAST, 2).getFace(BlockFace.SOUTH, 2));
		this.volume.setCornerTwo(locationBlock.getFace(BlockFace.UP, 2).getFace(BlockFace.WEST, 2).getFace(BlockFace.NORTH, 2));
		this.volume.saveBlocks();
		this.location = location;
		this.addMonumentBlocks();
	}

	public Volume getVolume() {
		return this.volume;
	}

	public void setVolume(Volume newVolume) {
		this.volume = newVolume;

	}

	public Team getOwnerTeam() {

		return this.ownerTeam;
	}
}
