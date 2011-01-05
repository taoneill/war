
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
		getInitialState()[0] = war.getServer().getBlockIdAt(x+1, y-1, z+1);
		getInitialState()[1] = war.getServer().getBlockIdAt(x+1, y-1, z);
		getInitialState()[2] = war.getServer().getBlockIdAt(x+1, y-1, z-1);
		getInitialState()[3] = war.getServer().getBlockIdAt(x, y-1, z+1);
		getInitialState()[4] = war.getServer().getBlockIdAt(x, y-1, z);
		getInitialState()[5] = war.getServer().getBlockIdAt(x, y-1, z-1);
		getInitialState()[6] = war.getServer().getBlockIdAt(x-1, y-1, z+1);
		getInitialState()[7] = war.getServer().getBlockIdAt(x-1, y-1, z);
		getInitialState()[8] = war.getServer().getBlockIdAt(x-1, y-1, z-1);
		getInitialState()[9] = war.getServer().getBlockIdAt(x, y, z);
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
		war.getServer().setBlockAt(getInitialState()[0], x+1, y-1, z+1);
		war.getServer().setBlockAt(getInitialState()[1], x+1, y-1, z);
		war.getServer().setBlockAt(getInitialState()[2], x+1, y-1, z-1);
		war.getServer().setBlockAt(getInitialState()[3], x, y-1, z+1);
		war.getServer().setBlockAt(getInitialState()[4], x, y-1, z);
		war.getServer().setBlockAt(getInitialState()[5], x, y-1, z-1);
		war.getServer().setBlockAt(getInitialState()[6], x-1, y-1, z+1);
		war.getServer().setBlockAt(getInitialState()[7], x-1, y-1, z);
		war.getServer().setBlockAt(getInitialState()[8], x-1, y-1, z-1);
		war.getServer().setBlockAt(getInitialState()[9], x, y, z);
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

	public void setInitialState(int[] initialState) {
		this.initialState = initialState;
	}

	public int[] getInitialState() {
		return initialState;
	}

	public void setLocation(Location location) {
		this.location = location;
		int x = (int)location.x;
		int y = (int)location.y;
		int z = (int)location.z;
		getInitialState()[0] = war.getServer().getBlockIdAt(x+1, y-1, z+1);
		getInitialState()[1] = war.getServer().getBlockIdAt(x+1, y-1, z);
		getInitialState()[2] = war.getServer().getBlockIdAt(x+1, y-1, z-1);
		getInitialState()[3] = war.getServer().getBlockIdAt(x, y-1, z+1);
		getInitialState()[4] = war.getServer().getBlockIdAt(x, y-1, z);
		getInitialState()[5] = war.getServer().getBlockIdAt(x, y-1, z-1);
		getInitialState()[6] = war.getServer().getBlockIdAt(x-1, y-1, z+1);
		getInitialState()[7] = war.getServer().getBlockIdAt(x-1, y-1, z);
		getInitialState()[8] = war.getServer().getBlockIdAt(x-1, y-1, z-1);
		getInitialState()[9] = war.getServer().getBlockIdAt(x, y, z);
		this.reset();
	}

	public boolean contains(Block block) {
		int x = (int)location.x;
		int y = (int)location.y;
		int z = (int)location.z;
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
