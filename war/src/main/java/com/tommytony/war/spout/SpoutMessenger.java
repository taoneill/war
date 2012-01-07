package com.tommytony.war.spout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.gui.Color;
import org.getspout.spoutapi.gui.Container;
import org.getspout.spoutapi.gui.ContainerType;
import org.getspout.spoutapi.gui.GenericContainer;
import org.getspout.spoutapi.gui.GenericLabel;
import org.getspout.spoutapi.gui.WidgetAnchor;
import org.getspout.spoutapi.player.SpoutPlayer;

import com.tommytony.war.Team;
import com.tommytony.war.TeamKind;
import com.tommytony.war.Warzone;

import bukkit.tommytony.war.War;

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
				if (System.currentTimeMillis() - message.getSendTime() > 10000) {
					toRemove.add(message);
				}				
			}
			
			for (PlayerMessage removing : toRemove) {
				messages.remove(removing);
			}
			
			drawMessages(playerName);
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
	
	private void drawMessages(String playerName) {
		SpoutPlayer player = SpoutManager.getPlayer(War.war.getServer().getPlayer(playerName));
		List<PlayerMessage> messages = playerMessages.get(playerName);
				
		clear(player);
		
		if (messages.size() > 0) {
//			Container msgListContainer = new GenericContainer();
//			msgListContainer.setAlign(WidgetAnchor.TOP_LEFT);
//			msgListContainer.setLayout(ContainerType.VERTICAL);
			
			int rank = 0;
			int maxLineWidth = 0;
			
			int verticalOffset = 2; 
			for (PlayerMessage message : messages) {
//				Container msgContainer = new GenericContainer();
//				msgContainer.setLayout(ContainerType.VERTICAL);
				
				int horizontalOffset = 2;
				
				String messageStr = "War> " + message.getMessage();
				String[] words = messageStr.split(" ");
				
				int noOfLetters = 0;
				for (String word : words) {
					noOfLetters += word.length() + 1;
					
					if (noOfLetters > 50) {						
						horizontalOffset = 2;
						verticalOffset += 12;
					}
					
					GenericLabel label = new GenericLabel(word);
					label.setTextColor(getWordColor(word, playerName));
					
					int width = GenericLabel.getStringWidth(word);
					label.setAlign(WidgetAnchor.TOP_LEFT);
					label.setWidth(width);
					label.setHeight(GenericLabel.getStringHeight(word));
					label.setX(horizontalOffset);
					label.setY(verticalOffset);
					player.getMainScreen().attachWidget(War.war, label);
					//label.shiftXPos(horizOffset);
					
					//lineContainer.addChild(label);
					//lineWidth += GenericLabel.getStringWidth(word);
					horizontalOffset += width + 2;
				}
				
//				lineContainer.setWidth(lineWidth + 50);
//				lineContainer.setHeight(12);
//				msgContainer.addChild(lineContainer);
//				
//				msgContainer.setWidth(maxLineWidth + 50);
//				msgContainer.setHeight(40);
//				msgListContainer.addChild(msgContainer);
				
				verticalOffset += 12;
				
				rank++;
			}

			// remove old message list
			
			
			// new message list
//			msgListContainer.setWidth(maxLineWidth);
//			msgListContainer.setHeight(200);
//			player.getMainScreen().attachWidget(War.war, msgListContainer);
		}		
	}

	private Color getWordColor(String word, String playerName) {
		Warzone zone = Warzone.getZoneByPlayerName(playerName);
		
		for (Team team : zone.getTeams()) {
			for (Player player : team.getPlayers()) {
				if (word.contains(player.getName())) {
					return team.getKind().getSpoutColor();
				}
			}
		}
		
		for (TeamKind kind : TeamKind.values()) {
			if (word.contains(kind.toString())) {
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
