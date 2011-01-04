import java.util.List;


public class WarMapper {
	
	public static void load(War war) {
		PropertiesFile warConfig = new PropertiesFile("war.txt");
		
		// Create file if need be
		if(!warConfig.containsKey("warzones")) {
			WarMapper.save(war);
			war.getLogger().info("War config file created.");			
		}
		
		// warzones
		String warzonesStr = warConfig.getString("warzones");
		String[] warzoneSplit = warzonesStr.split(",");
		war.getWarzones().clear();
		for(String warzoneName : warzoneSplit) {
			if(warzoneName != null && !warzoneName.equals("")){
				Warzone zone = WarzoneMapper.load(war, warzoneName);		// cascade load
				war.getWarzones().add(zone);
				zone.resetState();			// is this wise?
			}
		}
		
		// defaultLoadout
		String defaultLoadoutStr = warConfig.getString("defaultLoadout");
		String[] defaultLoadoutSplit = defaultLoadoutStr.split(";");
		war.getDefaultLoadout().clear();
		for(String itemStr : defaultLoadoutSplit) {
			if(itemStr != null && !itemStr.equals("")) {
				String[] itemStrSplit = itemStr.split(",");
				Item item = new Item(Integer.parseInt(itemStrSplit[0]),
						Integer.parseInt(itemStrSplit[1]), Integer.parseInt(itemStrSplit[2]));
				war.getDefaultLoadout().add(item);
			}
		}
		
		// defaultLifepool
		war.setDefaultLifepool(warConfig.getInt("defaultLifepool"));
		
		// defaultFriendlyFire
		war.setDefaultFriendlyFire(warConfig.getBoolean("defaultFriendlyFire"));
	}
	
	public static void save(War war) {
		PropertiesFile warConfig = new PropertiesFile("war.txt");
		String warzonesStr = "";
		
		// warzones
		for(Warzone zone : war.getWarzones()) {
			warzonesStr += zone.getName() + ",";
		}
		warConfig.setString("warzones", warzonesStr);
		
		// defaultLoadout
		String defaultLoadoutStr = "";
		List<Item> items = war.getDefaultLoadout();
		for(Item item : items) {
			defaultLoadoutStr += item.getItemId() + "," + item.getAmount() + "," + item.getSlot() + ";";
		}
		warConfig.setString("defaultLoadout", defaultLoadoutStr);
		
		// defaultLifepool
		warConfig.setInt("defaultLifePool", war.getDefaultLifepool());
		
		// defaultFriendlyFire
		warConfig.setBoolean("defaultFriendlyFire", war.getDefaultFriendlyFire());
		
		warConfig.save();
	}
}
