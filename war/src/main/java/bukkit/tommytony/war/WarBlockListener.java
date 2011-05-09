package bukkit.tommytony.war;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.block.BlockBreakEvent;
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
    	if(player != null && block != null) {
			Team team = war.getPlayerTeam(player.getName()); 
			Warzone zone = war.warzone(player.getLocation());
			if(team != null && block != null && zone != null 
					&& zone.isMonumentCenterBlock(block)
					&& block.getType() == team.getKind().getMaterial()
					&& block.getData() == team.getKind().getData()) {
				Monument monument = zone.getMonumentFromCenterBlock(block);
				if(monument != null && !monument.hasOwner()) {
					monument.capture(team);
					List<Team> teams = zone.getTeams();
					for(Team t : teams) {
						t.teamcast("Monument " + monument.getName() + " has been captured by team " + team.getName() + ".");
					}
					event.setCancelled(false);
					return; // important otherwise cancelled down a few line by isImportantblock
				} else {
					war.badMsg(player, "You can't capture a monument without a block of your team's material. Get one from your team spawn.");
					event.setCancelled(true);
					return;
				}
			}
			
			if(zone != null && zone.isImportantBlock(block)){
				war.badMsg(player, "Can't build here.");
				event.setCancelled(true);
				return;
			}
			// protect warzone lobbies
	    	for(Warzone wz: war.getWarzones()) {
	    		if(wz.getLobby() != null && wz.getLobby().getVolume().contains(block)) {
	    			war.badMsg(player, "Can't build here.");
		    		event.setCancelled(true);
		    		return;
	    		}
	    	}
	    	// protect the hub
	    	if(war.getWarHub() != null && war.getWarHub().getVolume().contains(block)) {
	    		war.badMsg(player, "Can't build here.");
	    		event.setCancelled(true);
	    		return;
	    	}
	    	
	    	// buildInZonesOnly
	    	if(zone == null && war.isBuildInZonesOnly() && !war.canBuildOutsideZone(player)) {
	    		war.badMsg(player, "You can only build inside warzones. Ask for the 'war.build' permission to build outside.");
	    		event.setCancelled(true);
	    		return;
	    	}
	    	
	    	// can't place a block of your team's color
	    	if(team != null && block.getType() == team.getKind().getMaterial()
					&& block.getData() == team.getKind().getData()) {
	    		war.badMsg(player, "You can only use your team's blocks to capture monuments.");
	    		event.setCancelled(true);
	    		return;
	    	}
	    	
	    	if(team != null && zone != null && zone.isFlagThief(player.getName())) {
				// a flag thief can't drop his flag
				war.badMsg(player, "Can't drop the flag. What are you doing? Run!");
				event.setCancelled(true);
				
			}
	    	
	    	boolean isZoneMaker = war.isZoneMaker(player);
	    	// unbreakableZoneBlocks
	    	if(zone != null && zone.isUnbreakableZoneBlocks() 
	    			&& (!isZoneMaker
	    					|| (isZoneMaker && team != null)) 
	    					) {
	    		// if the zone is unbreakable, no one but zone makers can break blocks (even then, zone makers in a team can't break blocks)
	    		war.badMsg(player, "The blocks in this zone are unbreakable - this also means you can't build!");
	    		event.setCancelled(true);
	    		return;
	    	}
		}
    }
	
	public void onBlockBreak(BlockBreakEvent event) {
		Player player = event.getPlayer();
    	Block block = event.getBlock();
    	if(player != null && block != null) {
    		handleBreakOrDamage(player, block, event);
    	}
    }
	
