

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;



public class War extends Plugin {
	
	private WarListener listener = new WarListener(this);
    private Logger log;
    String name = "War";
    String version = "0.1";
    
    
    private final List<Warzone> warzones = new ArrayList<Warzone>();
    //private final WarMessenger messenger = new WarMessenger();

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
		etc.getLoader().addListener( PluginLoader.Hook.HEALTH_CHANGE,  
				listener,
				this,
				PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener( PluginLoader.Hook.DAMAGE,  
				listener,
				this,
				PluginListener.Priority.HIGH);
		
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

	public Team getPlayerTeam(String playerName) {
		for(Warzone warzone : warzones) {
			return warzone.getPlayerTeam(playerName);
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
		String out = "[war] " + str;
		out = str(Colors.LightGray, out);
		if(out.length() > 120) {
			out = out.substring(0, 119);
		}
		log.log(Level.INFO, "Out: " + out);
		return out;
	}
	
	public String str(String color, String str) {
		
		if(str.length() > 60) {
			String out = "";
			List<String> subStrs = toSubStrings(str);
			List<String> coloredSubStrs = new ArrayList<String>();
			for(String sub : subStrs) {
				String colored = "";
				if(sub.length() < 60) {
					colored = color + sub;
				}
				else {
//					colored = color + sub.charAt(0) + color + sub.charAt(1) + color + sub.charAt(2) + color + sub.charAt(3) + color + sub.charAt(4) +
//							sub.substring(5);
					//if(sub.length() > 10) {
						colored = color + sub;
						//.substring(5, sub.length() - 5);
						//colored += color + sub.charAt(sub.length() - 5) + color + sub.charAt(sub.length() - 4) + color + sub.charAt(sub.length() - 3) 
						//			+ color + sub.charAt(sub.length() - 2) + color + sub.charAt(sub.length() - 1); 
					//}
				}
				coloredSubStrs.add(colored);
			}
			for(String sub : coloredSubStrs) {
				out += sub;
			}
			
			return out;
		} else {
			return color + str;
		}
	}
	
	private List<String> toSubStrings(String str) {
		List<String> subStrings = new ArrayList<String>();
		int start = 0;
		int end = 60;
		while(end < str.length()) {
			subStrings.add(str.substring(start, end));
			start += 60;
			end += 60;
		}
		if(start < str.length()) {
			subStrings.add(str.substring(start));
		}
		return subStrings;
	}

	public Warzone findWarzone(String warzoneName) {
		for(Warzone warzone : warzones) {
			if(warzone.getName().equals(warzoneName)) {
				return warzone;
			}
		}
		return null;
	}

}
