package com.tommytony.war;

import java.util.List;

import org.bukkit.Block;
import org.bukkit.Material;
import org.bukkit.Player;
import org.bukkit.event.block.BlockBrokenEvent;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPlacedEvent;

public class WarBlockListener extends BlockListener {

	private War war;

	public WarBlockListener(War war) {
		this.war = war;
	}
	
	public void onBlockPlaced(BlockPlacedEvent event) {
		Player player = event.getPlayer();
    	Block block = event.getBlock();
    	if(player != null) {
			Team team = war.getPlayerTeam(player.getName()); 
			Warzone zone = war.getPlayerWarzone(player.getName());
			if(team != null && block != null && zone != null 
					&& zone.isMonumentCenterBlock(block)
					&& (block.getType() == Material.YellowFlower || block.getType() == Material.RedRose || block.getType() == Material.Sapling)) {
				Monument monument = zone.getMonumentFromCenterBlock(block);
				if(!monument.hasOwner()) {
					monument.ignite(team);
					List<Team> teams = zone.getTeams();
					for(Team t : teams) {
						t.teamcast(war.str("Monument " + monument.getName() + " has been ignited by team " + team.getName() + "."));
					}
				} else {
					player.sendMessage(war.str("Monument must be smothered first."));
				}
			}
		}
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
	    	
	    	Team team = war.getPlayerTeam(player.getName());
	    	if(team != null && block != null && warzone != null 
					&& warzone.isMonumentCenterBlock(block)){
	    		Monument monument = warzone.getMonumentFromCenterBlock(block);
	    		if(monument.hasOwner()) {
					monument.smother();
					List<Team> teams = warzone.getTeams();
					for(Team t : teams) {
						t.teamcast(war.str("Monument " + monument.getName() + " has been smothered."));
					}
				}
	    	}
    	}
    }

    public void onBlockCanBuild(BlockCanBuildEvent event) {
    	// BUKKIT
//    	Block blockPlaced = event.getBlock();
//    	
//    	Warzone warzone = war.warzone(new Location(warblockPlaced.getX());
//    	if(warzone != null) {
//    		if(warzone.isImportantBlock(blockPlaced) || warzone.isImportantBlock(blockClicked)) {
//    			event.setCancelled(true);
//    		}
//    	}
    }

    public void onBlockFlow(BlockFromToEvent event) {
//    	Block block = null;
//    	Block blockTo = event.getBlock();
//    	Block blockFrom = event.getFromBlock();
//		if(blockTo != null) {
//			block = blockTo;
//		} else if (blockFrom != null) {
//			block = blockFrom;
//		}
//		
//		if(block != null) {
//			Warzone zone = war.warzone(new Location(block.getWorld(), block.getX(), block.getY(), block.getZ()));
//			if(zone != null && 
//					((blockTo != null && zone.isMonumentCenterBlock(blockTo)
//						|| (blockFrom != null && zone.isMonumentCenterBlock(blockFrom))))) {
//				Monument monument = null;
//				if(blockTo != null) monument = zone.getMonumentFromCenterBlock(blockTo);
//				if(monument == null && blockFrom != null) monument = zone.getMonumentFromCenterBlock(blockFrom);
//				if(monument.hasOwner()) {
//					monument.setOwnerTeam(null);
//					List<Team> teams = zone.getTeams();
//					for(Team team : teams) {
//						team.teamcast(war.str("Monument " + monument.getName() + " has been smothered."));
//					}
//				}
//			}
//		}
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
