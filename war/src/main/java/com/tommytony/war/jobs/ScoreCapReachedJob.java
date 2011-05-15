package com.tommytony.war.jobs;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.tommytony.war.Team;
import com.tommytony.war.Warzone;

public class ScoreCapReachedJob implements Runnable {

	private final Warzone zone;
	private final String winnersStr;

	public ScoreCapReachedJob(Warzone zone, String winnersStr) {
		this.zone = zone;
		this.winnersStr = winnersStr;
	}

	public void run() {
		for(Team t : zone.getTeams()) {
			t.teamcast(winnersStr);
			for(Player tp : t.getPlayers()) {
				// Send everyone to rally point (or zone lobby if not rally point)
				if(zone.getRallyPoint() != null) tp.teleport(zone.getRallyPoint());
				else tp.teleport(zone.getTeleport());
				tp.setFireTicks(0);
				tp.setRemainingAir(300);
				if(zone.hasPlayerInventory(tp.getName())){
					zone.restorePlayerInventory(tp);
				}
				if(winnersStr.contains(t.getName())) {
					// give reward
					for(Integer slot : zone.getReward().keySet()){
						ItemStack item = zone.getReward().get(slot);
						if(item != null) {
							tp.getInventory().addItem(item);
						}
					}
				}
			}
			t.resetPoints();
			t.getPlayers().clear();	// empty the team
		}
	}
}
