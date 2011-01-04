
public class Monument {
	private Location location;
	private int[] initialState = new int[10];
	private War war = null;
	private Team ownerTeam = null;
	private final String name;
	
	public Monument(String name, War war, Location location) {
		this.name = name;
		this.location = location;
		this.war = war;
		int x = (int)location.x;
		int y = (int)location.y;
		int z = (int)location.z;
		initialState[0] = war.getServer().getBlockIdAt(x+1, y-1, z+1);
		initialState[1] = war.getServer().getBlockIdAt(x+1, y-1, z);
		initialState[2] = war.getServer().getBlockIdAt(x+1, y-1, z-1);
		initialState[3] = war.getServer().getBlockIdAt(x, y-1, z+1);
		initialState[4] = war.getServer().getBlockIdAt(x, y-1, z);
		initialState[5] = war.getServer().getBlockIdAt(x, y-1, z-1);
		initialState[6] = war.getServer().getBlockIdAt(x-1, y-1, z+1);
		initialState[7] = war.getServer().getBlockIdAt(x-1, y-1, z);
		initialState[8] = war.getServer().getBlockIdAt(x-1, y-1, z-1);
		initialState[9] = war.getServer().getBlockIdAt(x, y, z);
		this.reset();
	}
	
	public boolean isNear(Location playerLocation) {
		int x = (int)getLocation().x;
		int y = (int)getLocation().y;
		int z = (int)getLocation().z;
		int playerX = (int)playerLocation.x;
		int playerY = (int)playerLocation.y;
		int playerZ = (int)playerLocation.z;
		int diffX = Math.abs(playerX - x);
		int diffY = Math.abs(playerY - y);
		int diffZ = Math.abs(playerZ - z);
		if(diffX < 6 && diffY < 6 && diffZ < 6) {
			return true;
		}
		return false;
	}
	
	public boolean isOwner(Team team) {
		if(team == ownerTeam) {
			return true;
		}
		return false;
	}
	
	public boolean hasOwner() {
		return ownerTeam != null;
	}
	
	public void ignite(Team team) {
		ownerTeam = team;
	}
	
	public void smother() {
		ownerTeam = null;
	}

	public void reset() {
		this.ownerTeam = null;
		int x = (int)getLocation().x;
		int y = (int)getLocation().y;
		int z = (int)getLocation().z;
		war.getServer().setBlockAt(49, x+1, y-1, z+1);
		war.getServer().setBlockAt(49, x+1, y-1, z);
		war.getServer().setBlockAt(49, x+1, y-1, z-1);
		war.getServer().setBlockAt(49, x, y-1, z+1);
		war.getServer().setBlockAt(87, x, y-1, z);
		war.getServer().setBlockAt(49, x, y-1, z-1);
		war.getServer().setBlockAt(49, x-1, y-1, z+1);
		war.getServer().setBlockAt(49, x-1, y-1, z);
		war.getServer().setBlockAt(49, x-1, y-1, z-1);
		war.getServer().setBlockAt(0, x, y, z);
	}
	
	public void remove() {
		int x = (int)getLocation().x;
		int y = (int)getLocation().y;
		int z = (int)getLocation().z;
		war.getServer().setBlockAt(initialState[0], x+1, y-1, z+1);
		war.getServer().setBlockAt(initialState[1], x+1, y-1, z);
		war.getServer().setBlockAt(initialState[2], x+1, y-1, z-1);
		war.getServer().setBlockAt(initialState[3], x, y-1, z+1);
		war.getServer().setBlockAt(initialState[4], x, y-1, z);
		war.getServer().setBlockAt(initialState[5], x, y-1, z-1);
		war.getServer().setBlockAt(initialState[6], x-1, y-1, z+1);
		war.getServer().setBlockAt(initialState[7], x-1, y-1, z);
		war.getServer().setBlockAt(initialState[8], x-1, y-1, z-1);
		war.getServer().setBlockAt(initialState[9], x, y, z);
	}

	public Location getLocation() {
		return location;
	}

	public void setOwnerTeam(Team team) {
		this.ownerTeam = team;
		
	}

	public String getName() {
		return name;
	}
	
	
	
}
