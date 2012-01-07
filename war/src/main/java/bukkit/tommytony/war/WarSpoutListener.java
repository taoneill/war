package bukkit.tommytony.war;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.event.spout.SpoutCraftEnableEvent;
import org.getspout.spoutapi.event.spout.SpoutListener;
import org.getspout.spoutapi.gui.Color;
import org.getspout.spoutapi.gui.GenericLabel;
import org.getspout.spoutapi.gui.WidgetAnchor;
import org.getspout.spoutapi.player.SpoutPlayer;

import com.tommytony.war.Team;
import com.tommytony.war.Warzone;
import com.tommytony.war.config.TeamConfig;

public class WarSpoutListener extends SpoutListener {
    static Plugin plugin;
	
	public WarSpoutListener(Plugin plugin) {
		WarSpoutListener.plugin = plugin;
	}

	@Override
	public void onSpoutCraftEnable(SpoutCraftEnableEvent event) {       
		if(!event.getPlayer().isSpoutCraftEnabled()) {
	    	//event.getPlayer().sendMessage("PROTIP: Get Spout at getspout.org for real-time scores display!");
			return;
		}
	}

	public static void updateStats(Warzone zone) {
		if (false) {
			List<GenericLabel> teamlines = new ArrayList<GenericLabel>();
			List<GenericLabel> playerlines = new ArrayList<GenericLabel>();
			List<GenericLabel> scorelines = new ArrayList<GenericLabel>();
			List<GenericLabel> lifelines = new ArrayList<GenericLabel>();
			int teammax = -15, playmax = -15, scoremax = -15;
			GenericLabel line;
	        
			// First, we collect all the team names
	        int linecounter = 0;
			for (Team t : zone.getTeams()) {
				// team name
				line = new GenericLabel(t.getName());
				if (t.getPlayers().size()==0) line.setTextColor(new Color(100,100,100));
				else line.setTextColor(t.getKind().getSpoutColor());
				line.setTooltip("Warzone: "+zone.getName()).setAnchor(WidgetAnchor.TOP_LEFT);
		        line.setAlign(WidgetAnchor.TOP_LEFT).setX(3).setY(3+linecounter*(GenericLabel.getStringHeight("O")+3)).setWidth(GenericLabel.getStringWidth(line.getText())).setHeight(GenericLabel.getStringHeight(line.getText()));
		        teamlines.add(line);
		        linecounter++;
			}
			// We need to find the longest name
			for (GenericLabel l : teamlines) {
				if (GenericLabel.getStringWidth(l.getText()) > teammax) teammax=GenericLabel.getStringWidth(l.getText());
			}
			
			// Now for the players
	        linecounter = 0;
			for (Team t : zone.getTeams()) {
				// player number
				line = new GenericLabel("Players: "+t.getPlayers().size());
				if (t.getPlayers().size()==0) line.setTextColor(new Color(100,100,100));
				line.setTooltip("Warzone: "+zone.getName()).setAnchor(WidgetAnchor.TOP_LEFT);
		        line.setAlign(WidgetAnchor.TOP_LEFT).setX(3+teammax+15).setY(3+linecounter*(GenericLabel.getStringHeight("O")+3)).setWidth(GenericLabel.getStringWidth(line.getText())).setHeight(GenericLabel.getStringHeight(line.getText()));
		        playerlines.add(line);
		        linecounter++;
			}
			// Again, we need the longest entry
			for (GenericLabel l : playerlines) {
				if (GenericLabel.getStringWidth(l.getText()) > playmax) playmax=GenericLabel.getStringWidth(l.getText());
			}
			
			// is there even a score cap (or is it just 1 point)?
			
			linecounter = 0;
			for (Team t : zone.getTeams()) {
				// scores
				line = new GenericLabel(t.getPoints()+"/"+t.getTeamConfig().resolveInt(TeamConfig.MAXSCORE)+" points");
				if (t.getPlayers().size()==0) line.setTextColor(new Color(100,100,100));
				line.setTooltip("Warzone: "+zone.getName()).setAnchor(WidgetAnchor.TOP_LEFT);
		        line.setAlign(WidgetAnchor.TOP_LEFT).setX(3+teammax+15+playmax+15).setY(3+linecounter*(GenericLabel.getStringHeight("O")+3)).setWidth(GenericLabel.getStringWidth(line.getText())).setHeight(GenericLabel.getStringHeight(line.getText()));
		        scorelines.add(line);
		        linecounter++;
			}
			// I bet you know what is done here!
			for (GenericLabel l : scorelines) {
				if (GenericLabel.getStringWidth(l.getText()) > scoremax) scoremax=GenericLabel.getStringWidth(l.getText());
			}
			
			// and finally, lives.
			linecounter = 0;
			for (Team t : zone.getTeams()) {
				line = new GenericLabel(t.getRemainingLifes()+"/"+t.getTeamConfig().resolveInt(TeamConfig.LIFEPOOL)+" lives");
				if (t.getPlayers().size()==0) line.setTextColor(new Color(100,100,100));
				line.setTooltip("Warzone: "+zone.getName()).setAnchor(WidgetAnchor.TOP_LEFT);
		        line.setAlign(WidgetAnchor.TOP_LEFT).setX(3+teammax+15+playmax+15+scoremax+15).setY(3+linecounter*(GenericLabel.getStringHeight("O")+3)).setWidth(GenericLabel.getStringWidth(line.getText())).setHeight(GenericLabel.getStringHeight(line.getText()));
		        scorelines.add(line);
		        linecounter++;
			}
			
			// Now to print it to the Spout players!
			List<GenericLabel> lines = new ArrayList<GenericLabel>();
			for (GenericLabel l : teamlines) lines.add(l);
			for (GenericLabel l : playerlines) lines.add(l);
			for (GenericLabel l : scorelines) lines.add(l);
			for (GenericLabel l : lifelines) lines.add(l);
			for (Team team : zone.getTeams()) {
				for (Player player : team.getPlayers()) {
					SpoutPlayer sp = SpoutManager.getPlayer(player);
					if (sp.isSpoutCraftEnabled()) {
						drawStats(sp, lines);
					}
				}
			}
		}
	}
	
	private static void drawStats(SpoutPlayer sp, List<GenericLabel> lines) {
		// remove old stats first
		removeStats(sp);
		for (GenericLabel l : lines) sp.getMainScreen().attachWidget(plugin, l.copy());
	}
	
	public static void removeStats(SpoutPlayer sp) {
		sp.getMainScreen().removeWidgets(plugin);
	}
}