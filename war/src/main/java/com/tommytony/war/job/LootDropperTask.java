package com.tommytony.war.job;

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
		for (ItemStack item : this.drop) {
			if (item != null) {
				this.location.getWorld().dropItemNaturally(this.location, item);
			}
		}
	}
}
