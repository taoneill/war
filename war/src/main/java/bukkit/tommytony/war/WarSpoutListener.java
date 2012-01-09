package bukkit.tommytony.war;

import org.bukkit.plugin.Plugin;
import org.getspout.spoutapi.event.spout.SpoutCraftEnableEvent;
import org.getspout.spoutapi.event.spout.SpoutListener;

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
}