package com.tommytony.war.volumes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
import org.bukkit.material.MaterialData;

import bukkit.tommytony.war.War;

/**
 * 
 * @author tommytony
 *
 */
public class Volume {
	private final String name;
	private final World world;
	//private final Warzone warzone;
	private Block cornerOne;
	private Block cornerTwo;
	private int[][][] blockTypes = null;
	private byte[][][] blockDatas = null;
	private HashMap<String, String[]> signLines = new HashMap<String, String[]>();
	private HashMap<String, List<ItemStack>> invBlockContents = new HashMap<String, List<ItemStack>>();
	private final War war;	

	public Volume(String name, War war, World world) {
		this.name = name;
		this.war = war;
		this.world = world;
	}
	
	public World getWorld() {
		return world;
	}
	
	public boolean hasTwoCorners() {
		return cornerOne != null && cornerTwo != null;
	}
	
	public void setCornerOne(Block block) {
		this.cornerOne = block;
	}
	
	public int saveBlocks() {
		int noOfSavedBlocks = 0;
		try {
			if(hasTwoCorners()) {
				this.setBlockTypes(new int[getSizeX()][getSizeY()][getSizeZ()]);
				this.setBlockDatas(new byte[getSizeX()][getSizeY()][getSizeZ()]);
				this.getSignLines().clear();
				int x = getMinX();
				for(int i = 0; i < getSizeX(); i++){
					int y = getMinY();
					for(int j = 0; j < getSizeY(); j++){
						int z = getMinZ();
						for(int k = 0;k < getSizeZ(); k++) {
							Block block = getWorld().getBlockAt(x, y, z);
							this.getBlockTypes()[i][j][k] = block.getTypeId();
							this.getBlockDatas()[i][j][k] = block.getData();
							BlockState state = block.getState();
							if(state instanceof Sign) {
								// Signs
								Sign sign = (Sign)state;
								this.getSignLines().put("sign-" + i + "-" + j + "," + k, sign.getLines());	
							} else if(state instanceof Chest) {
								// Chests
								Chest chest = (Chest)state;
								Inventory inv = chest.getInventory();
								int size = inv.getSize();
								List<ItemStack> items = new ArrayList<ItemStack>();
								for(int invIndex = 0; invIndex < size; invIndex++){
									ItemStack item = inv.getItem(invIndex);
									if(item != null && item.getType().getId() != Material.AIR.getId()) {
										items.add(item);
									}
								}
								this.getInvBlockContents().put("chest-" + i + "-" + j + "," + k, items);
							} else if(state instanceof Dispenser) {
								// Dispensers							
								Dispenser dispenser = (Dispenser)state;
								Inventory inv = dispenser.getInventory();
								int size = inv.getSize();
								List<ItemStack> items = new ArrayList<ItemStack>();
								for(int invIndex = 0; invIndex < size; invIndex++){
									ItemStack item = inv.getItem(invIndex);
									if(item != null && item.getType().getId() != Material.AIR.getId()) {
										items.add(item);
									}
								}
								this.getInvBlockContents().put("dispenser-" + i + "-" + j + "," + k, items);
							}
							z++;
							noOfSavedBlocks++;
						}
						y++;
					}
					x++;
				}
			}		
		} catch (Exception e) {
			this.getWar().getLogger().warning("Failed to save volume " + getName() + " blocks. " + e.getMessage());
		}
		return noOfSavedBlocks;
	}
	
