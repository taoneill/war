package com.tommytony.war.volume;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import org.apache.commons.lang.Validate;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Sign;
import org.bukkit.inventory.Inventory;
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
	private int[][][] blockTypes = null;
	private byte[][][] blockDatas = null;
	private HashMap<String, String[]> signLines = new HashMap<String, String[]>();
	private HashMap<String, List<ItemStack>> invBlockContents = new HashMap<String, List<ItemStack>>();
	private Location cornerOne;
	private Location cornerTwo;

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

	public int saveBlocks() {
		int noOfSavedBlocks = 0;
		int x = 0;
		int y = 0;
		int z = 0;
		try {
			if (this.hasTwoCorners()) {
				this.setBlockTypes(new int[this.getSizeX()][this.getSizeY()][this.getSizeZ()]);
				this.setBlockDatas(new byte[this.getSizeX()][this.getSizeY()][this.getSizeZ()]);
				this.getSignLines().clear();
				this.getInvBlockContents().clear();
				x = this.getMinX();
				for (int i = 0; i < this.getSizeX(); i++) {
					y = this.getMinY();
					for (int j = 0; j < this.getSizeY(); j++) {
						z = this.getMinZ();
						for (int k = 0; k < this.getSizeZ(); k++) {
							try {
								Block block = this.getWorld().getBlockAt(x, y, z);
								this.getBlockTypes()[i][j][k] = block.getTypeId();
								this.getBlockDatas()[i][j][k] = block.getData();
								BlockState state = block.getState();
								if (state instanceof Sign) {
									// Signs
									Sign sign = (Sign) state;
									if (sign.getLines() != null) {
										this.getSignLines().put("sign-" + i + "-" + j + "-" + k, sign.getLines());
									}

								} else if (state instanceof Chest) {
									// Chests
									Chest chest = (Chest) state;
									Inventory inv = chest.getInventory();
									int size = inv.getSize();
									List<ItemStack> items = new ArrayList<ItemStack>();
									for (int invIndex = 0; invIndex < size; invIndex++) {
										ItemStack item = inv.getItem(invIndex);
										if (item != null && item.getType().getId() != Material.AIR.getId()) {
											items.add(item);
										}
									}
									this.getInvBlockContents().put("chest-" + i + "-" + j + "-" + k, items);
								} else if (state instanceof Dispenser) {
									// Dispensers
									Dispenser dispenser = (Dispenser) state;
									Inventory inv = dispenser.getInventory();
									int size = inv.getSize();
									List<ItemStack> items = new ArrayList<ItemStack>();
									for (int invIndex = 0; invIndex < size; invIndex++) {
										ItemStack item = inv.getItem(invIndex);
										if (item != null && item.getType().getId() != Material.AIR.getId()) {
											items.add(item);
										}
									}
									this.getInvBlockContents().put("dispenser-" + i + "-" + j + "-" + k, items);
								}

								noOfSavedBlocks++;
							} catch (Exception e) {
								War.war.getLogger().warning("Failed to save block in volume " + this.getName() + ". Saved blocks so far:" + noOfSavedBlocks + ". Error at x:" + x + " y:" + y + " z:" + z + ". Exception:" + e.getClass().toString() + e.getMessage());
								e.printStackTrace();
							} finally {
								z++;
							}
						}
						y++;
					}
					x++;
				}
			}
		} catch (Exception e) {
			War.war.getLogger().warning("Failed to save volume " + this.getName() + " blocks. Saved blocks:" + noOfSavedBlocks + ". Error at x:" + x + " y:" + y + " z:" + z + ". Exception:" + e.getClass().toString() + " " + e.getMessage());
			e.printStackTrace();
		}
		return noOfSavedBlocks;
	}

	public void resetBlocksAsJob() {
		BlockResetJob job = new BlockResetJob(this);
		War.war.getServer().getScheduler().scheduleSyncDelayedTask(War.war, job);
	}

	public int resetBlocks() {
		int visitedBlocks = 0, noOfResetBlocks = 0, x = 0, y = 0, z = 0;
		int currentBlockId = 0;
		int oldBlockType = 0;
		this.clearBlocksThatDontFloat();
		try {
			if (this.hasTwoCorners() && this.isSaved()) {
				x = this.getMinX();
				for (int i = 0; i < this.getSizeX(); i++) {
					y = this.getMinY();
					for (int j = 0; j < this.getSizeY(); j++) {
						z = this.getMinZ();
						for (int k = 0; k < this.getSizeZ(); k++) {
							try {
								oldBlockType = this.getBlockTypes()[i][j][k];
								byte oldBlockData = this.getBlockDatas()[i][j][k];
								Block currentBlock = this.getWorld().getBlockAt(x, y, z);
								currentBlockId = currentBlock.getTypeId();
								if (currentBlockId != oldBlockType || (currentBlockId == oldBlockType && currentBlock.getData() != oldBlockData) || (currentBlockId == oldBlockType && currentBlock.getData() == oldBlockData && (oldBlockType == Material.WALL_SIGN.getId() || oldBlockType == Material.SIGN_POST.getId() || oldBlockType == Material.CHEST.getId() || oldBlockType == Material.DISPENSER.getId()))) {
									if (oldBlockType == Material.WALL_SIGN.getId() || oldBlockType == Material.SIGN_POST.getId()) {
										// Signs
										if (oldBlockType == Material.SIGN_POST.getId() && ((oldBlockData & 0x04) == 0x04) && i + 1 != this.getSizeX()) {
											Block southBlock = currentBlock.getRelative(Direction.SOUTH());
											int oldSouthBlockType = this.getBlockTypes()[i + 1][j][k];
											byte oldSouthBlockData = this.getBlockDatas()[i + 1][j][k];
											if (southBlock.getTypeId() != oldSouthBlockType) {
												southBlock.setTypeId(oldSouthBlockType);
												southBlock.setData(oldSouthBlockData);
											}
										}
										currentBlock.setType(Material.getMaterial(oldBlockType));
										BlockState state = currentBlock.getState();
										state.setData(new org.bukkit.material.Sign(oldBlockType, oldBlockData));
										if (state instanceof Sign) {
											Sign sign = (Sign) state;
											String[] lines = this.getSignLines().get("sign-" + i + "-" + j + "-" + k);
											if (lines != null && sign.getLines() != null) {
												if (lines.length > 0) {
													sign.setLine(0, lines[0]);
												}
												if (lines.length > 1) {
													sign.setLine(1, lines[1]);
												}
												if (lines.length > 2) {
													sign.setLine(2, lines[2]);
												}
												if (lines.length > 3) {
													sign.setLine(3, lines[3]);
												}
												sign.update(true);
											}
										}
									} else if (oldBlockType == Material.CHEST.getId()) {
										// Chests
										currentBlock.setType(Material.getMaterial(oldBlockType));
										currentBlock.setData(oldBlockData);
										BlockState state = currentBlock.getState();
										if (state instanceof Chest) {
											Chest chest = (Chest) state;
											List<ItemStack> contents = this.getInvBlockContents().get("chest-" + i + "-" + j + "-" + k);
											if (contents != null) {
												int ii = 0;
												chest.getInventory().clear();
												for (ItemStack item : contents) {
													if (item != null) {
														chest.getInventory().setItem(ii, item);
														ii++;
													}
												}
												chest.update(true);
											}
										}
									} else if (oldBlockType == Material.DISPENSER.getId()) {
										// Dispensers
										currentBlock.setType(Material.getMaterial(oldBlockType));
										currentBlock.setData(oldBlockData);
										BlockState state = currentBlock.getState();
										if (state instanceof Dispenser) {
											Dispenser dispenser = (Dispenser) state;
											List<ItemStack> contents = this.getInvBlockContents().get("dispenser-" + i + "-" + j + "-" + k);
											if (contents != null) {
												int ii = 0;
												dispenser.getInventory().clear();
												for (ItemStack item : contents) {
													if (item != null) {
														dispenser.getInventory().setItem(ii, item);
														ii++;
													}
												}
												dispenser.update(true);
											}
										}
									} else if (oldBlockType == Material.WOODEN_DOOR.getId() || oldBlockType == Material.IRON_DOOR_BLOCK.getId()) {
										// Door blocks

										// Check if is bottom door block
										if (j + 1 < this.getSizeY() && this.getBlockTypes()[i][j + 1][k] == oldBlockType) {
											// set both door blocks right away
											
											Block blockAbove = this.getWorld().getBlockAt(x, y + 1, z);
											blockAbove.setType(Material.getMaterial(oldBlockType));
											blockAbove.setData(this.getBlockDatas()[i][j + 1][k]);
											
											currentBlock.setType(Material.getMaterial(oldBlockType));
											currentBlock.setData(oldBlockData);
										}
									} else if (((oldBlockType == Material.TORCH.getId() && ((oldBlockData & 0x02) == 0x02)) || (oldBlockType == Material.REDSTONE_TORCH_OFF.getId() && ((oldBlockData & 0x02) == 0x02)) || (oldBlockType == Material.REDSTONE_TORCH_ON.getId() && ((oldBlockData & 0x02) == 0x02)) || (oldBlockType == Material.LEVER.getId() && ((oldBlockData & 0x02) == 0x02)) || (oldBlockType == Material.STONE_BUTTON.getId() && ((oldBlockData & 0x02) == 0x02)) || (oldBlockType == Material.LADDER.getId() && ((oldBlockData & 0x04) == 0x04)) || (oldBlockType == Material.RAILS.getId() && ((oldBlockData & 0x02) == 0x02))) && i + 1 != this.getSizeX()) {
										// Blocks that hang on a block south of themselves need to make sure that block is there before placing themselves... lol
										Block southBlock = currentBlock.getRelative(Direction.SOUTH());
										int oldSouthBlockType = this.getBlockTypes()[i + 1][j][k];
										byte oldSouthBlockData = this.getBlockDatas()[i + 1][j][k];
										if (southBlock.getTypeId() != oldSouthBlockType) {
											southBlock.setTypeId(oldSouthBlockType);
											southBlock.setData(oldSouthBlockData);
										}
										// change the block itself, now that we have a block to set it on
										currentBlock.setType(Material.getMaterial(oldBlockType));
										currentBlock.setData(oldBlockData);
									} else {
										// regular block
										currentBlock.setType(Material.getMaterial(oldBlockType));
										currentBlock.setData(oldBlockData);
									}
									noOfResetBlocks++;
								}
								visitedBlocks++;
							} catch (Exception e) {
								War.war.getLogger().warning("Failed to reset block in volume " + this.getName() + ". Visited blocks so far:" + visitedBlocks + ". Blocks reset: " + noOfResetBlocks + ". Error at x:" + x + " y:" + y + " z:" + z + ". Exception:" + e.getClass().toString() + " " + e.getMessage());
								e.printStackTrace();
							} finally {
								z++;
							}
						}
						y++;
					}
					x++;
				}
			}
		} catch (Exception e) {
			War.war.log("Failed to reset volume " + this.getName() + " blocks. Blocks visited: " + visitedBlocks + ". Blocks reset: " + noOfResetBlocks + ". Error at x:" + x + " y:" + y + " z:" + z + ". Current block: " + currentBlockId + ". Old block: " + oldBlockType + ". Exception: " + e.getClass().toString() + " " + e.getMessage(), Level.WARNING);
			e.printStackTrace();
		}
		return noOfResetBlocks;
	}

	public byte[][][] getBlockDatas() {
		return this.blockDatas;
	}

	public void setBlockDatas(byte[][][] data) {
		this.blockDatas = data;
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
		return this.getBlockTypes() != null;
	}

	public int[][][] getBlockTypes() {
		return this.blockTypes;
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

	public void setBlockTypes(int[][][] blockTypes) {
		this.blockTypes = blockTypes;
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

	private static final Material[] nonFloatingBlocks = {
		Material.SIGN_POST,
		Material.WALL_SIGN,
		Material.IRON_DOOR,
		Material.WOOD_DOOR,
		Material.LADDER,
		Material.YELLOW_FLOWER,
		Material.RED_ROSE,
		Material.RED_MUSHROOM,
		Material.BROWN_MUSHROOM,
		Material.SAPLING,
		Material.TORCH,
		Material.RAILS,
		Material.STONE_BUTTON,
		Material.STONE_PLATE,
		Material.WOOD_PLATE,
		Material.LEVER,
		Material.REDSTONE,
		Material.REDSTONE_TORCH_ON,
		Material.REDSTONE_TORCH_OFF,
		Material.CACTUS,
		Material.SNOW,
		Material.ICE
	};

	public void clearBlocksThatDontFloat() {
		this.replaceMaterials(nonFloatingBlocks, Material.AIR);
	}

	public void setSignLines(HashMap<String, String[]> signLines) {
		this.signLines = signLines;
	}

	public HashMap<String, String[]> getSignLines() {
		return this.signLines;
	}

	public void setInvBlockContents(HashMap<String, List<ItemStack>> invBlockContents) {
		this.invBlockContents = invBlockContents;
	}

	public HashMap<String, List<ItemStack>> getInvBlockContents() {
		return this.invBlockContents;
	}

	@Override
	public void finalize() {
		this.blockDatas = null;
		this.blockTypes = null;
		this.signLines.clear();
		this.signLines = null;
		this.invBlockContents.clear();
		this.invBlockContents = null;
	}

	public int size() {
		return this.getSizeX() * this.getSizeY() * this.getSizeZ();
	}
}
