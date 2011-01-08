package com.tommytony.war;

import org.bukkit.*;

public class Monument {
	private Location location;
	private int[] initialState = new int[26];
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
		
		// center
		getInitialState()[0] = world.getBlockAt(x, y, z).getTypeID();
		getInitialState()[1] = world.getBlockAt(x, y-1, z).getTypeID();
		
		// inner ring
		getInitialState()[2] = world.getBlockAt(x+1, y-1, z+1).getTypeID();
		getInitialState()[3] = world.getBlockAt(x+1, y-1, z).getTypeID();
		getInitialState()[4] = world.getBlockAt(x+1, y-1, z-1).getTypeID();
		
		getInitialState()[5] = world.getBlockAt(x, y-1, z+1).getTypeID();
		getInitialState()[6] = world.getBlockAt(x, y-1, z-1).getTypeID();
		
		getInitialState()[7] = world.getBlockAt(x-1, y-1, z+1).getTypeID();
		getInitialState()[9] = world.getBlockAt(x-1, y-1, z).getTypeID();
		getInitialState()[9] = world.getBlockAt(x-1, y-1, z-1).getTypeID();
		
		// outer ring
		getInitialState()[10] = world.getBlockAt(x+2, y-1, z+2).getTypeID();
		getInitialState()[11] = world.getBlockAt(x+2, y-1, z+1).getTypeID();
		getInitialState()[12] = world.getBlockAt(x+2, y-1, z).getTypeID();
		getInitialState()[13] = world.getBlockAt(x+2, y-1, z-1).getTypeID();
		getInitialState()[14] = world.getBlockAt(x+2, y-1, z-2).getTypeID();
		
		getInitialState()[15] = world.getBlockAt(x-1, y-1, z+2).getTypeID();
		getInitialState()[16] = world.getBlockAt(x-1, y-1, z-2).getTypeID();
		
		getInitialState()[17] = world.getBlockAt(x, y-1, z+2).getTypeID();
		getInitialState()[18] = world.getBlockAt(x, y-1, z-2).getTypeID();
		
		getInitialState()[19] = world.getBlockAt(x+1, y-1, z+2).getTypeID();
		getInitialState()[20] = world.getBlockAt(x+1, y-1, z-2).getTypeID();
		
		getInitialState()[21] = world.getBlockAt(x-2, y-1, z+2).getTypeID();
		getInitialState()[22] = world.getBlockAt(x-2, y-1, z+1).getTypeID();
		getInitialState()[23] = world.getBlockAt(x-2, y-1, z).getTypeID();
		getInitialState()[24] = world.getBlockAt(x-2, y-1, z-1).getTypeID();
		getInitialState()[25] = world.getBlockAt(x-2, y-1, z-2).getTypeID();
		
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
		int x = location.getBlockX();
		int y = location.getBlockY();
		int z = location.getBlockZ();
		
		world.getBlockAt(x+1, y-1, z+1).setType(Material.GlowingRedstoneOre);
		world.getBlockAt(x+1, y-1, z).setType(Material.GlowingRedstoneOre);
		world.getBlockAt(x+1, y-1, z-1).setType(Material.GlowingRedstoneOre);
		
		world.getBlockAt(x, y-1, z+1).setType(Material.GlowingRedstoneOre);		
		world.getBlockAt(x, y-1, z-1).setType(Material.GlowingRedstoneOre);
		
