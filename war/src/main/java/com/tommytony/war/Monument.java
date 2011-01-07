package com.tommytony.war;

import org.bukkit.*;

public class Monument {
	private Location location;
	private int[] initialState = new int[10];
	private World world = null;
	private Team ownerTeam = null;
	private final String name;
	
	public Monument(String name, World world, Location location) {
		this.name = name;
		this.location = location;
		this.world = world;
		int x = location.getBlockX();
		int y = location.getBlockY();
		int z = location.getBlockZ();
		getInitialState()[0] = world.getBlockAt(x+1, y-1, z+1).getTypeID();
		getInitialState()[1] = world.getBlockAt(x+1, y-1, z).getTypeID();
		getInitialState()[2] = world.getBlockAt(x+1, y-1, z-1).getTypeID();
		getInitialState()[3] = world.getBlockAt(x, y-1, z+1).getTypeID();
		getInitialState()[4] = world.getBlockAt(x, y-1, z).getTypeID();
		getInitialState()[5] = world.getBlockAt(x, y-1, z-1).getTypeID();
		getInitialState()[6] = world.getBlockAt(x-1, y-1, z+1).getTypeID();
		getInitialState()[7] = world.getBlockAt(x-1, y-1, z).getTypeID();
		getInitialState()[8] = world.getBlockAt(x-1, y-1, z-1).getTypeID();
		getInitialState()[9] = world.getBlockAt(x, y, z).getTypeID();
		this.reset();
	}
	
	public boolean isNear(Location playerLocation) {
		int x = location.getBlockX();
		int y = location.getBlockY();
		int z = location.getBlockZ();
		int playerX = (int)playerLocation.getBlockX();
		int playerY = (int)playerLocation.getBlockY();
		int playerZ = (int)playerLocation.getBlockZ();
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
		int x = location.getBlockX();
		int y = location.getBlockY();
		int z = location.getBlockZ();
		world.getBlockAt(x+1, y-1, z+1).setTypeID(49);
		world.getBlockAt(x+1, y-1, z).setTypeID(49);
		world.getBlockAt(x+1, y-1, z-1).setTypeID(49);
		world.getBlockAt(x, y-1, z+1).setTypeID(49);
		world.getBlockAt(x, y-1, z).setTypeID(87);
		world.getBlockAt(x, y-1, z-1).setTypeID(49);
		world.getBlockAt(x-1, y-1, z+1).setTypeID(49);
		world.getBlockAt(x-1, y-1, z).setTypeID(49);
		world.getBlockAt(x-1, y-1, z-1).setTypeID(49);
		world.getBlockAt(x, y, z).setTypeID(0);
	}
	
	public void remove() {
		int x = location.getBlockX();
		int y = location.getBlockY();
		int z = location.getBlockZ();
		world.getBlockAt(x+1, y-1, z+1).setTypeID(getInitialState()[0]);
		world.getBlockAt(x+1, y-1, z).setTypeID(getInitialState()[1]);
		world.getBlockAt(x+1, y-1, z-1).setTypeID(getInitialState()[2]);
		world.getBlockAt(x, y-1, z+1).setTypeID(getInitialState()[3]);
		world.getBlockAt(x, y-1, z).setTypeID(getInitialState()[4]);
		world.getBlockAt(x, y-1, z-1).setTypeID(getInitialState()[5]);
		world.getBlockAt(x-1, y-1, z+1).setTypeID(getInitialState()[6]);
		world.getBlockAt(x-1, y-1, z).setTypeID(getInitialState()[7]);
		world.getBlockAt(x-1, y-1, z-1).setTypeID(getInitialState()[8]);
		world.getBlockAt(x, y, z).setTypeID(getInitialState()[9]);
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
		int x = location.getBlockX();
		int y = location.getBlockY();
		int z = location.getBlockZ();
		getInitialState()[0] = world.getBlockAt(x+1, y-1, z+1).getTypeID();
		getInitialState()[1] = world.getBlockAt(x+1, y-1, z).getTypeID();
		getInitialState()[2] = world.getBlockAt(x+1, y-1, z-1).getTypeID();
		getInitialState()[3] = world.getBlockAt(x, y-1, z+1).getTypeID();
		getInitialState()[4] = world.getBlockAt(x, y-1, z).getTypeID();
		getInitialState()[5] = world.getBlockAt(x, y-1, z-1).getTypeID();
		getInitialState()[6] = world.getBlockAt(x-1, y-1, z+1).getTypeID();
		getInitialState()[7] = world.getBlockAt(x-1, y-1, z).getTypeID();
		getInitialState()[8] = world.getBlockAt(x-1, y-1, z-1).getTypeID();
		getInitialState()[9] = world.getBlockAt(x, y, z).getTypeID();
		this.reset();
	}

	public boolean contains(Block block) {
		int x = location.getBlockX();
		int y = location.getBlockY();
		int z = location.getBlockZ();
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
