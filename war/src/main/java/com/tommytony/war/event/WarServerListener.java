package com.tommytony.war.event;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.player.SpoutPlayer;

import com.tommytony.war.War;

public class WarServerListener implements Listener {

	@EventHandler
	public void onPluginDisable(final PluginDisableEvent event) {
		if (event.getPlugin().getDataFolder().getName().equals("Spout")) {
			if (War.war.isSpoutServer()) {
				for (Player player : War.war.getServer().getOnlinePlayers()) {
		            SpoutPlayer sp = SpoutManager.getPlayer(player);
		            if (sp.isSpoutCraftEnabled()) {
		                sp.getMainScreen().removeWidgets(War.war);
		            }
		        }
				War.war.getSpoutDisplayer().clearAll();
			}
		}
	}
}