	public int resetBlocks() {
		int noOfResetBlocks = 0;
		clearBlocksThatDontFloat();
		try {
			if(hasTwoCorners() && getBlockTypes() != null) {
				int x = getMinX();
				for(int i = 0; i < getSizeX(); i++){
					int y = getMinY();
					for(int j = 0; j < getSizeY(); j++){
						int z = getMinZ();
						for(int k = 0;k < getSizeZ(); k++) {
							int oldBlockType = getBlockTypes()[i][j][k];
							byte oldBlockData = getBlockDatas()[i][j][k];
							Block currentBlock = getWorld().getBlockAt(x, y, z);
							if(currentBlock.getTypeId() != oldBlockType ||
								(currentBlock.getTypeId() == oldBlockType && currentBlock.getData() != oldBlockData ) ||
								(currentBlock.getTypeId() == oldBlockType && currentBlock.getData() == oldBlockData &&
										(oldBlockType == Material.SIGN.getId() || oldBlockType == Material.SIGN_POST.getId()
												|| oldBlockType == Material.CHEST.getId() || oldBlockType == Material.DISPENSER.getId())
								)
							) {
								
//								if(oldBlockInfo.is(Material.SIGN) || oldBlockInfo.is(Material.SIGN_POST)) {
//									BlockState state = currentBlock.getState();
//									Sign currentSign = (Sign) state;
//									currentSign.setLine(0, oldBlockInfo.getSignLines()[0]);
//									currentSign.setLine(1, oldBlockInfo.getSignLines()[1]);
//									currentSign.setLine(2, oldBlockInfo.getSignLines()[2]);
//									currentSign.setLine(3, oldBlockInfo.getSignLines()[3]);
//									state.update();
//								}
								BlockState state = currentBlock.getState();
								if(oldBlockType == Material.SIGN.getId() 
										|| oldBlockType == Material.SIGN_POST.getId()) {
									// Signs
									state.setType(Material.getMaterial(oldBlockType));
									state.setData(new MaterialData(oldBlockType, oldBlockData));
									state.update(true);
									state = war.refetchStateForBlock(world, state.getBlock());
									Sign sign = (Sign)state;
									String[] lines = this.getSignLines().get("sign-" + i + "-" + j + "," + k);
									sign.setLine(0, lines[0]);
									sign.setLine(1, lines[1]);
									sign.setLine(2, lines[2]);
									sign.setLine(3, lines[3]);
									sign.update(true);
								} else if(oldBlockType == Material.CHEST.getId()) {
									// Chests
									state.setType(Material.getMaterial(oldBlockType));
									state.setData(new MaterialData(oldBlockType, oldBlockData));
									state.update(true);
									state = war.refetchStateForBlock(world, state.getBlock());
									Chest chest = (Chest)state;
									List<ItemStack> contents = this.getInvBlockContents().get("chest-" + i + "-" + j + "," + k);
									int ii = 0;
									chest.getInventory().clear();
									for(ItemStack item : contents) {
										chest.getInventory().setItem(ii, item);
										ii++;
									}
									chest.update(true);
								} else if(oldBlockType == Material.DISPENSER.getId()) {
									// Dispensers		
									state.setType(Material.getMaterial(oldBlockType));
									state.setData(new MaterialData(oldBlockType, oldBlockData));
									state.update(true);
									state = war.refetchStateForBlock(world, state.getBlock());
									Dispenser dispenser = (Dispenser)state;
									List<ItemStack> contents = this.getInvBlockContents().get("dispenser-" + i + "-" + j + "," + k);
									int ii = 0;
									dispenser.getInventory().clear();
									for(ItemStack item : contents) {
										dispenser.getInventory().setItem(ii, item);
										ii++;
									}
									dispenser.update(true);
								} else {
									// regular block
									currentBlock.setType(Material.getMaterial(oldBlockType));
									currentBlock.setData(oldBlockData);
								}
								noOfResetBlocks++;
							}
							z++;
						}
						y++;
					}
					x++;
				}
			}		
		} catch (Exception e) {
			this.getWar().warn("Failed to reset volume " + getName() + " blocks. " + e.getClass().toString() + " " + e.getMessage());
		}
		return noOfResetBlocks;
	}

	public byte[][][] getBlockDatas() {
		// TODO Auto-generated method stub
		return this.blockDatas;
	}
	
