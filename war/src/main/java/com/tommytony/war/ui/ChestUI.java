package com.tommytony.war.ui;

import com.tommytony.war.War;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Connor on 7/25/2017.
 */
public abstract class ChestUI {
	private Map<ItemStack, Runnable> actions;
	ChestUI() {
		actions = new HashMap<ItemStack, Runnable>();
	}

	protected void addItem(Inventory inv, int slot, ItemStack item, Runnable action) {
		inv.setItem(slot, item);
		actions.put(item, action);
	}

	public abstract void build(Player player, Inventory inv);

	public abstract String getTitle();

	public abstract int getSize();

	boolean processClick(ItemStack clicked, Inventory inventory) {
		if (actions.containsKey(clicked)) {
			War.war.getServer().getScheduler().runTask(War.war, actions.get(clicked));
			return true;
		}
		return false;
	}
}
