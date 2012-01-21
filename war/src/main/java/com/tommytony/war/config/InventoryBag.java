package com.tommytony.war.config;

import java.util.HashMap;

import org.bukkit.inventory.ItemStack;


import com.tommytony.war.War;
import com.tommytony.war.Warzone;

public class InventoryBag {

	private HashMap<String, HashMap<Integer, ItemStack>> loadouts = new HashMap<String, HashMap<Integer,ItemStack>>();
	private HashMap<Integer, ItemStack> reward = null;
	
	private Warzone warzone;
		
	public InventoryBag(Warzone warzone) {
		this.warzone = warzone;
	}
	
	public InventoryBag() {
		this.warzone = null;
	}

	public void addLoadout(String name, HashMap<Integer, ItemStack> loadout) {
		this.loadouts.put(name, loadout);
	}
	
	public void removeLoadout(String name) {
		this.loadouts.remove(name);
	}
	
	public boolean hasLoadouts() {
		return loadouts.size() > 0;
	}
	
	public HashMap<String, HashMap<Integer, ItemStack>> getLoadouts() {
		return this.loadouts;
	}
	
	public HashMap<String, HashMap<Integer, ItemStack>> resolveLoadouts() {
		if (this.hasLoadouts()) {
			return loadouts;
		} else if (warzone != null && warzone.getDefaultInventories().hasLoadouts()) {
			return warzone.getDefaultInventories().resolveLoadouts();
		} else if (War.war.getDefaultInventories().hasLoadouts()) {
			return War.war.getDefaultInventories().resolveLoadouts();
		} else {
			return new HashMap<String, HashMap<Integer, ItemStack>>();
		}
	}
	
	public void setReward(HashMap<Integer, ItemStack> reward) {
		this.reward = reward;
	}
	
	public boolean hasReward() {
		return reward != null;
	}
	
	public HashMap<Integer, ItemStack> getReward() {
		return reward;
	}
	
	public HashMap<Integer, ItemStack> resolveReward() {
		if (this.hasReward()) {
			return reward;
		} else if (warzone != null && warzone.getDefaultInventories().hasReward()) {
			return warzone.getDefaultInventories().resolveReward();
		} else {
			return War.war.getDefaultInventories().resolveReward();
		}
	}
	
	public void clearLoadouts() {
		this.loadouts.clear();
	}
}
