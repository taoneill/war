package com.tommytony.war.job;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.NoteBlock;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import com.tommytony.war.utility.DeferredBlockReset;
import com.tommytony.war.utility.Direction;
import com.tommytony.war.volume.Volume;

public class DeferredBlockResetsJob implements Runnable {

	List<DeferredBlockReset> deferred = new ArrayList<DeferredBlockReset>();
	private final World world;

	public DeferredBlockResetsJob(World world) {
		this.world = world;

	}

	public void add(DeferredBlockReset pleaseResetLater) {
		this.deferred.add(pleaseResetLater);
	}

	public boolean isEmpty() {
		return this.deferred.isEmpty();
	}

	public void run() {
		ArrayList<DeferredBlockReset> doors = new ArrayList<DeferredBlockReset>();
		
		for (DeferredBlockReset reset : this.deferred) {
			if (this.world != null && reset != null) {
				Block worldBlock = this.world.getBlockAt(reset.getX(), reset.getY(), reset.getZ());
				worldBlock.setType(Material.getMaterial(reset.getBlockType()));
	
				if (reset.getBlockType() == Material.WALL_SIGN.getId() || reset.getBlockType() == Material.SIGN_POST.getId()) {
					BlockState state = worldBlock.getState();
					state.setData(new org.bukkit.material.Sign(reset.getBlockType(), reset.getBlockData()));
					if (state instanceof Sign) {
						Sign sign = (Sign) state;
						if (reset.getLines() != null && sign.getLines() != null) {
							if (reset.getLines().length > 0) {
								sign.setLine(0, reset.getLines()[0]);
							}
							if (reset.getLines().length > 1) {
								sign.setLine(1, reset.getLines()[1]);
							}
							if (reset.getLines().length > 2) {
								sign.setLine(2, reset.getLines()[2]);
							}
							if (reset.getLines().length > 3) {
								sign.setLine(3, reset.getLines()[3]);
							}
							sign.update(true);
						}
					}
				} else if (reset.getBlockType() == Material.CHEST.getId()
						|| reset.getBlockType() == Material.DISPENSER.getId()
						|| reset.getBlockType() == Material.FURNACE.getId() 
						|| reset.getBlockType() == Material.BURNING_FURNACE.getId()) {
					List<ItemStack> items = reset.getItems();
	
					worldBlock.setType(Material.getMaterial(reset.getBlockType()));
					worldBlock.setData(reset.getBlockData());
					BlockState state = worldBlock.getState();
					if (state instanceof InventoryHolder) {
						InventoryHolder container = (InventoryHolder) state;
						if (items != null) {
							int ii = 0;
							container.getInventory().clear();
							for (ItemStack item : items) {
								if (item != null) {
									container.getInventory().setItem(ii, item);
									ii++;
								}
							}
							state.update(true);
							items.clear();
						}
					} else {
						// normal reset
						worldBlock.setData(reset.getBlockData());
					}
				} else if (reset.getBlockType() == Material.NOTE_BLOCK.getId()) {
					worldBlock.setType(Material.getMaterial(reset.getBlockType()));
					worldBlock.setData(reset.getBlockData());
					BlockState state = worldBlock.getState();
					if (state instanceof NoteBlock && reset.getRawNote() != null) {
						NoteBlock noteBlock = (NoteBlock) state;
						noteBlock.setRawNote(reset.getRawNote());
						noteBlock.update(true);
					} else {
						// normal reset
						worldBlock.setData(reset.getBlockData());
					}
				} else if (reset.getBlockType() == Material.WOODEN_DOOR.getId() || reset.getBlockType() == Material.IRON_DOOR_BLOCK.getId()) {
					// Door blocks
					doors.add(reset);
				} else {
					// normal data reset
					worldBlock.setData(reset.getBlockData());
				}
			}
		}
		
		// Take care of doors last
		for (DeferredBlockReset doorBlock : doors) {
			Block worldBlock = world.getBlockAt(doorBlock.getX(), doorBlock.getY(), doorBlock.getZ());
			if (worldBlock.getTypeId() != doorBlock.getBlockType() || worldBlock.getData() != doorBlock.getBlockData()) {
				// find its friend 
				for (DeferredBlockReset other : doors) {
					if (other.getX() == doorBlock.getX()
						&& other.getY() == doorBlock.getY() - 1
						&& other.getZ() == doorBlock.getZ()) {
						// doorBlock is above
						Block above = worldBlock;
						Block below = world.getBlockAt(other.getX(), other.getY(), other.getZ());
						above.setTypeId(doorBlock.getBlockType());
						above.setData(doorBlock.getBlockData());
						below.setTypeId(other.getBlockType());
						below.setData(other.getBlockData());
						scrubDroppedDoors(below);
						break;
					} else if (other.getX() == doorBlock.getX()
						&& other.getY() == doorBlock.getY() + 1
						&& other.getZ() == doorBlock.getZ()) {
						// doorBlock is below
						Block above = world.getBlockAt(other.getX(), other.getY(), other.getZ());
						Block below = worldBlock;
						above.setTypeId(doorBlock.getBlockType());
						above.setData(doorBlock.getBlockData());
						below.setTypeId(other.getBlockType());
						below.setData(other.getBlockData());
						scrubDroppedDoors(below);
						break;
					}
				}
			}
		}
	}

	private void scrubDroppedDoors(Block block) {
		Chunk chunk = block.getWorld().getChunkAt(block);
		Volume scrubVolume = new Volume("scrub", block.getWorld());
		scrubVolume.setCornerOne(block.getRelative(BlockFace.DOWN).getRelative(Direction.EAST()).getRelative(Direction.NORTH()));
		scrubVolume.setCornerTwo(block.getRelative(BlockFace.UP).getRelative(Direction.WEST()).getRelative(Direction.SOUTH()));
		for (Entity entity : chunk.getEntities()) {
			if ((entity instanceof Item && (((Item)entity).getItemStack().getTypeId() == Material.IRON_DOOR.getId() 
												|| ((Item)entity).getItemStack().getTypeId() == Material.WOOD_DOOR.getId()))
					&& scrubVolume.contains(entity.getLocation())) {
				entity.remove();
			}
		}
	}

}
