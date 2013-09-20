package com.tommytony.war.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class WarThiefDeathEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	private String player;
	private int type;
	
	public WarThiefDeathEvent(String player, int type) {
		this.player = player;
		this.type = type;
	}
	
	public String getThiefName() {
		return player;
	}
	
	public int getThiefType() {
		return type;
	}
	
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
	
	public static final int FLAG_THIEF = 0;
	public static final int BOMB_THIEF = 1;
	public static final int CAKE_THIEF = 2;
}
