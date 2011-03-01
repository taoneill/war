package com.tommytony.war.jobs;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Sign;
import org.bukkit.inventory.ItemStack;

import com.tommytony.war.volumes.Volume;

public class BlockResetJob implements Runnable {

	private final Volume volume;

	public BlockResetJob(Volume volume) {
		this.volume = volume;
	}
	
	public void run() {
		int visitedBlocks = 0, noOfResetBlocks = 0, x = 0, y = 0, z = 0;
		int currentBlockId = 0;
		int oldBlockType = 0;
		volume.clearBlocksThatDontFloat();
		try {
			if(volume.hasTwoCorners() && volume.getBlockTypes() != null) {
				x = volume.getMinX();
				for(int i = 0; i < volume.getSizeX(); i++){
					y = volume.getMinY();
					for(int j = 0; j < volume.getSizeY(); j++){
						z = volume.getMinZ();
						for(int k = 0;k < volume.getSizeZ(); k++) {
							try {
								oldBlockType = volume.getBlockTypes()[i][j][k];
								byte oldBlockData = volume.getBlockDatas()[i][j][k];
								Block currentBlock = volume.getWorld().getBlockAt(x, y, z);
								currentBlockId = currentBlock.getTypeId();
								if(currentBlockId != oldBlockType ||
									(currentBlockId == oldBlockType && currentBlock.getData() != oldBlockData ) ||
									(currentBlockId == oldBlockType && currentBlock.getData() == oldBlockData &&
											(oldBlockType == Material.WALL_SIGN.getId() || oldBlockType == Material.SIGN_POST.getId() 
													|| oldBlockType == Material.CHEST.getId() || oldBlockType == Material.DISPENSER.getId())
									)
								) {
									if(oldBlockType == Material.WALL_SIGN.getId() 
											|| oldBlockType == Material.SIGN_POST.getId()) {
										// Signs
										currentBlock.setType(Material.getMaterial(oldBlockType));
										currentBlock.setData(oldBlockData);
										BlockState state = currentBlock.getState();
										if(state instanceof Sign) {
											Sign sign = (Sign)state;
											String[] lines = volume.getSignLines().get("sign-" + i + "-" + j + "-" + k);
											if(lines != null && sign.getLines() != null) {
												if(lines.length>0)sign.setLine(0, lines[0]);
												if(lines.length>1)sign.setLine(1, lines[1]);
												if(lines.length>2)sign.setLine(2, lines[2]);
												if(lines.length>3)sign.setLine(3, lines[3]);
												sign.update(true);
											}
										}
									} else if(oldBlockType == Material.CHEST.getId()) {
										// Chests
										currentBlock.setType(Material.getMaterial(oldBlockType));
										currentBlock.setData(oldBlockData);
										BlockState state = currentBlock.getState();
										if(state instanceof Chest) {
											Chest chest = (Chest)state;
											List<ItemStack> contents = volume.getInvBlockContents().get("chest-" + i + "-" + j + "-" + k);
											if(contents != null) {
												int ii = 0;
												chest.getInventory().clear();
												for(ItemStack item : contents) {
													chest.getInventory().setItem(ii, item);
													ii++;
												}
												chest.update(true);
											}
										}
									} else if(oldBlockType == Material.DISPENSER.getId()) {
										// Dispensers		
										currentBlock.setType(Material.getMaterial(oldBlockType));
										currentBlock.setData(oldBlockData);
										BlockState state = currentBlock.getState();
										if(state instanceof Dispenser) {
											Dispenser dispenser = (Dispenser)state;
											List<ItemStack> contents = volume.getInvBlockContents().get("dispenser-" + i + "-" + j + "-" + k);
											if(contents != null) {
												int ii = 0;
												dispenser.getInventory().clear();
												for(ItemStack item : contents) {
													dispenser.getInventory().setItem(ii, item);
													ii++;
												}
												dispenser.update(true);
											}
										}
									} else if(oldBlockType == Material.WOODEN_DOOR.getId() || oldBlockType == Material.IRON_DOOR_BLOCK.getId()){
										// Door blocks
										
										// Check if is bottom door block
										if(j+1 < volume.getSizeY() && volume.getBlockTypes()[i][j+1][k] == oldBlockType) {
											// set both door blocks right away
											currentBlock.setType(Material.getMaterial(oldBlockType));
											currentBlock.setData(oldBlockData);
											Block blockAbove = volume.getWorld().getBlockAt(x, y+1, z);
											blockAbove.setType(Material.getMaterial(oldBlockType));
											blockAbove.setData(volume.getBlockDatas()[i][j+1][k]);
										}
									} else {
										// regular block
										currentBlock.setType(Material.getMaterial(oldBlockType));
										currentBlock.setData(oldBlockData);
									}
									noOfResetBlocks++;
								}
								visitedBlocks++;
							} catch (Exception e) {
								volume.getWar().getLogger().warning("Failed to reset block in volume " + volume.getName() + ". Visited blocks so far:" + visitedBlocks 
										+ ". Blocks reset: "+ noOfResetBlocks + 
										". Error at x:" + x + " y:" + y + " z:" + z + ". Exception:" + e.getClass().toString() + " " + e.getMessage());
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
			volume.getWar().logWarn("Failed to reset volume " + volume.getName() + " blocks. Blocks visited: " + visitedBlocks 
					+ ". Blocks reset: "+ noOfResetBlocks + ". Error at x:" + x + " y:" + y + " z:" + z 
					+ ". Current block: " + currentBlockId + ". Old block: " + oldBlockType + ". Exception: " + e.getClass().toString() + " " + e.getMessage());
			e.printStackTrace();
		}
	}

}
