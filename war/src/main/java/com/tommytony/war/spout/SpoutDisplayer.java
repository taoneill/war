package com.tommytony.war.spout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.gui.Color;
import org.getspout.spoutapi.gui.GenericGradient;
import org.getspout.spoutapi.gui.GenericLabel;
import org.getspout.spoutapi.gui.WidgetAnchor;
import org.getspout.spoutapi.player.SpoutPlayer;


import com.tommytony.war.Team;
import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import com.tommytony.war.config.TeamConfig;
import com.tommytony.war.config.TeamKind;

public class SpoutDisplayer {
	
	private static int LINE_HEIGHT = 5;
	private static int LINE_HEIGHT_WITH_MARGIN = 8;
	
	Map<String, List<PlayerMessage>> playerMessages = new HashMap<String, List<PlayerMessage>>();
	
	public void msg(SpoutPlayer sp, String message) {
		if (!playerMessages.containsKey(sp.getName())) {
			playerMessages.put(sp.getName(), new ArrayList<PlayerMessage>());
		}
		List<PlayerMessage> messages = playerMessages.get(sp.getName());
		messages.add(new PlayerMessage(message));
		
		// prevent huge stack of messages, 5 max
		if (messages.size() > 5) {
			// remove first
			messages.remove(0);
		}

		List<Integer> statsOffset = new ArrayList<Integer>(); 
		List<GenericLabel> lines = getStatsLines(Warzone.getZoneByPlayerName(sp.getName()), statsOffset);
		
		drawMessages(sp.getName(), lines, statsOffset);
	}
	 	
	public void fadeOutOldMessages() {
		for (String playerName : playerMessages.keySet()) {
			List<PlayerMessage> messages = playerMessages.get(playerName);
			List<PlayerMessage> toRemove = new ArrayList<PlayerMessage>();
			
			for (PlayerMessage message : messages) {
				if (System.currentTimeMillis() - message.getSendTime() > 15000) {
					
					toRemove.add(message);
				}				
			}
			
			for (PlayerMessage removing : toRemove) {
				messages.remove(removing);
			}
			
			if (toRemove.size() > 0) {
				List<Integer> statsOffset = new ArrayList<Integer>(); 
				List<GenericLabel> lines = getStatsLines(Warzone.getZoneByPlayerName(playerName), statsOffset);
				drawMessages(playerName, lines, statsOffset);
			}
		}
	}

	private void clear(SpoutPlayer player) {
		player.getMainScreen().removeWidgets(War.war);
	}

	public void clearAll() {
		List<String> namesToRemove = new ArrayList<String>();
		for (String name : playerMessages.keySet()) {
			Player player = War.war.getServer().getPlayer(name);
			if (player != null && playerMessages.containsKey(name)) {
				clear(SpoutManager.getPlayer(player));
			}

			namesToRemove.add(name);
		}
		
		for (String toRemove : namesToRemove) {
			playerMessages.remove(toRemove);
		}
	}
	
	public static String cleanForNotification(String toNotify) {
		if (toNotify.length() > 26) {
			return toNotify.substring(0, 25);
		}

		return toNotify;
	}
	
	public void updateStats(Warzone zone) {
		List<Integer> statsOffset = new ArrayList<Integer>(); 
		List<GenericLabel> statsLines = getStatsLines(zone, statsOffset);
		for (Team t : zone.getTeams()) {
			for (Player p : t.getPlayers()) {
				SpoutPlayer sp = SpoutManager.getPlayer(p);
				if (sp.isSpoutCraftEnabled()) {
					drawMessages(sp.getName(), statsLines, statsOffset);
				}
			}
		}
	}
	

	public void updateStats(Player player) {
		SpoutPlayer sp = SpoutManager.getPlayer(player);
		if (sp.isSpoutCraftEnabled()) {
			List<Integer> statsOffset = new ArrayList<Integer>(); 
			Warzone zone = Warzone.getZoneByPlayerName(player.getName());
			List<GenericLabel> statsLines = getStatsLines(zone, statsOffset);
			drawMessages(sp.getName(), statsLines, statsOffset);
		}
	}
	
