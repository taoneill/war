package com.tommytony.war.structure;

import org.apache.commons.lang.Validate;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class HubLobbyMaterials implements Cloneable {

	private ItemStack floorBlock;
	private ItemStack outlineBlock;
	private ItemStack gateBlock;
	private ItemStack lightBlock;

	public HubLobbyMaterials(ItemStack floorBlock, ItemStack outlineBlock,
			ItemStack gateBlock, ItemStack lightBlock) {
		Validate.isTrue(floorBlock.getType().isBlock()
				|| floorBlock.getType() == Material.AIR);
		Validate.isTrue(outlineBlock.getType().isBlock()
				|| outlineBlock.getType() == Material.AIR);
		Validate.isTrue(gateBlock.getType().isBlock()
				|| gateBlock.getType() == Material.AIR);
		Validate.isTrue(lightBlock.getType().isBlock()
				|| lightBlock.getType() == Material.AIR);
		this.floorBlock = floorBlock;
		this.outlineBlock = outlineBlock;
		this.gateBlock = gateBlock;
		this.lightBlock = lightBlock;
	}

	public ItemStack getFloorBlock() {
		return floorBlock;
	}

	public void setFloorBlock(ItemStack floorBlock) {
		this.floorBlock = floorBlock;
	}

	public ItemStack getOutlineBlock() {
		return outlineBlock;
	}

	public void setOutlineBlock(ItemStack outlineBlock) {
		this.outlineBlock = outlineBlock;
	}

	public ItemStack getGateBlock() {
		return gateBlock;
	}

	public void setGateBlock(ItemStack gateBlock) {
		this.gateBlock = gateBlock;
	}

	public ItemStack getLightBlock() {
		return lightBlock;
	}

	public void setLightBlock(ItemStack lightBlock) {
		this.lightBlock = lightBlock;
	}

	@Override
	public HubLobbyMaterials clone() {
		try {
			return (HubLobbyMaterials) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new Error(e);
		}
	}
}
