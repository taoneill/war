package com.tommytony.war.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class WarPlayerDeathEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	private String victim;
	
	public WarPlayerDeathEvent(String victim) {
		this.victim = victim;
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