	private static List<GenericLabel> getStatsLines(Warzone zone, List<Integer> offset) {
		List<GenericLabel> lines = new ArrayList<GenericLabel>();
		offset.add(0);
		offset.add(0);
		
		if (zone != null) {
			offset.clear();

			List<GenericLabel> teamlines = new ArrayList<GenericLabel>();
			List<GenericLabel> playerlines = new ArrayList<GenericLabel>();
			List<GenericLabel> scorelines = new ArrayList<GenericLabel>();
			List<GenericLabel> lifelines = new ArrayList<GenericLabel>();
			int teamMax = 0, scoreMax = 0, lifeMax = 0;
			GenericLabel line;
			
			GenericLabel teamsHeader = new GenericLabel(ChatColor.GRAY + "War> " + ChatColor.WHITE + zone.getName());
			int teamsHeaderWidth = GenericLabel.getStringWidth(teamsHeader.getText()) + 1;
			teamsHeader.setAnchor(WidgetAnchor.TOP_LEFT)
				.setX(3)
				.setY(2)
				.setWidth(teamsHeaderWidth)
				.setHeight(LINE_HEIGHT);
			lines.add(teamsHeader);
	        
			// First, we collect all the team names
	        int lineCounter = 1;
			for (Team t : zone.getTeams()) {
				// team name
				String teamStr = t.getName() + " (" + t.getPlayers().size() + "/" + t.getTeamConfig().resolveInt(TeamConfig.TEAMSIZE) + ")";
				line = new GenericLabel(teamStr);
				if (t.getPlayers().size() == 0) {
					line.setTextColor(new Color(100,100,100));
				}
				else {
					line.setText(t.getKind().getColor() + teamStr.replace("(", ChatColor.GRAY + "(" + ChatColor.WHITE).replace(")", ChatColor.GRAY + ")" + ChatColor.WHITE));
				}
		        line.setAnchor(WidgetAnchor.TOP_LEFT)
		        	.setX(3)
		        	.setY(4 + lineCounter * LINE_HEIGHT_WITH_MARGIN)
		        	.setWidth(GenericLabel.getStringWidth(line.getText()))
		        	.setHeight(LINE_HEIGHT);
		        teamlines.add(line);
		        lineCounter++;
			}
			
			// We need to find the longest name
			for (GenericLabel l : teamlines) {
				if (GenericLabel.getStringWidth(l.getText()) > teamMax) {
					teamMax = GenericLabel.getStringWidth(l.getText());
				}
				if (teamsHeaderWidth > teamMax) {
					teamMax = teamsHeaderWidth;
				}
			}
			
			// points header
			GenericLabel pointsHeader = new GenericLabel(ChatColor.GRAY + "score");
			int pointsHeaderWidth = GenericLabel.getStringWidth(pointsHeader.getText());
			pointsHeader.setAnchor(WidgetAnchor.TOP_LEFT)
				.setX(3 + teamMax + 2)
				.setY(2)
				.setWidth(pointsHeaderWidth)
				.setHeight(LINE_HEIGHT);
			lines.add(pointsHeader);
			
			
			lineCounter = 1;
			for (Team t : zone.getTeams()) {
				// scores
				line = new GenericLabel(t.getPoints() + "/" + t.getTeamConfig().resolveInt(TeamConfig.MAXSCORE));
				if (t.getPlayers().size() == 0) line.setTextColor(new Color(100, 100, 100));
		        line.setAnchor(WidgetAnchor.TOP_LEFT)
		        	.setX(3 + teamMax + 4)
		        	.setY(4 + lineCounter * LINE_HEIGHT_WITH_MARGIN)
		        	.setWidth(GenericLabel.getStringWidth(line.getText()))
		        	.setHeight(LINE_HEIGHT);
		        scorelines.add(line);
		        lineCounter++;
			}
			
			for (GenericLabel l : scorelines) {
				if (GenericLabel.getStringWidth(l.getText()) > scoreMax) {
					scoreMax = GenericLabel.getStringWidth(l.getText());
				}
			}
			if (pointsHeaderWidth > scoreMax) {
				scoreMax = pointsHeaderWidth;
			}
			
			// lifepool header
			GenericLabel livesHeader = new GenericLabel(ChatColor.GRAY + "lives");
			int livesHeaderWidth = GenericLabel.getStringWidth(livesHeader.getText());
			livesHeader.setAnchor(WidgetAnchor.TOP_LEFT)
				.setX(3 + teamMax + 4 + scoreMax + 2)
				.setY(2)
				.setWidth(livesHeaderWidth)
				.setHeight(LINE_HEIGHT);
			lines.add(livesHeader);
			
			
			// and finally, lives.
			lineCounter = 1;
			for (Team t : zone.getTeams()) {
				line = new GenericLabel(t.getRemainingLives() + "/" + t.getTeamConfig().resolveInt(TeamConfig.LIFEPOOL));
				if (t.getPlayers().size() == 0) line.setTextColor(new Color(100, 100, 100));
		        line.setAnchor(WidgetAnchor.TOP_LEFT)
		        	.setX(3 + teamMax + 4 + scoreMax + 4)
		        	.setY(4 + lineCounter * LINE_HEIGHT_WITH_MARGIN)
		        	.setWidth(GenericLabel.getStringWidth(line.getText()))
		        	.setHeight(LINE_HEIGHT);
		        lifelines.add(line);
		        lineCounter++;
			}
			
			for (GenericLabel l : lifelines) {
				if (GenericLabel.getStringWidth(l.getText()) > lifeMax) {
					lifeMax = GenericLabel.getStringWidth(l.getText());
				}
			}
			if (livesHeaderWidth > lifeMax) {
				lifeMax = livesHeaderWidth;
			}
					
			for (GenericLabel l : teamlines) { lines.add(l); }
			for (GenericLabel l : playerlines) { lines.add(l); }
			for (GenericLabel l : scorelines) { lines.add(l); }
			for (GenericLabel l : lifelines) { lines.add(l); }
			
			offset.add(3 + teamMax + 1 + scoreMax + 1 + lifeMax + 5);
			offset.add(4 + lineCounter * LINE_HEIGHT_WITH_MARGIN);
			
		}
		return lines;
		
	}
	
