package com.tommytony.war.job;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;

import com.tommytony.war.utility.DeferredBlockReset;

public class DeferredBlockResetsJob implements Runnable {

	List<BlockState> deferred = new ArrayList<BlockState>();

	public DeferredBlockResetsJob() {
	}

	@Deprecated
	public DeferredBlockResetsJob(World humor) {
	}

	public void add(BlockState pleaseResetLater) {
		this.deferred.add(pleaseResetLater);
	}

	@Deprecated
	public void add(DeferredBlockReset humor) {
	}

	public boolean isEmpty() {
		return this.deferred.isEmpty();
	}

	public void run() {
		for (BlockState reset : this.deferred) {
			reset.update(true, false);
			for (Entity ent : reset.getWorld().getEntities()) {
				if (ent instanceof Item
						&& ent.getLocation().distance(reset.getLocation()) < 2) {
					ent.remove();
				}
			}
		}
	}
}
