package com.tommytony.war.structure;

import org.apache.commons.lang.Validate;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class WarzoneMaterials implements Cloneable {
	private ItemStack mainBlock;
	private ItemStack standBlock;
	private ItemStack lightBlock;

	public WarzoneMaterials(ItemStack mainBlock, ItemStack standBlock, ItemStack lightBlock) {
		Validate.isTrue(mainBlock.getType().isBlock() || mainBlock.getType() == Material.AIR);
		Validate.isTrue(standBlock.getType().isBlock() || standBlock.getType() == Material.AIR);
		Validate.isTrue(lightBlock.getType().isBlock() || lightBlock.getType() == Material.AIR);
		this.mainBlock = mainBlock;
		this.standBlock = standBlock;
		this.lightBlock = lightBlock;
	}

	public ItemStack getMainBlock() {
		return mainBlock;
	}

	public void setMainBlock(ItemStack mainBlock) {
		this.mainBlock = mainBlock;
	}

	public ItemStack getStandBlock() {
		return standBlock;
	}

	public void setStandBlock(ItemStack standBlock) {
		this.standBlock = standBlock;
	}

	public ItemStack getLightBlock() {
		return lightBlock;
	}

	public void setLightBlock(ItemStack lightBlock) {
		this.lightBlock = lightBlock;
	}

	@Override
	public WarzoneMaterials clone() {
		try {
			return (WarzoneMaterials) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new Error(e);
		}
	}
}
