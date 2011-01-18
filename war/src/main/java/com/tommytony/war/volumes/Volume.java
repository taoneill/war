package com.tommytony.war.volumes;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;

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
	private BlockInfo[][][] blockInfos = null;
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
				this.setBlockInfos(new BlockInfo[getSizeX()][getSizeY()][getSizeZ()]);
				int x = getMinX();
				for(int i = 0; i < getSizeX(); i++){
					int y = getMinY();
					for(int j = 0; j < getSizeY(); j++){
						int z = getMinZ();
						for(int k = 0;k < getSizeZ(); k++) {
							this.getBlockInfos()[i][j][k] = new BlockInfo(getWorld().getBlockAt(x, y, z));
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
			if(hasTwoCorners() && getBlockInfos() != null) {
				int x = getMinX();
				for(int i = 0; i < getSizeX(); i++){
					int y = getMinY();
					for(int j = 0; j < getSizeY(); j++){
						int z = getMinZ();
						for(int k = 0;k < getSizeZ(); k++) {
							BlockInfo oldBlockInfo = getBlockInfos()[i][j][k];
							Block currentBlock = getWorld().getBlockAt(x, y, z);
							if(currentBlock.getTypeId() != oldBlockInfo.getTypeId() ||
								(currentBlock.getTypeId() == oldBlockInfo.getTypeId() && currentBlock.getData() != oldBlockInfo.getData()) ||
								(currentBlock.getTypeId() == oldBlockInfo.getTypeId() && currentBlock.getData() == oldBlockInfo.getData() &&
										(oldBlockInfo.is(Material.SIGN) || oldBlockInfo.is(Material.SIGN_POST))
								)
							) {
								currentBlock.setType(oldBlockInfo.getType());
								currentBlock.setData(oldBlockInfo.getData());
								if(oldBlockInfo.is(Material.SIGN) || oldBlockInfo.is(Material.SIGN_POST)) {
									BlockState state = currentBlock.getState();
									Sign currentSign = (Sign) state;
									currentSign.setLine(0, oldBlockInfo.getSignLines()[0]);
									currentSign.setLine(1, oldBlockInfo.getSignLines()[1]);
									currentSign.setLine(2, oldBlockInfo.getSignLines()[2]);
									currentSign.setLine(3, oldBlockInfo.getSignLines()[3]);
									state.update();
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
			this.getWar().getLogger().warning("Failed to reset volume " + getName() + " blocks. " + e.getClass().toString() + " " + e.getMessage());
		}
		return noOfResetBlocks;
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
		return getBlockInfos() != null;
	}

	public BlockInfo[][][] getBlockInfos() {
		return blockInfos;
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

	public void setBlockInfos(BlockInfo[][][] blockInfos) {
		this.blockInfos = blockInfos;
	}

	public War getWar() {
		return war;
	}

	public String getName() {
		return name;
	}

	public void setToMaterial(Material material) {
		try {
			if(hasTwoCorners() && getBlockInfos() != null) {
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
			this.getWar().getLogger().warning("Failed to set block to " + material + "in volume " + name + "." + e.getClass().toString() + " " + e.getMessage());
		}
	}
	
	public void setFaceMaterial(BlockFace face, Material material) {
		try {
			if(hasTwoCorners() && getBlockInfos() != null) {
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
			this.getWar().getLogger().warning("Failed to set block to " + material + "in volume " + name + "." + e.getClass().toString() + " " + e.getMessage());
		}
	}
	
	private void switchMaterials(Material[] oldTypes, Material newType) {
		try {
			if(hasTwoCorners() && getBlockInfos() != null) {
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
			this.getWar().getLogger().warning("Failed to switch block to " + newType + "in volume " + name + "." + e.getClass().toString() + " " + e.getMessage());
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

}
