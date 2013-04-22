package com.tommytony.war.job;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;

import com.tommytony.war.Team;
import com.tommytony.war.Warzone;

public class CheckWolfsJob implements Runnable {

	private final Wolf[] wolves;
	private final Player[] players;
	private int shouldDie = 0;
	private Random r;
	
	public CheckWolfsJob(Wolf[] wolves, Player[] players) {
		this.wolves = wolves;
		this.players = players;
		this.r = new Random();
	}

	public void run() {
		for(int i = 0; i < 4; i++) {
			if(players[i] == null) {
				continue;
			}
			if(Warzone.getZoneByLocation(players[i]) == null) {
				players[i] = null;
				this.shouldDie += i;
			}
			if(wolves[i] == null) {
				continue;
			}
			double dist = wolves[i].getLocation().distanceSquared(players[i].getLocation());
			if(dist > 400) { //we are too far away, teleport closer
				Location l = players[i].getLocation();
				boolean notSpawned = true;
				int x = 0, y = 0, z = 0;
				while(notSpawned) {
				    Block b = l.getWorld().getBlockAt(l.getBlockX() + r.nextInt(11) - 10, l.getBlockZ() + r.nextInt(11) - 10,
				    		l.getBlockY() + r.nextInt(11) - 10);
				    if(b.getType() == Material.AIR) {
				    	notSpawned = false;
				    }
				}
				wolves[i].getLocation().setX(x);
				wolves[i].getLocation().setY(y);
				wolves[i].getLocation().setZ(z);
			}
		}
		if(shouldDie == 10) {
			Thread.yield();
		}
	}
}
