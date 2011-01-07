package com.tommytony.war;

import java.util.List;
import java.util.logging.Level;

import org.bukkit.Entity;
import org.bukkit.Player;
import org.bukkit.event.entity.EntityDamagedByBlockEvent;
import org.bukkit.event.entity.EntityDamagedByEntityEvent;
import org.bukkit.event.entity.EntityListener;

public class WarEntityListener extends EntityListener {

	private final War war;

	public WarEntityListener(War war) {
		this.war = war;
		// TODO Auto-generated constructor stub
	}
	
	public void onEntityDamagedByBlock(EntityDamagedByBlockEvent event) {
		Entity damaged = event.getEntity();
		
		if(damaged != null && damaged instanceof Player){  
			Player player = (Player)damaged;
			if(event.getDamage() >= player.getHealth()) {
				Warzone zone = war.warzone(player.getLocation());
				if(war.getPlayerTeam(player.getName()) != null) {
					// player on a team killed himself
					handleDeath(((Player)damaged));
					
				} else if (zone != null ) {
					player.teleportTo(zone.getTeleport());
				}
				event.setCancelled(true);	// Don't let the block totally kill the player
			}
		}
			
    }

    public void onEntityDamagedByEntity(EntityDamagedByEntityEvent event) {
    	Entity attacker = event.getDamager();
    	Entity defender = event.getEntity();
    	
    	if(attacker != null && defender != null && attacker instanceof Player && defender instanceof Player) {
			// only let adversaries (same warzone, different team) attack each other
			Player a = (Player)attacker;
			Player d = (Player)defender;
			Warzone attackerWarzone = war.getPlayerWarzone(a.getName());
			Team attackerTeam = war.getPlayerTeam(a.getName());
			Warzone defenderWarzone = war.getPlayerWarzone(d.getName());
			Team defenderTeam = war.getPlayerTeam(d.getName());
			if(attackerTeam != null && defenderTeam != null 
					&& attackerTeam != defenderTeam 			
					&& attackerWarzone == defenderWarzone) {
				// A real attack: handle death scenario.
				if(event.getDamage() >= d.getHealth()) {
					// Player died
					handleDeath(d);
					event.setCancelled(true); // Don't let the killing blow fall down.
				}
				
				war.getLogger().log(Level.INFO, a.getName() + " hit " + d.getName() + " for " + event.getDamage());
			} else if (attackerTeam != null && defenderTeam != null 
					&& attackerTeam == defenderTeam 			
					&& attackerWarzone == defenderWarzone) {
				// same team
				if(attackerWarzone.getFriendlyFire()) {
					a.sendMessage(war.str("Friendly fire!")); // if ff is on, let the attack go through
				} else {
					a.sendMessage(war.str("Your attack missed!"));
					a.sendMessage(war.str("Your target is on your team."));
					event.setCancelled(true);	// ff is off
				}
			} else if (attackerTeam == null && defenderTeam == null){
				// normal PVP
			} else {
				a.sendMessage(war.str("Your attack missed!"));
				if(attackerTeam == null) {
					a.sendMessage(war.str(" You must join a team " +
						", then you'll be able to damage people " +
						"in the other teams in that warzone."));
				} else if (defenderTeam == null) {
					a.sendMessage(war.str("Your target is not in a team."));
				} else if (attackerTeam == defenderTeam) {
					a.sendMessage(war.str("Your target is on your team."));
				} else if (attackerWarzone != defenderWarzone) {
					a.sendMessage(war.str("Your target is playing in another warzone."));
				}
				event.setCancelled(true); // can't attack someone inside a warzone if you're not in a team
			}
		}
    }
    
    private void handleDeath(Player player) {
		Team team = war.getPlayerTeam(player.getName());
		if(team != null){
			// teleport to team spawn upon death
			Warzone zone = war.warzone(player.getLocation());
			boolean roundOver = false;
			synchronized(zone) {
				int remaining = team.getRemainingTickets();
				if(remaining == 0) { // your death caused your team to lose
					List<Team> teams = zone.getTeams();
					for(Team t : teams) {
						t.teamcast(war.str("The battle is over. Team " + team.getName() + " lost: " 
								+ player.getName() + " hit the bottom of their life pool." ));
						t.teamcast(war.str("A new battle begins. The warzone is being reset..."));
						if(!t.getName().equals(team.getName())) {
							// all other teams get a point
							t.addPoint();
							zone.resetSign(t);
						}
					}
					zone.resetState();
					roundOver = true;
				} else {
					team.setRemainingTickets(remaining - 1);
				}
			}
			if(!roundOver) {
				zone.respawnPlayer(team, player);
				player.sendMessage(war.str("You died!"));
				zone.resetSign(team);
				war.getLogger().log(Level.INFO, player.getName() + " died and was tp'd back to team " + team.getName() + "'s spawn");
			} else {
				war.getLogger().log(Level.INFO, player.getName() + " died and battle ended in team " + team.getName() + "'s disfavor");
			}
		}
    }
}
