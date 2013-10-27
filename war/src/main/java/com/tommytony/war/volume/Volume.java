package com.tommytony.war.volume;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.ItemStack;

import com.tommytony.war.War;
import com.tommytony.war.job.BlockResetJob;
import com.tommytony.war.utility.Direction;

/**
 *
 * @author tommytony
 *
 */
public class Volume {
	private String name;
	private World world;
	private Location cornerOne;
	private Location cornerTwo;
	private List<BlockState> blocks = new ArrayList<BlockState>();

	public Volume(String name, World world) {
		this.name = name;
		this.world = world;
	}

	public Volume(World world) {
		this(null, world);
	}

	public Volume(Location corner1, Location corner2) {
		this(corner1.getWorld());
		Validate.isTrue(corner1.getWorld() == corner2.getWorld(), "Cross-world volume");
		this.cornerOne = corner1;
		this.cornerTwo = corner2;
	}
	
	public void setName(String newName) {
		this.name = newName;
	}

	public World getWorld() {
		return this.world;
	}
	
	public void setWorld(World world) {
		this.world = world;
	}

	public boolean hasTwoCorners() {
		return this.cornerOne != null && this.cornerTwo != null;
	}

	public void setCornerOne(Block block) {
		this.cornerOne = block.getLocation();
	}

	public void setCornerOne(Location location) {
		this.cornerOne = location;
	}

	public void saveBlocks() {
		this.blocks.clear();
		for (int x = this.getMinX(); x <= this.getMaxX(); x++) {
			for (int y = this.getMinY(); y <= this.getMaxY(); y++) {
				for (int z = this.getMinZ(); z <= this.getMaxZ(); z++) {
					this.blocks.add(world.getBlockAt(x, y, z).getState());
				}
			}
		}
	}

	public void resetBlocksAsJob() {
		BlockResetJob job = new BlockResetJob(this);
		War.war.getServer().getScheduler().scheduleSyncDelayedTask(War.war, job);
	}

	public void resetBlocks() {
		for (BlockState state : this.blocks) {
			state.update(true, false);
		}
	}

	public void setCornerTwo(Block block) {
		this.cornerTwo = block.getLocation();
	}

	public void setCornerTwo(Location location) {
		this.cornerTwo = location;
	}

	public Location getMinXBlock() {
		if (this.cornerOne.getX() < this.cornerTwo.getX()) {
			return this.cornerOne;
		}
		return this.cornerTwo;
	}

	public Location getMinYBlock() {
		if (this.cornerOne.getY() < this.cornerTwo.getY()) {
			return this.cornerOne;
		}
		return this.cornerTwo;
	}

	public Location getMinZBlock() {
		if (this.cornerOne.getZ() < this.cornerTwo.getZ()) {
			return this.cornerOne;
		}
		return this.cornerTwo;
	}

	public int getMinX() {
		return this.getMinXBlock().getBlockX();
	}

	public int getMinY() {
		return this.getMinYBlock().getBlockY();
	}

	public int getMinZ() {
		return this.getMinZBlock().getBlockZ();
	}

	public Location getMaxXBlock() {
		if (this.cornerOne.getX() < this.cornerTwo.getX()) {
			return this.cornerTwo;
		}
		return this.cornerOne;
	}

	public Location getMaxYBlock() {
		if (this.cornerOne.getY() < this.cornerTwo.getY()) {
			return this.cornerTwo;
		}
		return this.cornerOne;
	}

	public Location getMaxZBlock() {
		if (this.cornerOne.getZ() < this.cornerTwo.getZ()) {
			return this.cornerTwo;
		}
		return this.cornerOne;
	}

	public int getMaxX() {
		return this.getMaxXBlock().getBlockX();
	}

	public int getMaxY() {
		return this.getMaxYBlock().getBlockY();
	}

	public int getMaxZ() {
		return this.getMaxZBlock().getBlockZ();
	}

	public int getSizeX() {
		return this.getMaxX() - this.getMinX() + 1;
	}

	public int getSizeY() {
		return this.getMaxY() - this.getMinY() + 1;
	}

	public int getSizeZ() {
		return this.getMaxZ() - this.getMinZ() + 1;
	}

	public boolean isSaved() {
		return this.blocks.size() > 0;
	}

	public List<BlockState> getBlocks() {
		return blocks;
	}

	public void setBlocks(List<BlockState> blocks) {
		this.blocks = blocks;
	}

	public Location getCornerOne() {
		return this.cornerOne;
	}

	public Location getCornerTwo() {
		return this.cornerTwo;
	}

	public boolean contains(Location location) {
		int x = location.getBlockX();
		int y = location.getBlockY();
		int z = location.getBlockZ();
		return this.hasTwoCorners() && location.getWorld().getName().equals(this.world.getName()) && x <= this.getMaxX() && x >= this.getMinX() && y <= this.getMaxY() && y >= this.getMinY() && z <= this.getMaxZ() && z >= this.getMinZ();
	}

