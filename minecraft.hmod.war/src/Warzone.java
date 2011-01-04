import java.util.ArrayList;
import java.util.List;

public class Warzone {
	private String name;
	private Location northwest;
	private Location southeast;
	private final List<Team> teams = new ArrayList<Team>();
	private final List<Monument> monuments = new ArrayList<Monument>();
	private final Server server;
	
	private int[][][] initialState = null;
	private Location teleport;
	private boolean friendlyFire;
	private War war;
	private int lifePool;
	private List<Item> loadout; 
	
	public Warzone(War war, String name) {
		this.war = war;
		this.server = war.getServer();
		this.name = name;
		this.friendlyFire = war.getDefaultFriendlyFire();
		this.setLifePool(war.getDefaultLifepool());
		this.setLoadout(war.getDefaultLoadout());
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
		return ready() && point.x <= getSoutheast().x && point.x >= getNorthwest().x 
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
		// remove old nw sign, if any (replace with air)
		if(this.northwest != null) {
			int x = (int)this.northwest.x;
			int y = (int)this.northwest.y;
			int z = (int)this.northwest.z;
			Block block = new Block(0, x, y, z);
			server.setBlock(block);
		}
		this.northwest = northwest;
		// add sign
		int x = (int)northwest.x;
		int y = (int)northwest.y;
		int z = (int)northwest.z;
		Block block = new Block(63, x, y, z, 10);	// towards southeast
		server.setBlock(block);
		block = server.getBlockAt(x, y, z);
		ComplexBlock complexBlock = server.getComplexBlock(x, y, z);
		Sign sign = (Sign)complexBlock;
		sign.setText(0, "Northwest");
		sign.setText(1, "corner of");
		sign.setText(2, "warzone");
		sign.setText(3, name);
		sign.update();
		
		saveState();
	}

	public Location getNorthwest() {
		return northwest;
	}

	public void setSoutheast(Location southeast) {
		// remove old se sign, if any (replace with air)
		if(this.southeast != null) {
			int x = (int)this.southeast.x;
			int y = (int)this.southeast.y;
			int z = (int)this.southeast.z;
			Block block = new Block(0, x, y, z);
			server.setBlock(block);
		}
		this.southeast = southeast;
		// add sign
		int x = (int)southeast.x;
		int y = (int)southeast.y;
		int z = (int)southeast.z;
		Block block = new Block(63, x, y, z, 2);	// towards northwest
		server.setBlock(block);
		block = server.getBlockAt(x, y, z);
		ComplexBlock complexBlock = server.getComplexBlock(x, y, z);
		Sign sign = (Sign)complexBlock;
		sign.setText(0, "Southeast");
		sign.setText(1, "corner of");
		sign.setText(2, "warzone");
		sign.setText(3, name);
		sign.update();
		
		saveState();
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
			setInitialState(new int[northSouth][128][eastWest]);
			int noOfSavedBlocks = 0;
			int x = (int)northwest.x;
			int minY = 0;
			int maxY = 128;
			for(int i = 0; i < northSouth; i++){
				int y = minY;
				for(int j = 0; j < maxY - minY; j++) {
					int z = (int)southeast.z;
					for(int k = 0; k < eastWest; k++) {
						getInitialState()[i][j][k] = server.getBlockIdAt(x, y, z);
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
	
	/**
	 * Goes back to the saved state of the warzone (resets only block types, not physics).
	 * Also teleports all players back to their respective spawns.
	 * @return
	 */
	public int resetState() {
		if(ready() && getInitialState() != null){
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
						int initialType = getInitialState()[i][j][k];
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
					respawnPlayer(team, player);
				}
				team.setRemainingTickets(War.LIFEPOOL);
			}
			
			// reset monuments
			for(Monument monument : monuments) {
				monument.reset();
			}
			
			this.setNorthwest(this.getNorthwest());
			this.setSoutheast(this.getSoutheast());
			
			return noOfResetBlocks;
		}
		return 0;
	}

	public void endRound() {
		
	}

	public void respawnPlayer(Team team, Player player) {
		Inventory playerInv = player.getInventory();
		playerInv.clearContents();
		playerInv.update();
		for(Item loadoutItem : loadout) {
			playerInv.addItem(loadoutItem);
		}
		playerInv.update();
		player.setHealth(30);
		player.setFireTicks(0);
		player.teleportTo(team.getTeamSpawn());
	}

	public boolean isMonumentFirestone(Block block) {
		for(Monument monument : monuments) {
			int x = (int)monument.getLocation().x;
			int y = (int)monument.getLocation().y;
			int z = (int)monument.getLocation().z;
			if(x == block.getX() && y == block.getY() && z == block.getZ()) {
				return true;
			}
		}
		return false;
	}
	
	public Monument getMonumentForFirestone(Block block) {
		for(Monument monument : monuments) {
			int x = (int)monument.getLocation().x;
			int y = (int)monument.getLocation().y;
			int z = (int)monument.getLocation().z;
			if(x == block.getX() && y == block.getY() && z == block.getZ()) {
				return monument;
			}
		}
		return null;
	}

	public boolean nearAnyOwnedMonument(Location to, Team team) {
		for(Monument monument : monuments) {
			if(monument.isNear(to) && monument.isOwner(team)) {
				return true;
			}
		}
		return false;
	}

	public List<Monument> getMonuments() {
		return monuments;
	}

	public boolean getFriendlyFire() {
		// TODO Auto-generated method stub
		return this.friendlyFire;
	}

	public void setLoadout(List<Item> loadout) {
		this.loadout = loadout;
	}

	public List<Item> getLoadout() {
		return loadout;
	}

	public void setLifePool(int lifePool) {
		this.lifePool = lifePool;
	}

	public int getLifePool() {
		return lifePool;
	}

	public void setFriendlyFire(boolean ffOn) {
		this.friendlyFire = ffOn;
	}

	public void setInitialState(int[][][] initialState) {
		this.initialState = initialState;
	}

	public int[][][] getInitialState() {
		return initialState;
	}

	
}
