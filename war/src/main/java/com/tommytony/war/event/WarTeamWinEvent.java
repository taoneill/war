package com.tommytony.war.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class WarTeamWinEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	private String[] members;
	
	public WarTeamWinEvent(String[] members) {
		this.members = members;
	}
	
	public String[] getWinners() {
		return members;
	}
	
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
