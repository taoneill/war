package com.tommytony.war.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class WarPlayerThiefEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	private Player player;
	private StolenObject type;
	
	public WarPlayerThiefEvent(Player thief, StolenObject stolen) {
		this.player = thief;
		this.type = stolen;
	}
	
	public Player getThief() {
		return player;
	}
	
	public StolenObject getStolenObject() {
		return type;
	}
	
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
	
	public enum StolenObject {
		FLAG,
		BOMB,
		CAKE
	}
}
