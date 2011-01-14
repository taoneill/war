package bukkit.tommytony.war;

import org.bukkit.*;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.tommytony.war.Team;
import com.tommytony.war.WarHub;
import com.tommytony.war.Warzone;
import com.tommytony.war.mappers.WarMapper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

/**
 * 
 * @author tommytony
 *
 */
public class War extends JavaPlugin {
	
	public War(PluginLoader pluginLoader, Server instance,
			PluginDescriptionFile desc, File plugin, ClassLoader cLoader) {
		super(pluginLoader, instance, desc, plugin, cLoader);
	}
	

	private WarPlayerListener playerListener = new WarPlayerListener(this);
	private WarEntityListener entityListener = new WarEntityListener(this);
	private WarBlockListener blockListener = new WarBlockListener(this);
    private Logger log;
    String name = "War";
    String version = "0.3";
    
    private final List<Warzone> warzones = new ArrayList<Warzone>();
    private final List<String> zoneMakerNames = new ArrayList<String>();
    private final HashMap<Integer, ItemStack> defaultLoadout = new HashMap<Integer, ItemStack>();
    private int defaultLifepool = 7;
    private boolean defaultFriendlyFire = false;
	private boolean defaultDrawZoneOutline = true;
	private boolean defaultAutoAssignOnly = false;
	private WarHub warHub;
	
	public void onDisable() {
		Logger.getLogger("Minecraft").info(name + " " + version + " disabled.");
	}

	public void onEnable() {
		this.log = Logger.getLogger("Minecraft");
		
		// Register hooks		
		PluginManager pm = getServer().getPluginManager();
		
		pm.registerEvent(Event.Type.PLAYER_LOGIN, playerListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_QUIT, playerListener, Priority.Normal, this);	// DISCONNECT
		pm.registerEvent(Event.Type.PLAYER_COMMAND, playerListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_MOVE, playerListener, Priority.Normal, this);
		
		pm.registerEvent(Event.Type.ENTITY_DEATH, entityListener, Priority.Normal, this); //HEALTH_CHANGE
		pm.registerEvent(Event.Type.ENTITY_DAMAGEDBY_ENTITY, entityListener, Priority.Normal, this); //DAMAGE
		
		pm.registerEvent(Event.Type.BLOCK_PLACED, blockListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.BLOCK_IGNITE, blockListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.BLOCK_FLOW, blockListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.BLOCK_CANBUILD, blockListener, Priority.Normal, this);	// BLOCK_PLACE
		pm.registerEvent(Event.Type.BLOCK_DAMAGED, blockListener, Priority.Normal, this);	// BROKEN
		
		// Load files from disk or create them (using these defaults)
		this.defaultLoadout.put(0, new ItemStack(Material.STONE_SWORD));
		this.defaultLoadout.put(1, new ItemStack(Material.BOW));
		this.defaultLoadout.put(2, new ItemStack(Material.ARROW, 7));
		this.defaultLoadout.put(3, new ItemStack(Material.STONE_PICKAXE));
		this.defaultLoadout.put(4, new ItemStack(Material.STONE_SPADE));
		this.defaultLoadout.put(5, new ItemStack(Material.STONE_AXE));
		this.defaultLoadout.put(6, new ItemStack(Material.BREAD, 2));
		this.defaultLifepool = 7;
		this.defaultFriendlyFire = false;
		this.defaultAutoAssignOnly = false;
		WarMapper.load(this, this.getServer().getWorlds()[0]);
		
		getLogger().info(name + " " + version + " enabled.");
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
			if(warzone.getVolume() != null && warzone.getVolume().contains(location)) return warzone;
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

	public List<Warzone> getWarzones() {
		return warzones;
	}
	
	public String str(String str) {
		String out = ChatColor.GRAY + "[war] " + ChatColor.WHITE + str;
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

	public HashMap<Integer, ItemStack> getDefaultLoadout() {
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

	public String getName() {
		return name;
	}

	public Warzone zoneOfZoneWallAtProximity(Location location) {
		for(Warzone zone : warzones) {
			if(zone.isNearWall(location)) return zone;
		}
		return null;
	}

	public List<String> getZoneMakerNames() {
		return zoneMakerNames;
	}
	
	public boolean isZoneMaker(String playerName) {
		for(String zoneMaker : zoneMakerNames) {
			if(zoneMaker.equals(playerName)) return true;
		}
		return false;			
	}

	public boolean getDefaultDrawZoneOutline() {
		return defaultDrawZoneOutline ;
	}

	public boolean getDefaultAutoAssignOnly() {
		
		return defaultAutoAssignOnly;
	}

	public void setDefaultAutoAssignOnly(boolean autoAssign) {
		this.defaultAutoAssignOnly = autoAssign;
	}

	public WarHub getWarHub() {
		// TODO Auto-generated method stub
		return warHub;
	}

	public void setWarHub(WarHub warHub) {
		this.warHub = warHub;
	}

	
}
