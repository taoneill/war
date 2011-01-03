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
	
	public int saveState() {
		if(ready()){
			int northSouth = ((int)(southeast.x)) - ((int)(northwest.x));
			int eastWest = ((int)(northwest.z)) - ((int)(southeast.z));
			initialState = new int[northSouth][128][eastWest];
			int noOfSavedBlocks = 0;
			int x = (int)northwest.x;
			int minY = 0;
			int maxY = 128;
			for(int i = 0; i < northSouth; i++){
				int y = minY;
				for(int j = 0; j < maxY - minY; j++) {
					int z = (int)southeast.z;
					for(int k = 0; k < eastWest; k++) {
						initialState[i][j][k] = server.getBlockIdAt(x, y, z);
						noOfSavedBlocks++;
						z++;
					}
					y++;
				}
				x++;
			}
			return noOfSavedBlocks;
		}
		return 0;
	}
	
	public int resetState() {
		if(ready() && initialState != null){
			// reset blocks
			int northSouth = ((int)(southeast.x)) - ((int)(northwest.x));
			int eastWest = ((int)(northwest.z)) - ((int)(southeast.z));
			int noOfResetBlocks = 0;
			int noOfFailures = 0;
			int x = (int)northwest.x;
			int minY = 0;
			int maxY = 128;
			for(int i = 0; i < northSouth; i++){
				int y = minY;
				for(int j = 0; j < maxY - minY; j++) {
					int z = (int)southeast.z;
					for(int k = 0; k < eastWest; k++) {
						int currentType = server.getBlockIdAt(x, y, z);
						int initialType = initialState[i][j][k];
						if(currentType != initialType) {
							if(server.setBlockAt(initialType,x, y, z)) {
								noOfResetBlocks++;
							} else {
								noOfFailures++;
							}
						}
						z++;
					}
					y++;					
				}
				x++;
			}
			
			// everyone back to team spawn with full health
			for(Team team : teams) {
				for(Player player : team.getPlayers()) {
					player.setHealth(20);
					player.teleportTo(team.getTeamSpawn());
				}
			}
			
			return noOfResetBlocks;
		}
		return 0;
	}
	
}
