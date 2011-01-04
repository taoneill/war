import java.io.File;
import java.util.List;


public class WarzoneMapper {

	public static Warzone load(War war, String name) {
		PropertiesFile warzoneConfig = new PropertiesFile("warzone-" + name + ".txt");
		Warzone warzone = new Warzone(war, name);
		
		// Create file if needed 
		if(!warzoneConfig.containsKey("name")) {
			WarzoneMapper.save(warzone);
			war.getLogger().info("Warzone " + name + " config file created.");
		}
				
		// northwest
		String nwStr = warzoneConfig.getString("northWest");
		String[] nwStrSplit = nwStr.split(",");
		int nwX = Integer.parseInt(nwStrSplit[0]);
		int nwY = Integer.parseInt(nwStrSplit[1]);
		int nwZ = Integer.parseInt(nwStrSplit[2]);
		Location nw = new Location(nwX, nwY, nwZ);
		warzone.setNorthwest(nw);
		
		// southeast
		String seStr = warzoneConfig.getString("southEast");
		String[] seStrSplit = seStr.split(",");
		int seX = Integer.parseInt(seStrSplit[0]);
		int seY = Integer.parseInt(seStrSplit[1]);
		int seZ = Integer.parseInt(seStrSplit[2]);
		Location se = new Location(seX, seY, seZ);
		warzone.setSoutheast(se);
		
		// teleport
		String teleportStr = warzoneConfig.getString("teleport");
		if(teleportStr != null && !teleportStr.equals("")) {
			String[] teleportSplit = teleportStr.split(",");
			int teleX = Integer.parseInt(teleportSplit[0]);
			int teleY = Integer.parseInt(teleportSplit[1]);
			int teleZ = Integer.parseInt(teleportSplit[2]);
			warzone.setTeleport(new Location(teleX, teleY, teleZ));
		}
		
		// teams
		String teamsStr = warzoneConfig.getString("teams");
		String[] teamsSplit = teamsStr.split(";");
		warzone.getTeams().clear();
		for(String teamStr : teamsSplit) {
			if(teamStr != null && !teamStr.equals("")){
				String[] teamStrSplit = teamStr.split(",");
				int teamX = Integer.parseInt(teamStrSplit[1]);
				int teamY = Integer.parseInt(teamStrSplit[2]);
				int teamZ = Integer.parseInt(teamStrSplit[3]);
				Team team = new Team(teamStrSplit[0], 
									new Location(teamX, teamY, teamZ));
				warzone.getTeams().add(team);
			}
		}
		
		// ff
		warzone.setFriendlyFire(warzoneConfig.getBoolean("friendlyFire"));
		
		// loadout
		String loadoutStr = warzoneConfig.getString("loadout");
		String[] loadoutStrSplit = loadoutStr.split(";");
		warzone.getLoadout().clear();
		for(String itemStr : loadoutStrSplit) {
			if(itemStr != null && !itemStr.equals("")) {
				String[] itemStrSplit = itemStr.split(",");
				Item item = new Item(Integer.parseInt(itemStrSplit[0]),
						Integer.parseInt(itemStrSplit[1]), Integer.parseInt(itemStrSplit[2]));
				warzone.getLoadout().add(item);
			}
		}
		
		// life pool
		warzone.setLifePool(warzoneConfig.getInt("lifePool"));
				
		// monuments
		String monumentsStr = warzoneConfig.getString("monuments");
		String[] monumentsSplit = monumentsStr.split(";");
		warzone.getMonuments().clear();
		for(String monumentStr  : monumentsSplit) {
			if(monumentStr != null && !monumentStr.equals("")){
				String[] monumentStrSplit = monumentStr.split(",");
				int monumentX = Integer.parseInt(monumentStrSplit[1]);
				int monumentY = Integer.parseInt(monumentStrSplit[2]);
				int monumentZ = Integer.parseInt(monumentStrSplit[3]);
				Monument monument = new Monument(monumentStrSplit[0], war, 
										new Location(monumentX, monumentY, monumentZ));
				warzone.getMonuments().add(monument);
			}
		}
		
		return warzone;
		
	}
	
	public static void save(Warzone warzone) {
		PropertiesFile warzoneConfig = new PropertiesFile("warzone-" + warzone.getName() + ".txt");
		
		// name
		warzoneConfig.setString("name", warzone.getName());
		
		// northwest
		String nwStr = "";
		Location nw = warzone.getNorthwest();
		if(nw != null) {
			nwStr = (int)nw.x + "," + (int)nw.y + "," + (int)nw.z;
		}
		warzoneConfig.setString("northWest", nwStr);
		
		// southeast
		String seStr = "";
		Location se = warzone.getSoutheast();
		if(se != null) {
			seStr = (int)se.x + "," + (int)se.y + "," + (int)se.z;
		}
		warzoneConfig.setString("southEast", seStr);
		
		// teleport
		String teleportStr = "";
		Location tele = warzone.getTeleport();
		if(tele != null) {
			teleportStr = (int)tele.x + "," + (int)tele.y + "," + (int)tele.z;
		}
		warzoneConfig.setString("teleport", teleportStr);
		
		// teams
		String teamsStr = "";
		List<Team> teams = warzone.getTeams();
		for(Team team : teams) {
			Location spawn = team.getTeamSpawn();
			teamsStr += team.getName() + "," + (int)spawn.x + "," + (int)spawn.y + "," + (int)spawn.z + ";";
		}
		warzoneConfig.setString("teams", teamsStr);
		
		// ff
		warzoneConfig.setBoolean("firendlyFire", warzone.getFriendlyFire());
		
		// loadout
		String loadoutStr = "";
		List<Item> items = warzone.getLoadout();
		for(Item item : items) {
			loadoutStr += item.getItemId() + "," + item.getAmount() + "," + item.getSlot() + ";";
		}
		warzoneConfig.setString("loadout", loadoutStr);
		
		// life pool
		warzoneConfig.setInt("lifePool", warzone.getLifePool());
		
		// monuments
		String monumentsStr = "";
		List<Monument> monuments = warzone.getMonuments();
		for(Monument monument : monuments) {
			Location monumentLoc = monument.getLocation();
			monumentsStr += monument.getName() + "," + (int)monumentLoc.x + "," + (int)monumentLoc.y + "," + (int)monumentLoc.z + ";";
		}
		warzoneConfig.setString("monuments", monumentsStr);
		
		warzoneConfig.save();
	}
	
	public static void delete(String name) {
		File warzoneConfig = new File("warzone-" + name + ".txt");
		warzoneConfig.delete();
	}

}
