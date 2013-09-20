package com.tommytony.war.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class WarPlayerThiefEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	private String player;
	private int type;
	
	public WarPlayerThiefEvent(String thief, int stolen) {
		this.player = thief;
		this.type = stolen;
	}
	
	public String getThief() {
		return player;
	}
	
	public int getStolenObject() {
		return type;
	}
	
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
	
	public static final int STOLE_FLAG = 0;
	public static final int STOLE_BOMB = 1;
	public static final int STOLE_CAKE = 2;
}
