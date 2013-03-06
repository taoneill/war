package com.tommytony.war.event;

import com.tommytony.war.Team;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.kitteh.tag.PlayerReceiveNameTagEvent;

public class WarTagListener implements Listener {
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onNameTag(PlayerReceiveNameTagEvent event) {
		Team team = Team.getTeamByPlayerName(event.getNamedPlayer().getName());
		if (team != null) {
			ChatColor teamColor = team.getKind().getColor();
			event.setTag(teamColor + event.getNamedPlayer().getName());
		}
	}
}
