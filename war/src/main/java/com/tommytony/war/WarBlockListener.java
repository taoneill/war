package com.tommytony.war;

import java.util.List;

import org.bukkit.Block;
import org.bukkit.Location;
import org.bukkit.Player;
import org.bukkit.event.block.BlockBrokenEvent;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockListener;

public class WarBlockListener extends BlockListener {

	private War war;

	public WarBlockListener(War war) {
		this.war = war;
	}
	
    public void onBlockBroken(BlockBrokenEvent event) {
    	Player player = event.getPlayer();
    	Block block = event.getBlock();
    	if(player != null && block != null) {
	    	Warzone warzone = war.warzone(player.getLocation());
	    	if(warzone != null && war.getPlayerTeam(player.getName()) == null) {
	    		// can't actually destroy blocks in a warzone if not part of a team
	    		player.sendMessage(war.str("Can't destroy part of a warzone if you're not in a team."));
	    		
// 				BUKKIT
//	    		event.setCancelled(true);
	    	}
	    	
	    	if(warzone != null && warzone.isImportantBlock(block)) {
	    		player.sendMessage(war.str("Can't destroy this."));
// 				BUKKIT
//	    		event.setCancelled(true);
	    	}
    	}
    }

    public void onBlockCanBuild(BlockCanBuildEvent event) {
    	// BUKKIT
    	// Player player = event.getPlayer();
//    	Block blockPlaced = event.getBlock();
//    	Warzone warzone = war.warzone(player.getLocation());
//    	if(warzone != null) {
//    		if(warzone.isImportantBlock(blockPlaced) || warzone.isImportantBlock(blockClicked)) {
//    			player.sendMessage(war.str("Can't build here."));
//    			event.setCancelled(true);
//    		}
//    	}
    }

    public void onBlockFlow(BlockFromToEvent event) {
    	Block block = null;
    	Block blockTo = event.getBlock();
    	Block blockFrom = event.getFromBlock();
		if(blockTo != null) {
			block = blockTo;
		} else if (blockFrom != null) {
			block = blockFrom;
		}
		
		if(block != null) {
			Warzone zone = war.warzone(new Location(block.getWorld(), block.getX(), block.getY(), block.getZ()));
			if(zone != null && 
					((blockTo != null && zone.isMonumentFirestone(blockTo)
						|| (blockFrom != null && zone.isMonumentFirestone(blockFrom))))) {
				Monument monument = null;
				if(blockTo != null) monument = zone.getMonumentForFirestone(blockTo);
				if(monument == null && blockFrom != null) monument = zone.getMonumentForFirestone(blockFrom);
				if(monument.hasOwner()) {
					monument.setOwnerTeam(null);
					List<Team> teams = zone.getTeams();
					for(Team team : teams) {
						team.teamcast(war.str("Monument " + monument.getName() + " has been smothered."));
					}
				}
			}
		}
    }

    public void onBlockIgnite(BlockIgniteEvent event) {
//		BUKKIT    	
//    	Player player = event.getPlayer();
//    	Block block = event.getBlock();
//    	if(player != null) {
//			Team team = war.getPlayerTeam(player.getName()); 
//			Warzone zone = war.getPlayerWarzone(player.getName());
//			if(team != null && block != null && zone != null && zone.isMonumentFirestone(block)) {
//				Monument monument = zone.getMonumentForFirestone(block);
//				if(!monument.hasOwner()) {
//					monument.ignite(team);
//					List<Team> teams = zone.getTeams();
//					for(Team t : teams) {
//						t.teamcast(war.str("Monument " + monument.getName() + " has been ignited by team " + team.getName() + "."));
//					}
//				} else {
//					player.sendMessage(war.str("Monument must be smothered first."));
//				}
//			}
//		}
    }
}
