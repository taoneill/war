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
		for (Team t : this.zone.getTeams()) {
			t.teamcast(this.winnersStr);
			for (Player tp : t.getPlayers()) {
				// Send everyone to rally point (or zone lobby if not rally point)
				if (this.zone.getRallyPoint() != null) {
					tp.teleport(this.zone.getRallyPoint());
				} else {
					tp.teleport(this.zone.getTeleport());
				}
				tp.setFireTicks(0);
				tp.setRemainingAir(300);
				if (this.zone.hasPlayerInventory(tp.getName())) {
					this.zone.restorePlayerInventory(tp);
				}
				if (this.winnersStr.contains(t.getName())) {
					// give reward
					for (Integer slot : this.zone.getReward().keySet()) {
						ItemStack item = this.zone.getReward().get(slot);
						if (item != null) {
							tp.getInventory().addItem(item);
						}
					}
				}
			}
			t.resetPoints();
			t.getPlayers().clear(); // empty the team
		}
	}
}
