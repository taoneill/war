package com.tommytony.war.jobs;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

public class LootDropperTask implements Runnable {

	private final List<ItemStack> drop;
	private final Location location;

	public LootDropperTask(Location location, List<ItemStack> drop) {
		this.location = location;
		this.drop = drop;
	}
	
	public void run() {
		for(ItemStack item : drop) {
			if(item != null) {
				location.getWorld().dropItemNaturally(location, item);
			}
		}
	}
}
