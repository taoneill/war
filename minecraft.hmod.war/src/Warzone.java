import java.util.ArrayList;
import java.util.HashMap;
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
	
	private HashMap<String, Item[]> inventories = new HashMap<String, Item[]>();
	
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
	
	public void removeNorthwest() {
		int x = (int)northwest.x;
		int y = (int)northwest.y;
		int z = (int)northwest.z;
		server.setBlockAt(0, x, y, z);
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
	
	public void removeSoutheast() {
		int x = (int)southeast.x;
		int y = (int)southeast.y;
		int z = (int)southeast.z;
		server.setBlockAt(0, x, y, z);
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
				Location spawn = team.getTeamSpawn();
//				removeSpawnArea(team);	// reset spawn
//				addSpawnArea(team, spawn, 41);
				for(Player player : team.getPlayers()) {
					respawnPlayer(team, player);
				}
				team.setRemainingTickets(lifePool);
				resetSign(team, spawn);
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
	
	public void removeSpawnArea(Team team) {
		// Reset spawn to what it was before the gold blocks
		int[] spawnState = team.getOldSpawnState();
		int x = (int)team.getTeamSpawn().x;
		int y = (int)team.getTeamSpawn().y;
		int z = (int)team.getTeamSpawn().z;
		war.getServer().setBlockAt(spawnState[0], x+1, y-1, z+1);
		war.getServer().setBlockAt(spawnState[1], x+1, y-1, z);
		war.getServer().setBlockAt(spawnState[2], x+1, y-1, z-1);
		war.getServer().setBlockAt(spawnState[3], x, y-1, z+1);
		war.getServer().setBlockAt(spawnState[4], x, y-1, z);
		war.getServer().setBlockAt(spawnState[5], x, y-1, z-1);
		war.getServer().setBlockAt(spawnState[6], x-1, y-1, z+1);
		war.getServer().setBlockAt(spawnState[7], x-1, y-1, z);
		war.getServer().setBlockAt(spawnState[8], x-1, y-1, z-1);
		war.getServer().setBlockAt(spawnState[9], x, y, z);
		
	}

	public void addSpawnArea(Team team, Location location, int blockType) {
		// Save the spawn state (i.e. the nine block under the player spawn)
		int[] spawnState = new int[10];
		int x = (int)location.x;
		int y = (int)location.y;
		int z = (int)location.z;
		spawnState[0] = war.getServer().getBlockIdAt(x+1, y-1, z+1);
		spawnState[1] = war.getServer().getBlockIdAt(x+1, y-1, z);
		spawnState[2] = war.getServer().getBlockIdAt(x+1, y-1, z-1);
		spawnState[3] = war.getServer().getBlockIdAt(x, y-1, z+1);
		spawnState[4] = war.getServer().getBlockIdAt(x, y-1, z);
		spawnState[5] = war.getServer().getBlockIdAt(x, y-1, z-1);
		spawnState[6] = war.getServer().getBlockIdAt(x-1, y-1, z+1);
		spawnState[7] = war.getServer().getBlockIdAt(x-1, y-1, z);
		spawnState[8] = war.getServer().getBlockIdAt(x-1, y-1, z-1);
		spawnState[9] = war.getServer().getBlockIdAt(x, y, z);
		team.setTeamSpawn(location);
		team.setOldSpawnState(spawnState);
		// Set the spawn as gold blocks
		war.getServer().setBlockAt(blockType, x+1, y-1, z+1);
		war.getServer().setBlockAt(blockType, x+1, y-1, z);
		war.getServer().setBlockAt(blockType, x+1, y-1, z-1);
		war.getServer().setBlockAt(blockType, x, y-1, z+1);
		war.getServer().setBlockAt(blockType, x, y-1, z);
		war.getServer().setBlockAt(blockType, x, y-1, z-1);
		war.getServer().setBlockAt(blockType, x-1, y-1, z+1);
		war.getServer().setBlockAt(blockType, x-1, y-1, z);
		war.getServer().setBlockAt(blockType, x-1, y-1, z-1);
		
		resetSign(team, location);
	}
	
	public void resetSign(Team team, Location location){
		int x = (int)location.x;
		int y = (int)location.y;
		int z = (int)location.z;
		
		Block block = new Block(63, x, y, z, 8);
		war.getServer().setBlock(block);
		block = war.getServer().getBlockAt(x, y, z);
		ComplexBlock complexBlock = war.getServer().getComplexBlock(x, y, z);
		Sign sign = (Sign)complexBlock;
		sign.setText(0, "Team");
		sign.setText(1, team.getName());
		sign.setText(2, team.getPoints() + " pts");
		sign.setText(3, team.getRemainingTickets() + "/" + lifePool + " lives left");
		sign.update();
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

	public boolean hasPlayerInventory(String playerName) {
		return inventories.containsKey(playerName);
	}

	public void keepPlayerInventory(Player player) {
		inventories.put(player.getName(), player.getInventory().getContents());
	}

	public void restorePlayerInventory(Player player) {
		Item[] originalContents = inventories.remove(player.getName());
		Inventory playerInv = player.getInventory(); 
		playerInv.clearContents();
		playerInv.update();
		for(Item item : originalContents) {
			playerInv.addItem(item);
		}
		playerInv.update();
		player.getInventory().update();
	}

	public boolean hasMonument(String monumentName) {
		boolean hasIt = false;
		for(Monument monument: monuments) {
			if(monument.getName().equals(monumentName)) {
				return true;
			}
		}
		return false;
	}
	
	public Monument getMonument(String monumentName) {
		boolean hasIt = false;
		for(Monument monument: monuments) {
			if(monument.getName().equals(monumentName)) {
				return monument;
			}
		}
		return null;
	}
	
	public boolean isImportantBlock(Block block) {
		block.getX();
		for(Monument m : monuments) {
			if(m.contains(block)){
				return true;
			}
		}
		for(Team t : teams) {
			if(t.contains(block)){
				return true;
			}
		}
		if(teleportNear(block)) {
			return true;
		}
		return false;
	}

	private boolean teleportNear(Block block) {
		int x = (int)this.teleport.x;
		int y = (int)this.teleport.y;
		int z = (int)this.teleport.z;
		int bx = block.getX();
		int by = block.getY();
		int bz = block.getZ();
		if((bx == x && by == y && bz == z) || 
				(bx == x+1 && by == y-1 && bz == z+1) ||
				(bx == x+1 && by == y-1 && bz == z) ||
				(bx == x+1 && by == y-1 && bz == z-1) ||
				(bx == x && by == y-1 && bz == z+1) ||
				(bx == x && by == y-1 && bz == z) ||
				(bx == x && by == y-1 && bz == z-1) ||
				(bx == x-1 && by == y-1 && bz == z+1) ||
				(bx == x-1 && by == y-1 && bz == z) ||
				(bx == x-1 && by == y-1 && bz == z-1) ) {
			return true;
		}
		return false;
	}

	

}
