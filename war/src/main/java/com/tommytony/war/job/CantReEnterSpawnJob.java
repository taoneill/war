package com.tommytony.war.job;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import com.tommytony.war.Team;
import com.tommytony.war.War;
import com.tommytony.war.utility.Direction;

public class CantReEnterSpawnJob implements Runnable {

	private static Random rand = new Random();
	private static final List<String> playersUnderSuspicion = new ArrayList<String>();
	private final Player player;
	private final Team playerTeam;

	public CantReEnterSpawnJob(Player player, Team playerTeam) {
		this.player = player;
		this.playerTeam = playerTeam;
		playersUnderSuspicion.add(player.getName());
	}
	
	@Override
	public void run() {
		Location playerLoc = this.player.getLocation();
		if (playerTeam.getSpawnVolume().contains(playerLoc)) { 
			int diffZ = playerLoc.getBlockZ() - playerTeam.getTeamSpawn().getBlockZ();
			int diffX = playerLoc.getBlockX() - playerTeam.getTeamSpawn().getBlockX();
			
			int finalZ = playerLoc.getBlockZ();
			int finalX = playerLoc.getBlockX();
			int bumpDistance = 1;
			if (diffZ == 0 && diffX == 0) {
				// at spawn already, get him moving
				finalZ += (minusOneZeroOrOne() * 2);
				finalX += (minusOneZeroOrOne() * 2);
			} else if (diffZ > 0 && diffX > 0) {
				finalZ += bumpDistance;
				finalX += bumpDistance;
			} else if (diffZ == 0 && diffX > 0) {
				finalX += bumpDistance;
			}else if (diffZ < 0 && diffX > 0) {
				finalZ -= bumpDistance;
				finalX += bumpDistance;
			} else if (diffZ < 0 && diffX == 0) {
				finalZ -= bumpDistance;
			} else if (diffZ > 0 && diffX < 0) {
				finalZ -= bumpDistance;
				finalX -= bumpDistance;
			} else if (diffZ == 0 && diffX < 0) {
				finalX -= bumpDistance;
			} else if (diffZ > 0 && diffX < 0) {
				finalZ += bumpDistance;
				finalX -= bumpDistance;
			} else if (diffZ > 0 && diffX == 0) {
				finalZ += bumpDistance;
			}
			
			Location nextCandidate = new Location(playerLoc.getWorld(),
					finalX,
					playerLoc.getY(),
					finalZ,
					playerLoc.getYaw(),
					playerLoc.getPitch()
			);
			
			Block nextCandidateBlock = nextCandidate.getWorld().getBlockAt(nextCandidate);
			int attempts = 0;
			// make sure this isn't the middle of a wall
			while (attempts < 32
					&& !playerTeam.getSpawnVolume().contains(nextCandidate)
					&& (attempts == 0 // fall through the first iteration because we may be still in spawn and with both air blocks 
							|| !nextCandidateBlock.getType().equals(Material.AIR) 
							|| !nextCandidateBlock.getRelative(BlockFace.UP).getType().equals(Material.AIR))) {
				// not air at destination, lets find somewhere nearby with air
				int zeroToSeven = rand.nextInt(8);
				int distanceAwayMultiplier = 3 + attempts/10;
				
				switch (zeroToSeven) {
					case 0: 
						nextCandidateBlock = playerTeam.getTeamSpawn().getBlock().getRelative(Direction.WEST(), distanceAwayMultiplier);
						break;
					case 1:
						nextCandidateBlock = playerTeam.getTeamSpawn().getBlock().getRelative(Direction.EAST(), distanceAwayMultiplier);
						break;
					case 2: 
						nextCandidateBlock = playerTeam.getTeamSpawn().getBlock().getRelative(Direction.NORTH(), distanceAwayMultiplier);
						break;
					case 3:	
						nextCandidateBlock = playerTeam.getTeamSpawn().getBlock().getRelative(Direction.SOUTH(), distanceAwayMultiplier);
						break;
					case 4: 
						nextCandidateBlock = playerTeam.getTeamSpawn().getBlock().getRelative(BlockFace.NORTH_WEST, distanceAwayMultiplier);
						break;
					case 5:
						nextCandidateBlock = playerTeam.getTeamSpawn().getBlock().getRelative(BlockFace.NORTH_EAST, distanceAwayMultiplier);
						break;
					case 6: 
						nextCandidateBlock = playerTeam.getTeamSpawn().getBlock().getRelative(BlockFace.SOUTH_WEST, distanceAwayMultiplier);
						break;
					default:	//7 
						nextCandidateBlock = playerTeam.getTeamSpawn().getBlock().getRelative(BlockFace.SOUTH_EAST, distanceAwayMultiplier);
						break;
				}
				
				nextCandidate = nextCandidateBlock.getLocation();

				attempts++;
			}
			
			if (attempts == 32) {
				// if random air search fails, go up!
				nextCandidateBlock = nextCandidate.getWorld().getHighestBlockAt(new Location(playerLoc.getWorld(),
						finalX,
						playerLoc.getY(),
						finalZ,
						playerLoc.getYaw(),
						playerLoc.getPitch()
				));
				nextCandidate = nextCandidateBlock.getLocation();
			}
			
			player.teleport(nextCandidate);
			
			War.war.badMsg(player, "Can't re-enter spawn!");
		}
		
		if (playersUnderSuspicion.contains(player.getName())) {
			playersUnderSuspicion.remove(player.getName());
		}
	}
	
	private int minusOneZeroOrOne() {
		int zeroToTwo = rand.nextInt(3);
		switch (zeroToTwo) {
			case 0: 
				return -1;
			case 1:
				return 0;
			default:
				return 1;
		}
	}

	public static List<String> getPlayersUnderSuspicion() {
		return playersUnderSuspicion;
	}
}
