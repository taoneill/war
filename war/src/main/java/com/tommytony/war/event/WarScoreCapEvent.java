package com.tommytony.war.event;

import java.util.List;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.tommytony.war.Team;

public class WarScoreCapEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	private List<Team> winningTeams;

	public List<Team> getWinningTeams() {
		return winningTeams;
	}

	public WarScoreCapEvent(List<Team> winningTeams) {
		this.winningTeams = winningTeams;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
