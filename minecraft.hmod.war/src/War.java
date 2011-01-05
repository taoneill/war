

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;



public class War extends Plugin {
	
	public static final int LIFEPOOL = 5;
	private WarListener listener = new WarListener(this);
    private Logger log;
    String name = "War";
    String version = "0.1";
    
    private final List<Warzone> warzones = new ArrayList<Warzone>();
    private final List<Item> defaultLoadout = new ArrayList<Item>();
    private int defaultLifepool = 7;
    private boolean defaultFriendlyFire = false;    

	public void initialize() {
		this.log = Logger.getLogger("Minecraft");
		
		
		// Register hMod hooks
		
		etc.getLoader().addListener(
                PluginLoader.Hook.COMMAND,
                listener,
                this,
                PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener( PluginLoader.Hook.LOGIN,  
				listener,
				this,
				PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener( PluginLoader.Hook.HEALTH_CHANGE,  
				listener,
				this,
				PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener( PluginLoader.Hook.DAMAGE,  
				listener,
				this,
				PluginListener.Priority.HIGH);
		etc.getLoader().addListener( PluginLoader.Hook.PLAYER_MOVE,  
				listener,
				this,
				PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener( PluginLoader.Hook.DISCONNECT,  
				listener,
				this,
				PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener( PluginLoader.Hook.IGNITE,  
				listener,
				this,
				PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener( PluginLoader.Hook.FLOW,  
				listener,
				this,
				PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener( PluginLoader.Hook.BLOCK_BROKEN,  
				listener,
				this,
				PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener( PluginLoader.Hook.BLOCK_PLACE,  
				listener,
				this,
				PluginListener.Priority.MEDIUM);
		
		// Load files from disk or create them
		this.defaultLoadout.add(new Item(272, 1, 0));
		this.defaultLoadout.add(new Item(261, 1, 1));
		this.defaultLoadout.add(new Item(262, 12, 2));
		this.defaultLoadout.add(new Item(274, 1, 3));
		this.defaultLoadout.add(new Item(273, 1, 4));
		this.defaultLoadout.add(new Item(275, 1, 5));
		this.defaultLoadout.add(new Item(259, 1, 27));
		this.defaultLoadout.add(new Item(297, 1, 6));
		this.defaultLoadout.add(new Item(3, 12, 8));
		this.defaultLoadout.add(new Item(301, 1, 100));
		this.defaultLoadout.add(new Item(300, 1, 101));
		this.defaultLoadout.add(new Item(299, 1, 102));
		this.defaultLoadout.add(new Item(298, 1, 103));
		this.defaultLifepool = 7;
		this.defaultFriendlyFire = false;
		WarMapper.load(this);
		
		getLogger().info(name + " " + version + " initialized.");
	}
	
	@Override
	public void disable() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void enable() {
		// TODO Auto-generated method stub
		
	}

	public Team getPlayerTeam(String playerName) {
		for(Warzone warzone : warzones) {
			Team team = warzone.getPlayerTeam(playerName);
			if(team != null) return team;
		}
		return null;
	}
	
	public Warzone getPlayerWarzone(String playerName) {
		for(Warzone warzone : warzones) {
			Team team = warzone.getPlayerTeam(playerName);
			if(team != null) return warzone;
		}
		return null;
	}	

	public Logger getLogger() {
		return log;
	}
	
	public Warzone warzone(Location location) {
		for(Warzone warzone : warzones) {
			if(warzone.contains(location)) return warzone;
		}
		return null;
	}

	public boolean inAnyWarzone(Location location) {
		if(warzone(location) == null) {
			return false;
		}
		return true;
	}
	
	public boolean inWarzone(String warzoneName, Location location) {
		Warzone currentZone = warzone(location);
		if(currentZone == null) {
			return false;
		} else if (warzoneName.equals(currentZone.getName())){
			return true;
		}
		return false;
	}

	public void addWarzone(Warzone zone) {
		warzones.add(zone);
	}

	public Server getServer() {
		// TODO Auto-generated method stub
		return etc.getServer();
	}

	public List<Warzone> getWarzones() {
		return warzones;
	}
	
	public String str(String str) {
		String out = Colors.LightGray + "[war] " + Colors.White + str;
		return out;
	}
	
	public Warzone findWarzone(String warzoneName) {
		for(Warzone warzone : warzones) {
			if(warzone.getName().equals(warzoneName)) {
				return warzone;
			}
		}
		return null;
	}

	public List<Item> getDefaultLoadout() {
		return defaultLoadout;
	}

	public void setDefaultLifepool(int defaultLifepool) {
		this.defaultLifepool = defaultLifepool;
	}

	public int getDefaultLifepool() {
		return defaultLifepool;
	}

	public void setDefaultFriendlyFire(boolean defaultFriendlyFire) {
		this.defaultFriendlyFire = defaultFriendlyFire;
	}

	public boolean getDefaultFriendlyFire() {
		return defaultFriendlyFire;
	}

}
