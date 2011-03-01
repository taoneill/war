package com.tommytony.war.volumes;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Sign;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class BlockSaveJob extends Thread {
	private final Volume volume;

	public BlockSaveJob(Volume volume) {
		this.volume = volume;
	}
	public void run() {
		int noOfSavedBlocks = 0;
		int x = 0;
		int y = 0;
		int z = 0;
		try {
			if(volume.hasTwoCorners()) {
				volume.setBlockTypes(new int[volume.getSizeX()][volume.getSizeY()][volume.getSizeZ()]);
				volume.setBlockDatas(new byte[volume.getSizeX()][volume.getSizeY()][volume.getSizeZ()]);
				volume.getSignLines().clear();
				volume.getInvBlockContents().clear();
				x = volume.getMinX();
				for(int i = 0; i < volume.getSizeX(); i++){
					y = volume.getMinY();
					for(int j = 0; j < volume.getSizeY(); j++){
						z = volume.getMinZ();
						for(int k = 0;k < volume.getSizeZ(); k++) {
							try {
								Block block = volume.getWorld().getBlockAt(x, y, z);
								volume.getBlockTypes()[i][j][k] = block.getTypeId();
								volume.getBlockDatas()[i][j][k] = block.getData();
								BlockState state = block.getState();
								if(state instanceof Sign) {
									// Signs
									Sign sign = (Sign)state;
									if(sign.getLines() != null) {
										volume.getSignLines().put("sign-" + i + "-" + j + "-" + k, sign.getLines());
									}
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
									volume.getInvBlockContents().put("chest-" + i + "-" + j + "-" + k, items);
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
									volume.getInvBlockContents().put("dispenser-" + i + "-" + j + "-" + k, items);
								}
								
								noOfSavedBlocks++;
							} catch (Exception e) {
								volume.getWar().getLogger().warning("Failed to save block in volume " + getName() + ". Saved blocks so far:" + noOfSavedBlocks 
										+ ". Error at x:" + x + " y:" + y + " z:" + z + ". Exception:" + e.getClass().toString() + e.getMessage());
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
			volume.getWar().getLogger().warning("Failed to save volume " + getName() + " blocks. Saved blocks:" + noOfSavedBlocks 
					+ ". Error at x:" + x + " y:" + y + " z:" + z + ". Exception:" + e.getClass().toString() + " "+ e.getMessage());
			e.printStackTrace();
		}
		//return noOfSavedBlocks;
	}
}
