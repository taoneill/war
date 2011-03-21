package com.tommytony.war.jobs;

import org.bukkit.entity.Player;

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
				tp.teleportTo(zone.getTeleport());	// TODO: change this to a more general rally point (which will enable linking zones together)
				tp.setFireTicks(0);
				tp.setRemainingAir(300);
				if(zone.hasPlayerInventory(tp.getName())){
					zone.restorePlayerInventory(tp);
				}
				if(winnersStr.contains(t.getName())) {
					// give reward
					for(Integer slot : zone.getReward().keySet()){
						tp.getInventory().addItem(zone.getReward().get(slot));
					}
				}
			}
			t.setPoints(0);
			t.getPlayers().clear();	// empty the team
		}
	}
}