	public void setBlockDatas(byte[][][] data) {
		this.blockDatas = data;
	}
	

	public void setCornerTwo(Block block) {
		this.cornerTwo = block;
	}
	
	public Block getMinXBlock() {
		if(cornerOne.getX() < cornerTwo.getX()) return cornerOne;
		return cornerTwo;
	}
	
	public Block getMinYBlock() {
		if(cornerOne.getY() < cornerTwo.getY()) return cornerOne;
		return cornerTwo;
	}
	
	public Block getMinZBlock() {
		if(cornerOne.getZ() < cornerTwo.getZ()) return cornerOne;
		return cornerTwo;
	}
	
	public int getMinX() {
		return getMinXBlock().getX();
	}
	
	public int getMinY() {
		return getMinYBlock().getY();
	}
	
	public int getMinZ() {
		return getMinZBlock().getZ();
	}
	
	public Block getMaxXBlock() {
		if(cornerOne.getX() < cornerTwo.getX()) return cornerTwo;
		return cornerOne;
	}
	
	public Block getMaxYBlock() {
		if(cornerOne.getY() < cornerTwo.getY()) return cornerTwo;
		return cornerOne;
	}
	
	public Block getMaxZBlock() {
		if(cornerOne.getZ() < cornerTwo.getZ()) return cornerTwo;
		return cornerOne;
	}
	
	public int getMaxX() {
		return getMaxXBlock().getX();
	}
	
	public int getMaxY() {
		return getMaxYBlock().getY();
	}
	
	public int getMaxZ() {
		return getMaxZBlock().getZ();
	}
	
	public int getSizeX() {
		return getMaxX() - getMinX() + 1;
	}
	
	public int getSizeY() {
		return getMaxY() - getMinY() + 1;
	}
	
	public int getSizeZ() {
		return getMaxZ() - getMinZ() + 1;
	}	

	public boolean isSaved() {
		return getBlockTypes() != null;
	}

	public int[][][] getBlockTypes() {
		return blockTypes;
	}
	
	public Block getCornerOne() {
		return cornerOne;
	}
	
	public Block getCornerTwo() {
		return cornerTwo;
	}

	public boolean contains(Location location) {
		int x = location.getBlockX();
		int y = location.getBlockY();
		int z = location.getBlockZ();
		return hasTwoCorners() && x <= getMaxX() && x >= getMinX() && 
				y <= getMaxY() && y >= getMinY() &&
				z <= getMaxZ() && z >= getMinZ();
	}

	public boolean contains(Block block) {
		int x = block.getX();
		int y = block.getY();
		int z = block.getZ();
		return hasTwoCorners() && x <= getMaxX() && x >= getMinX() && 
				y <= getMaxY() && y >= getMinY() &&
				z <= getMaxZ() && z >= getMinZ();
	}

	public void setBlockTypes(int[][][] blockTypes) {
		this.blockTypes = blockTypes;
	}

	public War getWar() {
		return war;
	}

	public String getName() {
		return name;
	}

	public void setToMaterial(Material material) {
		try {
			if(hasTwoCorners() && getBlockTypes() != null) {
				int x = getMinX();
				for(int i = 0; i < getSizeX(); i++){
					int y = getMinY();
					for(int j = 0; j < getSizeY(); j++){
						int z = getMinZ();
						for(int k = 0;k < getSizeZ(); k++) {
							Block currentBlock = getWorld().getBlockAt(x, y, z);
							currentBlock.setType(material);
							z++;
						}
						y++;
					}
					x++;
				}
			}		
		} catch (Exception e) {
			this.getWar().warn("Failed to set block to " + material + "in volume " + name + "." + e.getClass().toString() + " " + e.getMessage());
		}
	}
	