//    public void onBlockDamage(BlockDamageEvent event) {
//    	Player player = event.getPlayer();
//    	Block block = event.getBlock();
//    	if(player != null && block != null && event.getDamageLevel() == BlockDamageLevel.BROKEN) {
//    		handleBreakOrDamage(player,block, event);
//	    	
//    	}
//    }

	private void handleBreakOrDamage(Player player, Block block, Cancellable event) {
		Warzone warzone = war.warzone(player.getLocation());
    	Team team = war.getPlayerTeam(player.getName());
    	boolean isZoneMaker = war.isZoneMaker(player);
    	
    	if(warzone != null && team == null && !isZoneMaker) {
    		// can't actually destroy blocks in a warzone if not part of a team
    		war.badMsg(player, "Can't destroy part of a warzone if you're not in a team.");
    		event.setCancelled(true);
    		return;
    	} else if(team != null && block != null && warzone != null 
				&& warzone.isMonumentCenterBlock(block)){
			Monument monument = warzone.getMonumentFromCenterBlock(block);
			if(monument.hasOwner()) {

				List<Team> teams = warzone.getTeams();
				for(Team t : teams) {
					t.teamcast("Team " + monument.getOwnerTeam().getName() + " loses control of monument " + monument.getName());
				}
				monument.uncapture();
			}
			event.setCancelled(false);
			return;
		}else if(warzone != null && warzone.isImportantBlock(block)) {
			
    		if(team != null && team.getSpawnVolume().contains(block)) {
    			ItemStack teamKindBlock = new ItemStack(team.getKind().getMaterial(), team.getKind().getData());
    			if(player.getInventory().contains(teamKindBlock)) {
    				war.badMsg(player, "You already have a " + team.getName() + " block.");
    				event.setCancelled(true);
    				return;
    			} else {
    				event.setCancelled(false);		// very important, otherwise could get cancelled but unbreakableZoneBlocks further down
    				return;
    			}
    			// let team members loot one block the spawn for monument captures
    		} else if (team != null && warzone.isEnemyTeamFlagBlock(team, block)) {
    			if(warzone.isFlagThief(player.getName())) {
    				// detect audacious thieves
    				war.badMsg(player, "You can only steal one flag at a time!");
    			} else {
	    			Team lostFlagTeam = warzone.getTeamForFlagBlock(block);
	    			if (lostFlagTeam.getPlayers().size() != 0) {
	    				// player just broke the flag block of other team: cancel to avoid drop, give player the block, set block to air
		    			ItemStack teamKindBlock = new ItemStack(lostFlagTeam.getKind().getMaterial(), 1, (short)1, new Byte(lostFlagTeam.getKind().getData()));
		    			player.getInventory().clear();
		    			player.getInventory().addItem(teamKindBlock);
		    			warzone.addFlagThief(lostFlagTeam, player.getName());
		    			block.setType(Material.AIR);
		    			
		    			for(Team t : warzone.getTeams()) {
							t.teamcast(player.getName() + " stole team " + lostFlagTeam.getName() + "'s flag.");
							if(t.getName().equals(lostFlagTeam.getName())){
								t.teamcast("Prevent " + player.getName() + " from reaching team " + team.getName() + "'s spawn or flag.");
							}
						}
		    			war.msg(player, "You have team " + lostFlagTeam.getName() + "'s flag. Reach your team spawn or flag to capture it!");
	    			} else {
	    				war.msg(player, "You can't steal team " + lostFlagTeam.getName() + "'s flag since there are no players there.");
	    			}
    			}
    			event.setCancelled(true);
    			return;
    		} else if (!warzone.isMonumentCenterBlock(block)){
	    		war.badMsg(player, "Can't destroy this.");
	    		event.setCancelled(true);
	    		return;
    		}
    	}
    	
    	// protect warzone lobbies
    	if(block != null) {
	    	for(Warzone zone: war.getWarzones()) {
	    		if(zone.getLobby() != null && 
	    				zone.getLobby().getVolume().contains(block)) {
	    			war.badMsg(player, "Can't destroy this.");
		    		event.setCancelled(true);
		    		return;
	    		}
	    	}
    	}
    	
    	// protect the hub
    	if(war.getWarHub() != null && war.getWarHub().getVolume().contains(block)) {
    		war.badMsg(player, "Can't destroy this.");
    		event.setCancelled(true);
    		return;
    	}
    	
    	// buildInZonesOnly
    	Warzone blockZone = war.warzone(new Location(block.getWorld(), block.getX(), block.getY(), block.getZ()));
    	if(blockZone == null 
    			&& war.isBuildInZonesOnly() 
    			&& !war.canBuildOutsideZone(player)) {
    		war.badMsg(player, "You can only build inside warzones. Ask for the 'war.build' permission to build outside.");
    		event.setCancelled(true);
    		return;
    	}
    	
    	// unbreakableZoneBlocks
    	if(blockZone != null && blockZone.isUnbreakableZoneBlocks() 
    			&& (!isZoneMaker
    					|| (isZoneMaker && team != null)) 
    					) {
    		// if the zone is unbreakable, no one but zone makers can break blocks (even then, zone makers in a team can't break blocks
    		war.badMsg(player, "The blocks in this zone are unbreakable!");
    		event.setCancelled(true);
    		return;
    	}
	}
}
