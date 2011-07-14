package com.tommytony.war.jobs;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;

import com.tommytony.war.utils.DeferredBlockReset;

public class DeferredBlockResetsJob implements Runnable {

	List<DeferredBlockReset> deferred = new ArrayList<DeferredBlockReset>();
	private final World world;

	public DeferredBlockResetsJob(World world) {
		this.world = world;

	}

	public void add(DeferredBlockReset pleaseResetLater) {
		deferred.add(pleaseResetLater);
	}

	public boolean isEmpty() {
		return deferred.isEmpty();
	}


	public void run() {
		for(DeferredBlockReset reset : deferred) {
			Block worldBlock = world.getBlockAt(reset.getX(), reset.getY(), reset.getZ());
			worldBlock.setType(Material.getMaterial(reset.getBlockType()));

			if(reset.getBlockType() == Material.SIGN_POST.getId()) {
				BlockState state = worldBlock.getState();
				state.setData(new org.bukkit.material.Sign(reset.getBlockType(), reset.getBlockData()));
				if(state instanceof Sign) {
					Sign sign = (Sign)state;
					//String[] lines = this.getSignLines().get("sign-" + i + "-" + j + "-" + k);
					if(reset.getLines() != null && sign.getLines() != null) {
						if(reset.getLines().length>0)sign.setLine(0, reset.getLines()[0]);
						if(reset.getLines().length>1)sign.setLine(1, reset.getLines()[1]);
						if(reset.getLines().length>2)sign.setLine(2, reset.getLines()[2]);
						if(reset.getLines().length>3)sign.setLine(3, reset.getLines()[3]);
						sign.update(true);
					}
				}
			} else {
				// normal data reset
				worldBlock.setData(reset.getBlockData());
			}




		}
	}

}
