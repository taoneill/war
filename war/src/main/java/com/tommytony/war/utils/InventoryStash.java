package com.tommytony.war.utils;

import org.bukkit.inventory.ItemStack;

public class InventoryStash {
	private ItemStack[] contents;
	private ItemStack helmet;
	private ItemStack chest;
	private ItemStack legs;
	private ItemStack feet;

	public InventoryStash(ItemStack[] contents) {
		this.setContents(contents);

	}

	public InventoryStash(ItemStack[] contents, ItemStack helmet, ItemStack chest, ItemStack legs, ItemStack feet) {
		this.setContents(contents);
		this.setHelmet(helmet);
		this.setChest(chest);
		this.setLegs(legs);
		this.setFeet(feet);

	}

	public void setContents(ItemStack[] contents) {
		this.contents = contents;
	}

	public ItemStack[] getContents() {
		return this.contents;
	}

	public void setHelmet(ItemStack helmet) {
		this.helmet = helmet;
	}

	public ItemStack getHelmet() {
		return this.helmet;
	}

	public void setChest(ItemStack chest) {
		this.chest = chest;
	}

	public ItemStack getChest() {
		return this.chest;
	}

	public void setLegs(ItemStack legs) {
		this.legs = legs;
	}

	public ItemStack getLegs() {
		return this.legs;
	}

	public void setFeet(ItemStack feet) {
		this.feet = feet;
	}

	public ItemStack getFeet() {
		return this.feet;
	}


}
