package com.tommytony.war.volumes;

import java.util.Scanner;

import org.bukkit.Block;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Sign;

import com.tommytony.war.War;
import com.tommytony.war.Warzone;

public class Volume {
	private final String name;
	private final World world;
	private final Warzone warzone;
	private Block cornerOne;
	private Block cornerTwo;
	private BlockInfo[][][] blockInfos = null;
	private final War war;	

	public Volume(String name, War war, Warzone warzone) {
		this.name = name;
		this.war = war;
		this.warzone = warzone;
		this.world = warzone.getWorld();
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
				this.blockInfos = new BlockInfo[getSizeX()][getSizeY()][getSizeZ()];
				int x = getMinX();
				for(int i = 0; i < getSizeX(); i++){
					int y = getMinY();
					for(int j = 0; j < getSizeY(); j++){
						int z = getMinZ();
						for(int k = 0;k < getSizeZ(); k++) {
							this.blockInfos[i][j][k] = new BlockInfo(world.getBlockAt(x, y, z));
							z++;
							noOfSavedBlocks++;
						}
						y++;
					}
					x++;
				}
			}		
		} catch (Exception e) {
			this.war.getLogger().warning(war.str("Failed to save volume " + name + " blocks. " + e.getMessage()));
		}
		return noOfSavedBlocks;
	}
	
	public int resetBlocks() {
		int noOfResetBlocks = 0;
		try {
			if(hasTwoCorners() && blockInfos != null) {
				int x = getMinX();
				for(int i = 0; i < getSizeX(); i++){
					int y = getMinY();
					for(int j = 0; j < getSizeY(); j++){
						int z = getMinZ();
						for(int k = 0;k < getSizeZ(); k++) {
							BlockInfo oldBlockInfo = blockInfos[i][j][k];
							Block currentBlock = world.getBlockAt(x, y, z);
							if(currentBlock.getTypeID() != oldBlockInfo.getTypeID() ||
								(currentBlock.getTypeID() == oldBlockInfo.getTypeID() && currentBlock.getData() != oldBlockInfo.getData()) ||
								(currentBlock.getTypeID() == oldBlockInfo.getTypeID() && currentBlock.getData() == oldBlockInfo.getData() &&
										(oldBlockInfo.is(Material.Sign) || oldBlockInfo.is(Material.SignPost))
								)
							) {
								currentBlock.setType(oldBlockInfo.getType());
								currentBlock.setData(oldBlockInfo.getData());
								if(oldBlockInfo.is(Material.Sign) || oldBlockInfo.is(Material.SignPost)) {
									Sign currentSign = (Sign) currentBlock;
									currentSign.setLine(0, oldBlockInfo.getSignLines()[0]);
									currentSign.setLine(1, oldBlockInfo.getSignLines()[0]);
									currentSign.setLine(2, oldBlockInfo.getSignLines()[0]);
									currentSign.setLine(3, oldBlockInfo.getSignLines()[0]);
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
			this.war.getLogger().warning(war.str("Failed to reset volume " + name + " blocks. " + e.getMessage()));
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
		return getMaxX() - getMinX();
	}
	
	public int getSizeY() {
		return getMaxY() - getMinY();
	}
	
	public int getSizeZ() {
		return getMaxZ() - getMinZ();
	}	

	public boolean isSaved() {
		return blockInfos != null;
	}

	public BlockInfo[][][] getBlockInfos() {
		return blockInfos;
	}
	
	public String blocksToString() {
		StringBuilder volumeStringBuilder = new StringBuilder();
		volumeStringBuilder.append(cornerOne.getX() + "," + cornerOne.getY() + "," + cornerOne.getZ() + ";");
		volumeStringBuilder.append(cornerTwo.getX() + "," + cornerTwo.getY() + "," + cornerTwo.getZ() + ";");
		
		for(int i = 0; i < getSizeX(); i++){
			for(int j = 0; j < getSizeY(); j++) {
				for(int k = 0; k < getSizeZ(); k++) {
					BlockInfo info = getBlockInfos()[i][j][k]; 
					volumeStringBuilder.append(info.getTypeID() + "," + info.getData() + "," + info.getSignLines()[0]);
					if(info.getType() == Material.Sign || info.getType() == Material.SignPost) {
						String[] lines = info.getSignLines();
						volumeStringBuilder.append(lines[0] + "\n");
						volumeStringBuilder.append(lines[1] + "\n");
						volumeStringBuilder.append(lines[2] + "\n");
						volumeStringBuilder.append(lines[3] + "\n");
					}
					volumeStringBuilder.append(";");
				}
			}
		}
		return volumeStringBuilder.toString();
	}
	
	public void blocksFromString(String volumeString) {
		Scanner scanner = new Scanner(volumeString);
		int x1 = scanner.nextInt();
		scanner.next(",");
		int y1 = scanner.nextInt();
		scanner.next(",");
		int z1 = scanner.nextInt();
		scanner.next(";");
		cornerOne = world.getBlockAt(x1, y1, z1);
		int x2 = scanner.nextInt();
		scanner.next(",");
		int y2 = scanner.nextInt();
		scanner.next(",");
		int z2 = scanner.nextInt();
		scanner.next(";");
		cornerOne = world.getBlockAt(x2, y2, z2);
		
		blockInfos = new BlockInfo[getSizeX()][getSizeY()][getSizeZ()];
		for(int i = 0; i < getSizeX(); i++){
			for(int j = 0; j < getSizeY(); j++) {
				for(int k = 0; k < getSizeZ(); k++) {
					// scan ne
					int typeID = scanner.nextInt();
					scanner.next(",");
					byte data = scanner.nextByte();
					String[] lines = null;
					if(typeID == Material.Sign.getID() || typeID == Material.SignPost.getID()) {
						scanner.next(",");
						lines = new String[4];
						lines[0] = scanner.nextLine();
						lines[1] = scanner.nextLine();
						lines[2] = scanner.nextLine();
						lines[3] = scanner.nextLine();
					}
					scanner.next(";");
					getBlockInfos()[i][j][k] = new BlockInfo(typeID, data, lines);
				}
			}
		}
	}

	public Warzone getWarzone() {
		return warzone;
	}

	public boolean contains(Location location) {
		int x = location.getBlockX();
		int y = location.getBlockY();
		int z = location.getBlockZ();
		return x <= getMaxX() && x >= getMinX() && 
				y <= getMaxY() && y >= getMinY() &&
				z <= getMaxZ() && z >= getMinZ();
	}

}
