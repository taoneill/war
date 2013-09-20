package com.tommytony.war.event;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.tommytony.war.Warzone;

public class WarPlayerDeathEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	private Player victim;
	private Warzone zone;
	private Entity killer;
	private DamageCause cause;

	public WarPlayerDeathEvent(Warzone zone, Player victim, Entity killer,
			DamageCause cause) {
		this.zone = zone;
		this.victim = victim;
		this.killer = killer;
		this.cause = cause;
	}

	public Warzone getZone() {
		return zone;
	}

	public Entity getKiller() {
		return killer;
	}

	public DamageCause getCause() {
		return cause;
	}

	public Player getVictim() {
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
