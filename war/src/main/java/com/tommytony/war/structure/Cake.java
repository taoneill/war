package com.tommytony.war.structure;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;

import com.tommytony.war.Warzone;
import com.tommytony.war.utility.Direction;
import com.tommytony.war.volume.Volume;

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
		// make air (old two-high above floor)
		Volume airGap = new Volume(new Location(this.volume.getWorld(),
				this.volume.getCornerOne().getX(), this.volume.getCornerOne()
						.getY() + 1, this.volume.getCornerOne().getZ()),
				new Location(this.volume.getWorld(), this.volume.getCornerTwo()
						.getX(), this.volume.getCornerOne().getY() + 2,
						this.volume.getCornerTwo().getZ()));
		airGap.setToMaterial(Material.AIR);
		
		int x = this.location.getBlockX();
		int y = this.location.getBlockY();
		int z = this.location.getBlockZ();

		// center
		BlockState current = this.warzone.getWorld().getBlockAt(x, y - 1, z).getState();
		current.setType(this.warzone.getWarzoneMaterials().getMainBlock().getType());
		current.setData(this.warzone.getWarzoneMaterials().getMainBlock().getData());
		current.update(true);

		// inner ring
		current = this.warzone.getWorld().getBlockAt(x + 1, y - 1, z + 1).getState();
		current.setType(this.warzone.getWarzoneMaterials().getLightBlock().getType());
		current.setData(this.warzone.getWarzoneMaterials().getLightBlock().getData());
		current.update(true);
		current = this.warzone.getWorld().getBlockAt(x + 1, y - 1, z).getState();
		current.setType(this.warzone.getWarzoneMaterials().getMainBlock().getType());
		current.setData(this.warzone.getWarzoneMaterials().getMainBlock().getData());
		current.update(true);
		current = this.warzone.getWorld().getBlockAt(x + 1, y - 1, z - 1).getState();
		current.setType(this.warzone.getWarzoneMaterials().getMainBlock().getType());
		current.setData(this.warzone.getWarzoneMaterials().getMainBlock().getData());
		current.update(true);

		current = this.warzone.getWorld().getBlockAt(x, y - 1, z + 1).getState();
		current.setType(this.warzone.getWarzoneMaterials().getMainBlock().getType());
		current.setData(this.warzone.getWarzoneMaterials().getMainBlock().getData());
		current.update(true);
		current = this.warzone.getWorld().getBlockAt(x, y - 1, z).getState();
		current.setType(this.warzone.getWarzoneMaterials().getLightBlock().getType());
		current.setData(this.warzone.getWarzoneMaterials().getLightBlock().getData());
		current.update(true);
		current = this.warzone.getWorld().getBlockAt(x, y - 1, z - 1).getState();
		current.setType(this.warzone.getWarzoneMaterials().getMainBlock().getType());
		current.setData(this.warzone.getWarzoneMaterials().getMainBlock().getData());
		current.update(true);

		current = this.warzone.getWorld().getBlockAt(x - 1, y - 1, z + 1).getState();
		current.setType(this.warzone.getWarzoneMaterials().getMainBlock().getType());
		current.setData(this.warzone.getWarzoneMaterials().getMainBlock().getData());
		current.update(true);
		current = this.warzone.getWorld().getBlockAt(x - 1, y - 1, z).getState();
		current.setType(this.warzone.getWarzoneMaterials().getMainBlock().getType());
		current.setData(this.warzone.getWarzoneMaterials().getMainBlock().getData());
		current.update(true);
		current = this.warzone.getWorld().getBlockAt(x - 1, y - 1, z - 1).getState();
		current.setType(this.warzone.getWarzoneMaterials().getLightBlock().getType());
		current.setData(this.warzone.getWarzoneMaterials().getLightBlock().getData());
		current.update(true);

		// block holder
		current = this.warzone.getWorld().getBlockAt(x, y, z).getState();
		current.setType(this.warzone.getWarzoneMaterials().getStandBlock().getType());
		current.setData(this.warzone.getWarzoneMaterials().getStandBlock().getData());
		current.update(true);
		this.warzone.getWorld().getBlockAt(x, y + 1, z).setType(Material.CAKE_BLOCK);
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
		this.volume.setCornerOne(locationBlock.getRelative(BlockFace.DOWN).getRelative(Direction.EAST(), 1).getRelative(Direction.SOUTH(), 1));
		this.volume.setCornerTwo(locationBlock.getRelative(BlockFace.UP, 2).getRelative(Direction.WEST(), 1).getRelative(Direction.NORTH(), 1));
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
