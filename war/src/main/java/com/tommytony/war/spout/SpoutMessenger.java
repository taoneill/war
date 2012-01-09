package com.tommytony.war.spout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.gui.Color;
import org.getspout.spoutapi.gui.GenericLabel;
import org.getspout.spoutapi.gui.WidgetAnchor;
import org.getspout.spoutapi.player.SpoutPlayer;

import bukkit.tommytony.war.War;

import com.tommytony.war.Team;
import com.tommytony.war.TeamKind;
import com.tommytony.war.Warzone;

public class SpoutMessenger {

	Map<String, List<PlayerMessage>> playerMessages = new HashMap<String, List<PlayerMessage>>();
	
	public void msg(SpoutPlayer sp, String message) {
		if (!playerMessages.containsKey(sp.getName())) {
			playerMessages.put(sp.getName(), new ArrayList<PlayerMessage>());
		}
		playerMessages.get(sp.getName()).add(new PlayerMessage(message));
		drawMessages(sp.getName());
	}
	
	public void fadeOutOldMessages() {
		for (String playerName : playerMessages.keySet()) {
			List<PlayerMessage> messages = playerMessages.get(playerName);
			List<PlayerMessage> toRemove = new ArrayList<PlayerMessage>();
			
			for (PlayerMessage message : messages) {
				if (System.currentTimeMillis() - message.getSendTime() > 12000) {
					
					toRemove.add(message);
				}				
			}
			
			for (PlayerMessage removing : toRemove) {
				messages.remove(removing);
			}
			
			if (toRemove.size() > 0) {
				drawMessages(playerName);
			}
		}
	}
	
	public void remove(String playerName) {
		Player player = War.war.getServer().getPlayer(playerName);
		if (player != null && playerMessages.containsKey(playerName)) {
			clear(SpoutManager.getPlayer(player));
			playerMessages.remove(playerName);			
		}
	}
	
	private void clear(SpoutPlayer player) {
		player.getMainScreen().removeWidgets(War.war);
	}

	public void clearAll() {
		for (String name : playerMessages.keySet()) {
			Player player = War.war.getServer().getPlayer(name);
			if (player != null && playerMessages.containsKey(name)) {
				clear(SpoutManager.getPlayer(player));
			}
			playerMessages.remove(name);
		}
	}
	
	public static String cleanForNotification(String toNotify) {
		if (toNotify.length() > 26) {
			return toNotify.substring(0, 25);
		}
		return toNotify;
	}
	
	private void drawMessages(String playerName) {
		Player bukkitPlayer = War.war.getServer().getPlayer(playerName);
		if (bukkitPlayer != null) {
			SpoutPlayer player = SpoutManager.getPlayer(bukkitPlayer);
			List<PlayerMessage> messages = playerMessages.get(playerName);
				
			// remove old widgets
			clear(player);
			
			if (messages.size() > 0) {
				int rank = 0;			
				Warzone zone = Warzone.getZoneByPlayerName(playerName);			
				int verticalOffset = 2; 
				
				for (PlayerMessage message : messages) {
					int horizontalOffset = 2;
					
					String messageStr = "War> " + message.getMessage();
					String[] words = messageStr.split(" ");
					
					for (String word : words) {
						
						if (horizontalOffset > 230) {	
							horizontalOffset = 2;
							verticalOffset += 8;
						}
	
						word = addMissingColor(word, zone);
						
						GenericLabel label = new GenericLabel(word);
						int width = GenericLabel.getStringWidth(word);
						label.setAlign(WidgetAnchor.TOP_LEFT);
						label.setWidth(width);
						label.setHeight(GenericLabel.getStringHeight(word));
						label.setX(horizontalOffset);
						label.setY(verticalOffset);
						
						player.getMainScreen().attachWidget(War.war, label);
	
						horizontalOffset += width + 2;
					}
					
					verticalOffset += 9;
					
					rank++;
				}
			}		
		}
	}

	public static String addMissingColor(String word, Warzone zone) {
		if (zone != null) {
			for (Team team : zone.getTeams()) {
				for (Player player : team.getPlayers()) {
					if (word.startsWith(player.getName())) {
						return team.getKind().getColor() + word + ChatColor.WHITE;
					}
				}
			}
		}
		
		for (TeamKind kind : TeamKind.values()) {
			if (word.startsWith(kind.toString())) {
				return kind.getColor() + word + ChatColor.WHITE;
			}
		}
		
		if (word.equals("War>")) {
			return ChatColor.GRAY + word + ChatColor.WHITE;
		}
		
		// white by default
		return word;
	}

	private Color getWordColor(String word, Warzone zone) {
		if (zone != null) {
			for (Team team : zone.getTeams()) {
				for (Player player : team.getPlayers()) {
					if (word.startsWith(player.getName())) {
						return team.getKind().getSpoutColor();
					}
				}
			}
		}
		
		for (TeamKind kind : TeamKind.values()) {
			if (word.startsWith(kind.toString())) {
				return kind.getSpoutColor();
			}
		}
		
		if (word.equals("War>")) {
			return new Color(200,200,200);
		}
		
		// white by default
		return new Color(255,255,255);
	}
}