	public boolean contains(Block block) {
		int x = block.getX();
		int y = block.getY();
		int z = block.getZ();
		return this.hasTwoCorners() && block.getWorld().getName().equals(this.world.getName()) && x <= this.getMaxX() && x >= this.getMinX() && y <= this.getMaxY() && y >= this.getMinY() && z <= this.getMaxZ() && z >= this.getMinZ();
	}

	public String getName() {
		return this.name;
	}

	public void setToMaterial(Material material) {
		Validate.notNull(material);
		Validate.isTrue(this.hasTwoCorners(), "Incomplete volume");
		for (int x = this.getMinX(); x <= this.getMaxX(); x++) {
			for (int y = this.getMinY(); y <= this.getMaxY(); y++) {
				for (int z = this.getMinZ(); z <= this.getMaxZ(); z++) {
					this.getWorld().getBlockAt(x, y, z).setType(material);
				}
			}
		}
	}

	public void setFaceMaterial(BlockFace face, ItemStack faceBlock) {
		Validate.isTrue(this.hasTwoCorners(), "Incomplete volume");
		for (int x = this.getMinX(); x <= this.getMaxX(); x++) {
			for (int y = this.getMinY(); y <= this.getMaxY(); y++) {
				for (int z = this.getMinZ(); z <= this.getMaxZ(); z++) {
					if ((face == BlockFace.DOWN && y == this.getMinY())
							|| (face == BlockFace.UP && y == this.getMaxY())
							|| (face == Direction.NORTH() && x == this.getMinX())
							|| (face == Direction.EAST() && z == this.getMinZ())
							|| (face == Direction.SOUTH() && x == this.getMaxX())
							|| (face == Direction.WEST() && z == this.getMaxZ())) {
						BlockState currentBlock = this.getWorld().getBlockAt(x, y, z).getState();
						currentBlock.setType(faceBlock.getType());
						currentBlock.setData(faceBlock.getData());
						currentBlock.update(true);
					}
				}
			}
		}
	}
	
	public void setFloorOutline(ItemStack outlineBlock) {
		Validate.isTrue(this.hasTwoCorners(), "Incomplete volume");
		for (int x = this.getMinX(); x <= this.getMaxX(); x++) {
			for (int z = this.getMinZ(); z <= this.getMaxZ(); z++) {
				if (x == this.getMinX() || x == this.getMaxX() || z == this.getMinZ() || z == this.getMaxZ()) {
					BlockState currentBlock = this.getWorld().getBlockAt(x, this.getMinY(), z).getState();
					currentBlock.setType(outlineBlock.getType());
					currentBlock.setData(outlineBlock.getData());
					currentBlock.update(true);
				}
			}
		}
	}

	public void replaceMaterial(Material original, Material replacement) {
		Validate.isTrue(this.hasTwoCorners(), "Incomplete volume");
		for (int x = this.getMinX(); x <= this.getMaxX(); x++) {
			for (int y = this.getMinY(); y <= this.getMaxY(); y++) {
				for (int z = this.getMinZ(); z <= this.getMaxZ(); z++) {
					if (this.getWorld().getBlockAt(x, y, z).getType() == original) {
						this.getWorld().getBlockAt(x, y, z).setType(replacement);
					}
				}
			}
		}
	}

	public void replaceMaterials(Material[] materials, Material replacement) {
		for (Material mat: materials) {
			this.replaceMaterial(mat, replacement);
		}
	}

	public void clearBlocksThatDontFloat() {
		Validate.isTrue(this.hasTwoCorners(), "Incomplete volume");
		for (int x = this.getMinX(); x <= this.getMaxX(); x++) {
			for (int y = this.getMinY(); y <= this.getMaxY(); y++) {
				for (int z = this.getMinZ(); z <= this.getMaxZ(); z++) {
					switch (this.getWorld().getBlockAt(x, y, z).getType()) {
					case SIGN_POST:
					case WALL_SIGN:
					case IRON_DOOR:
					case WOOD_DOOR:
					case LADDER:
					case YELLOW_FLOWER:
					case RED_ROSE:
					case RED_MUSHROOM:
					case BROWN_MUSHROOM:
					case SAPLING:
					case TORCH:
					case RAILS:
					case STONE_BUTTON:
					case STONE_PLATE:
					case WOOD_PLATE:
					case LEVER:
					case REDSTONE:
					case REDSTONE_TORCH_ON:
					case REDSTONE_TORCH_OFF:
					case CACTUS:
					case SNOW:
					case ICE:
						this.getWorld().getBlockAt(x, y, z)
								.setType(Material.AIR);
					default:
						break;
					}
				}
			}
		}
	}

	@Override
	public void finalize() {
		this.blocks.clear();
		this.blocks = null;
	}

	public int size() {
		return this.getSizeX() * this.getSizeY() * this.getSizeZ();
	}
}
