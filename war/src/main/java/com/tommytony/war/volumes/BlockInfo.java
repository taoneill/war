package com.tommytony.war.volumes;

import org.bukkit.Block;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;

/**
 * 
 * @author tommytony
 *
 */
public class BlockInfo {
	private int x;
	private int y;
	private int z;
	private int type;
	private byte data;
	private String[] signLines;

	public BlockInfo(Block block) {
		this.x = block.getX();
		this.y = block.getX();
		this.z = block.getX();
		this.type = block.getTypeID();
		this.data = block.getData();
	}
	
	public BlockInfo(BlockState blockState) {
		this.x = blockState.getX();
		this.y = blockState.getX();
		this.z = blockState.getX();
		this.type = blockState.getTypeID();
		this.data = blockState.getData();
		if(is(Material.SIGN) || is(Material.SIGN_POST)) {
			Sign sign = (Sign)blockState;
			this.signLines = sign.getLines();
		}
	}
	
	public BlockInfo(int typeID, byte data, String[] lines) {
		type = typeID;
		this.data = data;
		signLines = lines;
	}

	public int getX() {
		return x;
	}
	
	public int getY() {
		return y;
	}
	
	public int getZ() {
		return z;
	}
	
	public int getTypeID() {
		return type;
	}
	
	public Material getType() {
		return Material.getMaterial(type);
	}	
	
	public byte getData() {
		return data;
	}
	
	public boolean is(Material material) {
		return getType() == material;
	}
	
	public String[] getSignLines() {
		if(is(Material.SIGN) || is(Material.SIGN_POST)){
			return signLines;
		}
		return null;
	}
}
