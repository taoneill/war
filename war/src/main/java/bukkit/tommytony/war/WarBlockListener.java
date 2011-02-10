package bukkit.tommytony.war;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockDamageLevel;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import com.tommytony.war.Monument;
import com.tommytony.war.Team;
import com.tommytony.war.Warzone;

/**
 * 
 * @author tommytony
 *
 */
public class WarBlockListener extends BlockListener {

	private War war;

	public WarBlockListener(War war) {
		this.war = war;
	}
	
	public void onBlockPlace(BlockPlaceEvent event) {
		Player player = event.getPlayer();
    	Block block = event.getBlock();
    	boolean captured = false;
    	if(player != null && block != null) {
			Team team = war.getPlayerTeam(player.getName()); 
			Warzone zone = war.warzone(player.getLocation());
			if(team != null && block != null && zone != null 
					&& zone.isMonumentCenterBlock(block)
					&& block.getType() == team.getMaterial()) {
				Monument monument = zone.getMonumentFromCenterBlock(block);
				if(monument != null && !monument.hasOwner()) {
					monument.capture(team);
					List<Team> teams = zone.getTeams();
					for(Team t : teams) {
						t.teamcast(war.str("Monument " + monument.getName() + " has been captured by team " + team.getName() + "."));
					}
					
					return; // important otherwise cancelled down a few line by isImportantblock
				} else {
					player.sendMessage(war.bad("You can't capture a monument without a block of your team's material. Get one from your team spawn."));
					event.setCancelled(true);
					return;
				}
			}
			
			if(zone != null && zone.isImportantBlock(block)){
				player.sendMessage(war.bad("Can't build here."));
				event.setCancelled(true);
				return;
			}
			// protect warzone lobbies
	    	for(Warzone wz: war.getWarzones()) {
	    		if(wz.getLobby() != null && wz.getLobby().getVolume().contains(block)) {
	    			player.sendMessage(war.bad("Can't build here."));
		    		event.setCancelled(true);
		    		return;
	    		}
	    	}
	    	// protect the hub
	    	if(war.getWarHub() != null && war.getWarHub().getVolume().contains(block)) {
	    		player.sendMessage(war.bad("Can't build here."));
	    		event.setCancelled(true);
	    		return;
	    	}
		}
    }
	
    public void onBlockDamage(BlockDamageEvent event) {
    	Player player = event.getPlayer();
    	Block block = event.getBlock();
    	if(player != null && block != null && event.getDamageLevel() == BlockDamageLevel.BROKEN) {
	    	Warzone warzone = war.warzone(player.getLocation());
	    	Team team = war.getPlayerTeam(player.getName());
	    	boolean isZoneMaker = war.isZoneMaker(player);
	    	
	    	if(warzone != null && war.getPlayerTeam(player.getName()) == null && !isZoneMaker) {
	    		// can't actually destroy blocks in a warzone if not part of a team
	    		player.sendMessage(war.bad("Can't destroy part of a warzone if you're not in a team."));
	    		event.setCancelled(true);
	    		return;
	    	} else if(team != null && block != null && warzone != null 
					&& warzone.isMonumentCenterBlock(block)){
				Monument monument = warzone.getMonumentFromCenterBlock(block);
				if(monument.hasOwner()) {

					List<Team> teams = warzone.getTeams();
					for(Team t : teams) {
						t.teamcast(war.str("Team " + monument.getOwnerTeam().getName() + " loses control of monument " + monument.getName()));
					}
					monument.uncapture();
				}
				return;
			}else if(warzone != null && warzone.isImportantBlock(block)) {
	    		if(team != null && team.getSpawnVolume().contains(block)) {
	    			if(player.getInventory().contains(team.getMaterial())) {
	    				player.sendMessage(war.bad("You already have a " + team.getName() + " block."));
	    				event.setCancelled(true);
	    				return;
	    			}
	    			// let team members loot one block the spawn for monument captures
	    		} else if (team != null && warzone.isEnemyTeamFlagBlock(team, block)) {
	    			if(warzone.isFlagThief(player.getName())) {
	    				// detect audacious thieves
	    				player.sendMessage(war.bad("You can only steal one flag at a time!"));
	    			} else {
		    			// player just broke the flag block of other team: cancel to avoid drop, give player the block, set block to air
		    			Team lostFlagTeam = warzone.getTeamForFlagBlock(block);
		    			player.getInventory().clear();
		    			player.getInventory().addItem(new ItemStack(lostFlagTeam.getMaterial(), 1));
		    			warzone.addFlagThief(lostFlagTeam, player.getName());
		    			block.setType(Material.AIR);
		    			
		    			for(Team t : warzone.getTeams()) {
							t.teamcast(war.str(player.getName() + " stole team " + lostFlagTeam.getName() + "'s flag."));
							if(t.getName().equals(lostFlagTeam.getName())){
								t.teamcast(war.str("Prevent " + player.getName() + " from reaching team " + team.getName() + "'s spawn or flag."));
							}
						}
		    			player.sendMessage(war.str("You have team " + lostFlagTeam.getName() + "'s flag. Reach your team spawn or flag to capture it!"));
	    			}
	    			event.setCancelled(true);
	    			return;
	    		} else if (!warzone.isMonumentCenterBlock(block)){
		    		player.sendMessage(war.bad("Can't destroy this."));
		    		event.setCancelled(true);
		    		return;
	    		}
	    	} 
	    	
	    	// protect warzone lobbies
	    	if(block != null) {
		    	for(Warzone zone: war.getWarzones()) {
		    		if(zone.getLobby() != null && 
		    				zone.getLobby().getVolume().contains(block)) {
		    			player.sendMessage(war.bad("Can't destroy this."));
			    		event.setCancelled(true);
			    		return;
		    		}
		    	}
	    	}
	    	
	    	// protect the hub
	    	if(war.getWarHub() != null && war.getWarHub().getVolume().contains(block)) {
	    		player.sendMessage(war.bad("Can't destroy this."));
	    		event.setCancelled(true);
	    		return;
	    	}
    	}
    }
}