	private static void drawStats(SpoutPlayer sp, List<GenericLabel> lines) {
		for (GenericLabel l : lines) {
			sp.getMainScreen().attachWidget(War.war, l.copy());
		}
	}
	
	private void drawMessages(String playerName, List<GenericLabel> statsLines, List<Integer> statsOffset) {
		Player bukkitPlayer = War.war.getServer().getPlayer(playerName);
		if (bukkitPlayer != null) {
			SpoutPlayer player = SpoutManager.getPlayer(bukkitPlayer);
			List<PlayerMessage> messages = playerMessages.get(playerName);
						
			// remove old widgets
			clear(player);
			
			// add bg
			GenericGradient gradient = new GenericGradient();
			gradient.setAnchor(WidgetAnchor.TOP_LEFT);
			gradient.setTopColor(new Color(0.0F, 0.0F, 0.0F, 0.4F)); // (order is Red, Green, Blue, Alpha)
			gradient.setBottomColor(new Color(0.0F, 0.0F, 0.0F, 0.0F));
			gradient.setHeight(statsOffset.get(1) + 4).setWidth((int)(statsOffset.get(0)));
			
			player.getMainScreen().attachWidget(War.war, gradient);
			
			// border in color of team
			GenericGradient teamGradient = new GenericGradient();
			teamGradient.setAnchor(WidgetAnchor.TOP_LEFT);
			
			Team team = Team.getTeamByPlayerName(playerName);
			
			Color spoutColor = new Color(250.0F, 250.0F, 250.0F, 1.0F);
			if (team != null) {
				spoutColor = team.getKind().getSpoutColor();
			}
			spoutColor.setAlpha(0.5F);
			
			teamGradient.setY(2 + LINE_HEIGHT_WITH_MARGIN);
			teamGradient.setTopColor(spoutColor);
			teamGradient.setBottomColor(new Color(256, 256, 256, 1.0F));
			teamGradient.setHeight(2).setWidth((int)(statsOffset.get(0)));
			
			player.getMainScreen().attachWidget(War.war, teamGradient);
			
			// update stats panel
			drawStats(player, statsLines);
			
			// finally messages
			if (messages != null && messages.size() > 0) {
				Warzone zone = Warzone.getZoneByPlayerName(playerName);			
				int verticalOffset = statsOffset.get(1) + 4; 
				
				for (PlayerMessage message : messages) {
					int horizontalOffset = 2;
					
					String messageStr = ChatColor.GRAY + ">" + ChatColor.WHITE + " " + message.getMessage();
					String[] words = messageStr.split(" ");
					
					for (String word : words) {
						
						if (horizontalOffset > 160) {	
							horizontalOffset = 2;
							verticalOffset += LINE_HEIGHT_WITH_MARGIN;
						}
	
						word = addMissingColor(word, zone);
						
						GenericLabel label = new GenericLabel(word);
						int width = GenericLabel.getStringWidth(word);
						label.setAnchor(WidgetAnchor.TOP_LEFT);
						label.setWidth(width);
						label.setHeight(LINE_HEIGHT);
						label.setX(horizontalOffset);
						label.setY(verticalOffset);
						
						player.getMainScreen().attachWidget(War.war, label);
	
						horizontalOffset += width + 2;
					}
					
					verticalOffset += LINE_HEIGHT_WITH_MARGIN + 1;
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

//	private Color getWordColor(String word, Warzone zone) {
//		if (zone != null) {
//			for (Team team : zone.getTeams()) {
//				for (Player player : team.getPlayers()) {
//					if (word.startsWith(player.getName())) {
//						return team.getKind().getSpoutColor();
//					}
//				}
//			}
//		}
//		
//		for (TeamKind kind : TeamKind.values()) {
//			if (word.startsWith(kind.toString())) {
//				return kind.getSpoutColor();
//			}
//		}
//		
//		if (word.equals("War>")) {
//			return new Color(200,200,200);
//		}
//		
//		// white by default
//		return new Color(255,255,255);
//	}
}
