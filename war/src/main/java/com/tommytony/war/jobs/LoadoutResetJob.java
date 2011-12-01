package com.tommytony.war.jobs;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import bukkit.tommytony.war.War;

import com.tommytony.war.Team;
import com.tommytony.war.Warzone;

public class LoadoutResetJob implements Runnable {

	private final Player player;
	private final Warzone zone;
	private final Team team;
	private int loadout = 0;

	public LoadoutResetJob(Warzone zone, Team team, Player player) {
		this.zone = zone;
		this.team = team;
		this.player = player;
	}
	
	public LoadoutResetJob(Warzone zone, Team team, Player player, int loadout) {
		this.zone = zone;
		this.team = team;
		this.player = player;
		this.loadout = loadout;
	}

	public void run() {
		if (loadout==0) this.zone.resetInventory(this.team, this.player);
		else {
			int i = 0;
			Iterator it = this.zone.getExtraLoadouts().entrySet().iterator();
		    while (it.hasNext()) {
		        Map.Entry pairs = (Map.Entry)it.next();
		        if (i == loadout - 1) {
		        	this.zone.resetInventory(this.team, this.player, (HashMap<Integer, ItemStack>)pairs.getValue());
		        }
		        i++;
		    }
		}
		// Stop fire here, since doing it in the same tick as death doesn't extinguish it
		this.player.setFireTicks(0);
	}

}
