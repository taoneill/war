package com.tommytony.war.utils;

import java.util.List;

import org.bukkit.GameMode;
import org.bukkit.inventory.ItemStack;

import com.tommytony.war.PotionEffect;

public class PlayerState {
	private ItemStack[] contents;
	private ItemStack helmet;
	private ItemStack chest;
	private ItemStack legs;
	private ItemStack feet;
	private final float exhaustion;
	private final float saturation;
	private final int foodLevel;
	private final int health;
	private final GameMode gamemode;
	private final List<PotionEffect> potionEffects;
	private final String playerTitle;

	public PlayerState(GameMode gamemode, ItemStack[] contents, ItemStack helmet, ItemStack chest, ItemStack legs, ItemStack feet, int health, float exhaustion, float saturation, int foodLevel, List<PotionEffect> potionEffects, String playerTitle) {
		this.gamemode = gamemode;
		this.health = health;
		this.exhaustion = exhaustion;
		this.saturation = saturation;
		this.foodLevel = foodLevel;
		this.potionEffects = potionEffects;
		this.playerTitle = playerTitle;
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

	public float getExhaustion() {
		return exhaustion;
	}

	public float getSaturation() {
		return saturation;
	}

	public int getFoodLevel() {
		return foodLevel;
	}

	public int getHealth() {
		return health;
	}

	public GameMode getGamemode() {
		return gamemode;
	}

	public List<PotionEffect> getPotionEffects() {
		return potionEffects;
	}

	public String getPlayerTitle() {
		return playerTitle;
	}

}
