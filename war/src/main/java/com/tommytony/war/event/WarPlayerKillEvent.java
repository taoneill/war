package com.tommytony.war.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class WarPlayerKillEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	private String attacker, victim;
	
	public WarPlayerKillEvent(String attacker, String victim) {
		this.attacker = attacker;
		this.victim = victim;
	}
	
	public String getAttacker() {
		return attacker;
	}
	
	public String getVictim() {
		return victim;
	}
	
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
