package com.tommytony.war.volumes;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Scanner;

import javax.naming.BinaryRefAddr;

import org.bukkit.Block;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;

import bukkit.tommytony.war.War;

import com.tommytony.war.Warzone;

/**
 * 
 * @author tao
 *
 */
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
			this.getWar().getLogger().warning(getWar().str("Failed to save volume " + getName() + " blocks. " + e.getMessage()));
		}
		return noOfSavedBlocks;
	}
	
	public int resetBlocks() {
		int noOfResetBlocks = 0;
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
							if(currentBlock.getTypeID() != oldBlockInfo.getTypeID() ||
								(currentBlock.getTypeID() == oldBlockInfo.getTypeID() && currentBlock.getData() != oldBlockInfo.getData()) ||
								(currentBlock.getTypeID() == oldBlockInfo.getTypeID() && currentBlock.getData() == oldBlockInfo.getData() &&
										(oldBlockInfo.is(Material.Sign) || oldBlockInfo.is(Material.SignPost))
								)
							) {
								currentBlock.setType(oldBlockInfo.getType());
								currentBlock.setData(oldBlockInfo.getData());
								if(oldBlockInfo.is(Material.Sign) || oldBlockInfo.is(Material.SignPost)) {
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
			this.getWar().getLogger().warning(getWar().str("Failed to reset volume " + getName() + " blocks. " + e.getClass().toString() + " " + e.getMessage()));
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
	
//	public String blocksToString() {
//		if(hasTwoCorners() && blockInfos != null) {
//			StringBuilder volumeStringBuilder = new StringBuilder();
//			volumeStringBuilder.append(cornerOne.getX() + "," + cornerOne.getY() + "," + cornerOne.getZ() + ";");
//			volumeStringBuilder.append(cornerTwo.getX() + "," + cornerTwo.getY() + "," + cornerTwo.getZ() + ";");
//			
//			for(int i = 0; i < getSizeX(); i++){
//				for(int j = 0; j < getSizeY(); j++) {
//					for(int k = 0; k < getSizeZ(); k++) {
//						BlockInfo info = getBlockInfos()[i][j][k];
//						if(info == null) {
//							volumeStringBuilder.append("0,0,");
//						} else {
//							volumeStringBuilder.append(info.getTypeID() + "," + info.getData() + ",");
//							if(info.getType() == Material.Sign || info.getType() == Material.SignPost) {
//								String[] lines = info.getSignLines();
//								volumeStringBuilder.append(lines[0] + "\n");
//								volumeStringBuilder.append(lines[1] + "\n");
//								volumeStringBuilder.append(lines[2] + "\n");
//								volumeStringBuilder.append(lines[3] + "\n");
//							}
//						}
//						
//						volumeStringBuilder.append(";");
//					}
//				}
//			}
//			return volumeStringBuilder.toString();
//		}
//		return "";
//	}
	
//	public void blocksFromString(String volumeString) {
//		Scanner scanner = new Scanner(volumeString);
//		int x1 = 0;
//		if(scanner.hasNext(".+,")) x1 = Integer.parseInt(scanner.next(".+,").replace(",", ""));
//		int y1 = 0;
//		if(scanner.hasNext(".+,")) y1 = Integer.parseInt(scanner.next(".+,").replace(",", ""));
//		int z1 = 0;
//		if(scanner.hasNext(".+,")) z1 = Integer.parseInt(scanner.next(".+,").replace(",", ""));
//		if(scanner.hasNext(";")) scanner.next(";");
//		cornerOne = getWorld().getBlockAt(x1, y1, z1);
//		
//		int x2 = 0;
//		if(scanner.hasNext(".+,")) x2 = Integer.parseInt(scanner.next(".+,").replace(",", ""));
//		int y2 = 0;
//		if(scanner.hasNext(".+,")) y2 = Integer.parseInt(scanner.next(".+,").replace(",", ""));
//		int z2 = 0;
//		if(scanner.hasNext(".+,")) z2 = Integer.parseInt(scanner.next(".+,").replace(",", ""));
//		if(scanner.hasNext(";")) scanner.next(";");
//		cornerTwo = getWorld().getBlockAt(x2, y2, z2);
//		
//		setBlockInfos(new BlockInfo[getSizeX()][getSizeY()][getSizeZ()]);
//		for(int i = 0; i < getSizeX(); i++){
//			for(int j = 0; j < getSizeY(); j++) {
//				for(int k = 0; k < getSizeZ(); k++) {
//					int typeID = 0;
//					if(scanner.hasNext(".+,")) typeID = Integer.parseInt(scanner.next(".+,").replace(",", ""));
//					byte data = 0;
//					if(scanner.hasNext(".+,")) data = Byte.parseByte(scanner.next(".+,").replace(",", ""));
//					String[] lines = null;
//					if(typeID == Material.Sign.getID() || typeID == Material.SignPost.getID()) {
//						lines = new String[4];
//						if(scanner.hasNextLine()) lines[0] = scanner.nextLine();
//						if(scanner.hasNextLine()) lines[1] = scanner.nextLine();
//						if(scanner.hasNextLine()) lines[2] = scanner.nextLine();
//						if(scanner.hasNextLine()) lines[3] = scanner.nextLine();
//					}
//					if(scanner.hasNext(";")) scanner.next(";");
//					getBlockInfos()[i][j][k] = new BlockInfo(typeID, data, lines);
//				}
//			}
//		}
//	}
	
	public void fromDisk() throws IOException {
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(new File(war.getName() + "/warzone-" + warzone.getName() + "/" + name)));
			;
			String firstLine = in.readLine();
			if(firstLine != null && !firstLine.equals("")) {
				int x1 = Integer.parseInt(in.readLine());
				int y1 = Integer.parseInt(in.readLine());
				int z1 = Integer.parseInt(in.readLine());
				int x2 = Integer.parseInt(in.readLine());
				int y2 = Integer.parseInt(in.readLine());
				int z2 = Integer.parseInt(in.readLine());
				cornerOne = getWorld().getBlockAt(x1, y1, z1);
				cornerTwo = getWorld().getBlockAt(x2, y2, z2);
				
				setBlockInfos(new BlockInfo[getSizeX()][getSizeY()][getSizeZ()]);
				for(int i = 0; i < getSizeX(); i++){
					for(int j = 0; j < getSizeY(); j++) {
						for(int k = 0; k < getSizeZ(); k++) {
							String blockLine = in.readLine();
							String[] blockSplit = blockLine.split(",");
							
							int typeID = Integer.parseInt(blockSplit[0]);
							byte data = Byte.parseByte(blockSplit[1]);
							String[] lines = null;
							if(typeID == Material.Sign.getID() || typeID == Material.SignPost.getID()) {
								String signLines = blockSplit[2];
								if(blockSplit.length > 3) {
									// sign includes commas
									for(int splitI = 3; splitI < blockSplit.length; splitI++) {
										signLines.concat(blockSplit[splitI]);
									}
								}
								String[] signLinesSplit = signLines.split("[line]");
								lines = new String[4];
								lines[0] = signLinesSplit[0];
								lines[1] = signLinesSplit[1];
								lines[2] = signLinesSplit[2];
								lines[3] = signLinesSplit[3];
							}
							getBlockInfos()[i][j][k] = new BlockInfo(typeID, data, lines);
						}
					}
				}
			}
		} finally {
			if(in != null) in.close();
		}
	}
	
	public void toDisk() throws IOException {
		if(isSaved() && getBlockInfos() != null) {
			BufferedWriter out = null;
			try {
				out = new BufferedWriter(new FileWriter(new File(war.getName() + "/warzone-" + warzone.getName() + "/" + name)));
				out.write("corner1"); out.newLine();
				out.write(cornerOne.getX()); out.newLine();
				out.write(cornerOne.getY()); out.newLine();
				out.write(cornerOne.getZ()); out.newLine();
				out.write("corner2"); out.newLine();
				out.write(cornerTwo.getX()); out.newLine();
				out.write(cornerTwo.getY()); out.newLine();
				out.write(cornerTwo.getZ()); out.newLine();
				
				for(int i = 0; i < getSizeX(); i++){
					for(int j = 0; j < getSizeY(); j++) {
						for(int k = 0; k < getSizeZ(); k++) {
							BlockInfo info = getBlockInfos()[i][j][k];
							if(info == null) {
								out.write("0,0,"); out.newLine();
							} else {
								if(info.getType() == Material.Sign || info.getType() == Material.SignPost) {
									String[] lines = info.getSignLines();
									out.write(info.getTypeID() + "," + info.getData() + "," + lines[0] + "[line]" + lines[1] + "[line]" + lines[2] + "[line]"+ lines[3]);
									
								} else {
									out.write(info.getTypeID() + "," + info.getData() + ","); 
								}
							}
						}
					}
				}
			} finally {
				if(out != null) out.close();	
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

}