		world.getBlockAt(x-1, y-1, z+1).setType(Material.GlowingRedstoneOre);
		world.getBlockAt(x-1, y-1, z).setType(Material.GlowingRedstoneOre);
		world.getBlockAt(x-1, y-1, z-1).setType(Material.GlowingRedstoneOre);
	}
	
	public void smother() {
		ownerTeam = null;
		int x = location.getBlockX();
		int y = location.getBlockY();
		int z = location.getBlockZ();
		world.getBlockAt(x+1, y-1, z+1).setType(Material.CoalOre);
		world.getBlockAt(x+1, y-1, z).setType(Material.CoalOre);
		world.getBlockAt(x+1, y-1, z-1).setType(Material.CoalOre);
		
		world.getBlockAt(x, y-1, z+1).setType(Material.CoalOre);		
		world.getBlockAt(x, y-1, z-1).setType(Material.CoalOre);
		
		world.getBlockAt(x-1, y-1, z+1).setType(Material.CoalOre);
		world.getBlockAt(x-1, y-1, z).setType(Material.CoalOre);
		world.getBlockAt(x-1, y-1, z-1).setType(Material.CoalOre);
		
	}

	public void reset() {
		this.ownerTeam = null;
		int x = location.getBlockX();
		int y = location.getBlockY();
		int z = location.getBlockZ();
		
		// center
		world.getBlockAt(x, y, z).setType(Material.Air);
		world.getBlockAt(x, y-1, z).setType(Material.Soil);
		
		// inner ring
		world.getBlockAt(x+1, y-1, z+1).setType(Material.CoalOre);
		world.getBlockAt(x+1, y-1, z).setType(Material.CoalOre);
		world.getBlockAt(x+1, y-1, z-1).setType(Material.CoalOre);
		
		world.getBlockAt(x, y-1, z+1).setType(Material.CoalOre);		
		world.getBlockAt(x, y-1, z-1).setType(Material.CoalOre);
		
		world.getBlockAt(x-1, y-1, z+1).setType(Material.CoalOre);
		world.getBlockAt(x-1, y-1, z).setType(Material.CoalOre);
		world.getBlockAt(x-1, y-1, z-1).setType(Material.CoalOre);
		
		// outer ring
		//world.getBlockAt(x+2, y-1, z+2).setType(Material.Obsidian);
		world.getBlockAt(x+2, y-1, z+1).setType(Material.Obsidian);
		world.getBlockAt(x+2, y-1, z).setType(Material.Obsidian);
		world.getBlockAt(x+2, y-1, z-1).setType(Material.Obsidian);
		//world.getBlockAt(x+2, y-1, z-2).setType(Material.Obsidian);
		
		world.getBlockAt(x-1, y-1, z+2).setType(Material.Obsidian);
		world.getBlockAt(x-1, y-1, z-2).setType(Material.Obsidian);
		
		world.getBlockAt(x, y-1, z+2).setType(Material.Obsidian);
		world.getBlockAt(x, y-1, z-2).setType(Material.Obsidian);
		
		world.getBlockAt(x+1, y-1, z+2).setType(Material.Obsidian);
		world.getBlockAt(x+1, y-1, z-2).setType(Material.Obsidian);
		
		//world.getBlockAt(x-2, y-1, z+2).setType(Material.Obsidian);
		world.getBlockAt(x-2, y-1, z+1).setType(Material.Obsidian);
		world.getBlockAt(x-2, y-1, z).setType(Material.Obsidian);
		world.getBlockAt(x-2, y-1, z-1).setType(Material.Obsidian);
		//world.getBlockAt(x-2, y-1, z-2).setType(Material.Obsidian);
	}
	
	public void remove() {
		int x = location.getBlockX();
		int y = location.getBlockY();
		int z = location.getBlockZ();
		
		// center
		world.getBlockAt(x, y, z).setTypeID(getInitialState()[0]);
		world.getBlockAt(x, y - 1, z).setTypeID(getInitialState()[1]);

		// inner ring
		world.getBlockAt(x+1, y-1, z+1).setTypeID(getInitialState()[2]);
		world.getBlockAt(x+1, y-1, z).setTypeID(getInitialState()[3]);
		world.getBlockAt(x+1, y-1, z-1).setTypeID(getInitialState()[4]);
		
		world.getBlockAt(x, y-1, z+1).setTypeID(getInitialState()[5]);
		world.getBlockAt(x, y-1, z-1).setTypeID(getInitialState()[6]);
		
		world.getBlockAt(x-1, y-1, z+1).setTypeID(getInitialState()[7]);
		world.getBlockAt(x-1, y-1, z).setTypeID(getInitialState()[8]);
		world.getBlockAt(x-1, y-1, z-1).setTypeID(getInitialState()[9]);
		
		// outer ring
		world.getBlockAt(x+2, y-1, z+2).setTypeID(getInitialState()[10]);
		world.getBlockAt(x+2, y-1, z+1).setTypeID(getInitialState()[11]);
		world.getBlockAt(x+2, y-1, z).setTypeID(getInitialState()[12]);
		world.getBlockAt(x+2, y-1, z-1).setTypeID(getInitialState()[13]);
		world.getBlockAt(x+2, y-1, z-2).setTypeID(getInitialState()[14]);
		
		world.getBlockAt(x-1, y-1, z+2).setTypeID(getInitialState()[15]);
		world.getBlockAt(x-1, y-1, z-2).setTypeID(getInitialState()[16]);
		
		world.getBlockAt(x, y-1, z+2).setTypeID(getInitialState()[17]);
		world.getBlockAt(x, y-1, z-2).setTypeID(getInitialState()[18]);
		
		world.getBlockAt(x+1, y-1, z+2).setTypeID(getInitialState()[19]);
		world.getBlockAt(x+1, y-1, z-2).setTypeID(getInitialState()[20]);
		
		world.getBlockAt(x-2, y-1, z+2).setTypeID(getInitialState()[21]);
		world.getBlockAt(x-2, y-1, z+1).setTypeID(getInitialState()[22]);
		world.getBlockAt(x-2, y-1, z).setTypeID(getInitialState()[23]);
		world.getBlockAt(x-2, y-1, z-1).setTypeID(getInitialState()[24]);
		world.getBlockAt(x-2, y-1, z-2).setTypeID(getInitialState()[25]);
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
		if(/*(bx == x && by == y && bz == z) ||*/ 
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
