import java.util.ArrayList;
import java.util.List;


public class Warzone {
	private String name;
	private Location northwest;
	private Location southeast;
	private final List<Team> teams = new ArrayList<Team>();
	private final Server server;
	
	private int[][][] initialState = null;
	private Location teleport; 
	
	public Warzone(Server server, String name) {
		this.server = server;
		this.name = name;
	}
	
	public boolean ready() {
		if(getNorthwest() != null && getSoutheast() != null 
				&& !tooSmall() && !tooBig()) return true;
		return false;
	}
	
	public boolean tooSmall() {
		if((getSoutheast().x - getNorthwest().x < 20)
				|| (getNorthwest().z - getSoutheast().z < 20)) return true;
		return false;
	}
	
	public boolean tooBig() {
		if((getSoutheast().x - getNorthwest().x > 1000)
				|| (getNorthwest().z - getSoutheast().z > 1000)) return true;
		return false;
	}
	
	public boolean contains(Location point) {
		return point.x <= getSoutheast().x && point.x >= getNorthwest().x 
				&& point.z <= getNorthwest().z && point.z >= getSoutheast().z;
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

	public String getName() {
		return name;
	}

	public void setNorthwest(Location northwest) {
		this.northwest = northwest;
		if(ready()) {
			saveState();
		}
	}

	public void saveState() {
		if(ready()){
			int northSouth = (int)(southeast.x - northwest.x);
			int eastWest = (int)(northwest.z - southeast.z);
			initialState = new int[northSouth][128][eastWest];
			for(int x = 0; x < northSouth; x++){
				for(int y = 0; y < 128; y++) {
					for(int z = 0; z < eastWest; z++) {
						initialState[x][y][z] = server.getBlockAt(x, y, z).getType();
					}
				}
			}
		}
	}
	
	public void resetState() {
		if(ready() && initialState != null){
			// reset blocks
			int northSouth = (int)(southeast.x - northwest.x);
			int eastWest = (int)(northwest.z - southeast.z);
			initialState = new int[northSouth][128][eastWest];
			for(int x = 0; x < northSouth; x++){
				for(int y = 0; y < 128; y++) {
					for(int z = 0; z < eastWest; z++) {
						server.setBlockAt(initialState[x][y][z],x, y, z);
					}
				}
			}
			
			// everyone back to team spawn with full health
			for(Team team : teams) {
				for(Player player : team.getPlayers()) {
					player.setHealth(20);
					player.teleportTo(team.getTeamSpawn());
				}
			}
		}
	}

	public Location getNorthwest() {
		return northwest;
	}

	public void setSoutheast(Location southeast) {
		this.southeast = southeast;
		if(ready()) {
			saveState();
		}
	}

	public Location getSoutheast() {
		return southeast;
	}

	public void setTeleport(Location location) {
		this.teleport = location;
	}

	public Location getTeleport() {
		return this.teleport;
	}
	
	
}
