package com.tommytony.war.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class WarPlayerLeaveEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	private String player;
	
	public WarPlayerLeaveEvent(String player) {
		this.player = player;
	}
	
	public String getQuitter() {
		return player;
	}
	
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
