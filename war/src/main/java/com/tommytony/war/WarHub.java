package com.tommytony.war;

import org.bukkit.Location;
import org.bukkit.World;

import com.tommytony.war.volumes.Volume;

import bukkit.tommytony.war.War;

public class WarHub {
	private final War war;
	private final World world;
	private final Location location;
	private Volume volume;
	
	public WarHub(War war, World world, Location location) {
		this.war = war;
		this.world = world;
		this.location = location;
		this.volume = new Volume("warHub", war, warzone);
	}

}
