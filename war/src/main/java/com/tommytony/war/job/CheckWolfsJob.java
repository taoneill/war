package com.tommytony.war.job;

import java.util.Random;

import org.bukkit.Location;
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
			double dist = wolves[i].getLocation().distanceSquared(players[i].getLocation());
			if(dist > 900) { //we are too far away, teleport closer
				Location l = players[i].getLocation();
				wolves[i].getLocation().setX(l.getBlockX() + r.nextInt(11) - 10);
				wolves[i].getLocation().setZ(l.getBlockZ() + r.nextInt(11) - 10);
				wolves[i].getLocation().setY(l.getBlockY());
			}
		}
		if(shouldDie == 10) {
			Thread.yield();
		}
	}
}