	public void setFaceMaterial(BlockFace face, Material material) {
		try {
			if(hasTwoCorners() && getBlockTypes() != null) {
				int x = getMinX();
				for(int i = 0; i < getSizeX(); i++){
					int y = getMinY();
					for(int j = 0; j < getSizeY(); j++){
						int z = getMinZ();
						for(int k = 0;k < getSizeZ(); k++) {
							if((face == BlockFace.DOWN && y == getMinY()) 
									|| (face == BlockFace.UP && y == getMaxY())
									|| (face == BlockFace.NORTH && x == getMinX())
									|| (face == BlockFace.EAST && z == getMinZ())
									|| (face == BlockFace.SOUTH && x == getMaxX())
									|| (face == BlockFace.WEST && z == getMaxZ())) {
								Block currentBlock = getWorld().getBlockAt(x, y, z);
								currentBlock.setType(material);
							}
							z++;
						}
						y++;
					}
					x++;
				}
			}		
		} catch (Exception e) {
			this.getWar().warn("Failed to set block to " + material + "in volume " + name + "." + e.getClass().toString() + " " + e.getMessage());
		}
	}
	
	private void switchMaterials(Material[] oldTypes, Material newType) {
		try {
			if(hasTwoCorners() && getBlockTypes() != null) {
				int x = getMinX();
				for(int i = 0; i < getSizeX(); i++){
					int y = getMinY();
					for(int j = 0; j < getSizeY(); j++){
						int z = getMinZ();
						for(int k = 0;k < getSizeZ(); k++) {
							Block currentBlock = getWorld().getBlockAt(x, y, z);
							for(Material oldType : oldTypes) {
								if(currentBlock.getType().getId() == oldType.getId()) {
									currentBlock.setType(newType);
//									BlockState state = currentBlock.getState();
//									if(state != null) {
//										state.setType(newType);
//										state.update(true);
//									}
								}
							}
							z++;
						}
						y++;
					}
					x++;
				}
			}	
		} catch (Exception e) {
			this.getWar().warn("Failed to switch block to " + newType + "in volume " + name + "." + e.getClass().toString() + " " + e.getMessage());
		}
	}

	public void clearBlocksThatDontFloat() {
		Material[] toAirMaterials = new Material[22];
		toAirMaterials[0] = Material.SIGN_POST;
		toAirMaterials[1] = Material.SIGN;
		toAirMaterials[2] = Material.IRON_DOOR_BLOCK;
		toAirMaterials[3] = Material.WOOD_DOOR;
		toAirMaterials[4] = Material.LADDER;
		toAirMaterials[5] = Material.YELLOW_FLOWER;
		toAirMaterials[6] = Material.RED_ROSE;
		toAirMaterials[7] = Material.RED_MUSHROOM;
		toAirMaterials[8] = Material.BROWN_MUSHROOM;
		toAirMaterials[9] = Material.SAPLING;
		toAirMaterials[10] = Material.TORCH;
		toAirMaterials[11] = Material.RAILS;
		toAirMaterials[12] = Material.STONE_BUTTON;
		toAirMaterials[13] = Material.STONE_PLATE;
		toAirMaterials[14] = Material.WOOD_PLATE;
		toAirMaterials[15] = Material.LEVER;
		toAirMaterials[16] = Material.REDSTONE;
		toAirMaterials[17] = Material.REDSTONE_TORCH_ON;
		toAirMaterials[18] = Material.REDSTONE_TORCH_OFF;
		toAirMaterials[19] = Material.CACTUS;
		toAirMaterials[20] = Material.SNOW;
		toAirMaterials[21] = Material.ICE;
		switchMaterials(toAirMaterials, Material.AIR);
	}

	public void setSignLines(HashMap<String, String[]> signLines) {
		this.signLines = signLines;
	}

	public HashMap<String, String[]> getSignLines() {
		return signLines;
	}

	public void setInvBlockContents(HashMap<String, List<ItemStack>> invBlockContents) {
		this.invBlockContents = invBlockContents;
	}

	public HashMap<String, List<ItemStack>> getInvBlockContents() {
		return invBlockContents;
	}

}
