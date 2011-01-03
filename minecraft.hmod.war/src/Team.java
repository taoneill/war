import java.util.ArrayList;
import java.util.List;

public class Team {
	private List<Player> players = new ArrayList<Player>();
	private Location teamSpawn = null;
	private String name;
	
	public Team(String name, Location teamSpawn) {
		this.setName(name);
		this.teamSpawn = teamSpawn;
	}
	
	public void setTeamSpawn(Location teamSpawn) {
		this.teamSpawn = teamSpawn;
	}
	
	public Location getTeamSpawn() {
		return teamSpawn;
	}
	
	public void addPlayer(Player player) {
		this.players.add(player);
	}
	
	public List<Player> getPlayers() {
		return players;
	}
	
	public void teamcast(String message) {
		for(Player player : players) {
			player.sendMessage(message);
		}
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public boolean removePlayer(String name) {
		Player thePlayer = null;
		for(Player player : players) {
			if(player.getName().equals(name)) {
				thePlayer = player; 
			}
		}
		if(thePlayer != null) {
			players.remove(thePlayer);
			return true;
		}
		return false;
	}

}
