package bukkit.tommytony.war;

import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.block.BlockDamageLevel;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPlaceEvent;

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
    	if(player != null) {
			Team team = war.getPlayerTeam(player.getName()); 
			Warzone zone = war.getPlayerTeamWarzone(player.getName());
			boolean isZoneMaker = war.isZoneMaker(player);
			if(team != null && block != null && zone != null 
					&& zone.isMonumentCenterBlock(block)
					&& block.getType() == team.getMaterial()) {
				Monument monument = zone.getMonumentFromCenterBlock(block);
				if(!monument.hasOwner()) {
					monument.capture(team);
					List<Team> teams = zone.getTeams();
					for(Team t : teams) {
						t.teamcast(war.str("Monument " + monument.getName() + " has been captured by team " + team.getName() + "."));
					}
					
					captured = true;	// important otherwise cancelled down a few line by isImportantblock
				} else {
					player.sendMessage(war.str("You can't capture a monument without a block of your team's material. Get one from your team spawn."));
					event.setCancelled(true);
				}
			}
			if(!captured) {
				if(zone != null && zone.isImportantBlock(block) && !isZoneMaker){
					player.sendMessage(war.str("Can't build here."));
					event.setCancelled(true);
				}
				// protect warzone lobbies
		    	if(block != null) {
			    	for(Warzone wz: war.getWarzones()) {
			    		if(wz.getLobby() != null && wz.getLobby().getVolume().contains(block) && !isZoneMaker) {
			    			player.sendMessage(war.str("Can't build here."));
				    		event.setCancelled(true);
			    		}
			    	}
		    	}
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
	    		player.sendMessage(war.str("Can't destroy part of a warzone if you're not in a team."));
	    		event.setCancelled(true);
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
			}else if(warzone != null && warzone.isImportantBlock(block) && !isZoneMaker) {
	    		if(team != null && team.getVolume().contains(block)) {
	    			if(player.getInventory().contains(team.getMaterial())) {
	    				player.sendMessage(war.str("You already have a " + team.getName() + " block."));
	    				event.setCancelled(true);
	    			}
	    			// let team members loot one block the spawn for monument captures
	    		} else if (!warzone.isMonumentCenterBlock(block)){
		    		player.sendMessage(war.str("Can't destroy this."));
		    		event.setCancelled(true);
	    		}
	    	} 
	    	
	    	// protect warzone lobbies
	    	if(block != null) {
		    	for(Warzone zone: war.getWarzones()) {
		    		if(zone.getLobby().getVolume().contains(block)) {
		    			player.sendMessage(war.str("Can't destroy this."));
			    		event.setCancelled(true);
		    		}
		    	}
	    	}
    	}
    }
}
