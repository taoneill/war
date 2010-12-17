

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;



public class War extends Plugin {
	
	private WarListener listener = new WarListener(this);
    private Logger log;
    String name = "War";
    String version = "0.1";
    
    private final List<Team> teams = new ArrayList<Team>();

	public void initialize() {
		this.log = Logger.getLogger("Minecraft");
		getLogger().info(name + " " + version + " initialized");
		
		etc.getLoader().addListener(
                PluginLoader.Hook.COMMAND,
                listener,
                this,
                PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener( PluginLoader.Hook.LOGIN,  
				listener,
				this,
				PluginListener.Priority.MEDIUM);
//        etc.getLoader().addListener(
//                PluginLoader.Hook.BLOCK_CREATED,
//                listener,
//                this,
//                PluginListener.Priority.MEDIUM);
	}
	
	@Override
	public void disable() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void enable() {
		// TODO Auto-generated method stub
		
	}

	public List<Team> getTeams() {
		return teams;
	}
	
	public Team getPlayerTeam(String playerName) {
		for(Team team : teams) {
			for(Player player : team.getPlayers()) {
				if(player.getName().equals(playerName)) {
					return team;
				}
			}
		}
		return null;
	}

	public Logger getLogger() {
		return log;
	}

}
