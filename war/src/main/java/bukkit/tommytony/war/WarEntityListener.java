package bukkit.tommytony.war;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageByProjectileEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.tommytony.war.Team;
import com.tommytony.war.Warzone;

/**
 * 
 * @author tommytony
 *
 */
public class WarEntityListener extends EntityListener {

	private final War war;

	public WarEntityListener(War war) {
		this.war = war;
		// TODO Auto-generated constructor stub
	}
	
	public void onEntityDamage(EntityDamageEvent event) {
		Entity defender = event.getEntity();
		
		if(defender instanceof Player) {
			Player d = (Player)defender;
			if(event.getDamage() >= d.getHealth()) {
				// Player died
				Warzone defenderWarzone = war.getPlayerTeamWarzone(d.getName());
				Team defenderTeam = war.getPlayerTeam(d.getName());
				if(defenderTeam != null) {
					
					handleDeath(d, defenderWarzone, defenderTeam);
					event.setCancelled(true); // Don't let the killing blow fall down.
				}
			}
		}
		
    }
	
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
    	Entity attacker = event.getDamager();
    	Entity defender = event.getEntity();
    	
    	if(attacker != null && defender != null && attacker instanceof Player && defender instanceof Player) {
			// only let adversaries (same warzone, different team) attack each other
			Player a = (Player)attacker;
			Player d = (Player)defender;
			Warzone attackerWarzone = war.getPlayerTeamWarzone(a.getName());
			Team attackerTeam = war.getPlayerTeam(a.getName());
			Warzone defenderWarzone = war.getPlayerTeamWarzone(d.getName());
			Team defenderTeam = war.getPlayerTeam(d.getName());
			if(attackerTeam != null && defenderTeam != null 
					&& attackerTeam != defenderTeam 			
					&& attackerWarzone == defenderWarzone) {
				// Make sure one of the players isn't in the spawn
				if(!defenderTeam.getVolume().contains(d.getLocation())){
				// A real attack: handle death scenario. ==> now handled in entity damage as well
				//synchronized(d) {
					if(d.getHealth() <= 0) {
//						// Player died, loot him!
//						PlayerInventory attackerInv = a.getInventory();
//						PlayerInventory defenderInv = d.getInventory();
//						HashMap<Integer, ItemStack> noMorePlace = new HashMap<Integer, ItemStack>();
//						for(ItemStack stack : defenderInv.getContents()) {
//							HashMap<Integer, ItemStack> newNoMorePlace = attackerInv.addItem(stack);
//							noMorePlace.putAll(newNoMorePlace);
//						}					
//						
//						// attacker inventory is full, drop the rest.
//						if(!noMorePlace.isEmpty()) {
//							for(Integer key : noMorePlace.keySet()) {
//								ItemStack toDrop = noMorePlace.get(key);
//								defender.getWorld().dropItem(defender.getLocation(), toDrop);
//							}
//						}
						
						handleDeath(d, defenderWarzone, defenderTeam);
						event.setCancelled(true);
					}
				}
				else {	// attacking person in spawn
					a.sendMessage(war.str("Can't attack a player that's inside his team's spawn."));
					event.setCancelled(true);
				}
				//}
			} else if (attackerTeam != null && defenderTeam != null 
					&& attackerTeam == defenderTeam 			
					&& attackerWarzone == defenderWarzone) {
				// same team
				if(attackerWarzone.getFriendlyFire()) {
					a.sendMessage(war.str("Friendly fire is on! Please, don't hurt your teammates.")); // if ff is on, let the attack go through
				} else {
					a.sendMessage(war.str("Your attack missed!"));
					a.sendMessage(war.str("Your target is on your team."));
					event.setCancelled(true);	// ff is off
				}
			} else if (attackerTeam == null && defenderTeam == null && !war.isPvpInZonesOnly()){
				// let normal PVP through is its not turned off
			} else if (attackerTeam == null && defenderTeam == null && war.isPvpInZonesOnly()) {
				a.sendMessage(war.str("Your attack missed! Global PVP is turned off. You can only attack other players in warzones. Try /warhub, /zones and /zone."));
				event.setCancelled(true);	// global pvp is off
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
    
    public void onEntityDamageByProjectile(EntityDamageByProjectileEvent event) {
    }
    
    private void handleDeath(Player player, Warzone playerWarzone, Team playerTeam) {
    	// teleport to team spawn upon death
		player.sendMessage(war.str("You died."));
		boolean newBattle = false;
		boolean scoreCapReached = false;
		synchronized(playerWarzone) {
			synchronized(player) {
				int remaining = playerTeam.getRemainingTickets();
				if(remaining == 0) { // your death caused your team to lose
					List<Team> teams = playerWarzone.getTeams();
					for(Team t : teams) {
						t.teamcast(war.str("The battle is over. Team " + playerTeam.getName() + " lost: " 
								+ player.getName() + " died and there were no lives left in their life pool." ));
						
						if(!t.getName().equals(playerTeam.getName())) {
							// all other teams get a point
							t.addPoint();
							t.resetSign();
						}
					}
					// detect score cap
					List<Team> scoreCapTeams = new ArrayList<Team>();
					for(Team t : teams) {
						if(t.getPoints() == playerWarzone.getScoreCap()) {
							scoreCapTeams.add(t);
						}
					}
					if(!scoreCapTeams.isEmpty()) {
						String winnersStr = "Score cap reached! Winning team(s): ";
						for(Team winner : scoreCapTeams) {
							winnersStr += winner.getName() + " ";
						}
						winnersStr += ". The warzone is being reset... Please choose a new team.";
						// Score cap reached. Reset everything.
						for(Team t : teams) {
							t.teamcast(war.str(winnersStr));
							for(Player tp : t.getPlayers()) {
								if(tp.getName() != player.getName()) {
									tp.teleportTo(playerWarzone.getTeleport());
								}
							}
							t.setPoints(0);
							t.getPlayers().clear();	// empty the team
						}
						playerWarzone.getVolume().resetBlocks();
						playerWarzone.initializeZone();
						scoreCapReached = true;
					} else {
						// We can keep going
						for(Team t : teams) {
							t.teamcast(war.str("A new battle begins. The warzone is being reset..."));
						}
						playerWarzone.getVolume().resetBlocks();
						playerWarzone.initializeZone();
						newBattle = true;
						playerTeam.setRemainingTickets(playerTeam.getRemainingTickets()); // TODO get rid of this dirty workaround for the twice move-on-death bug
					}
				} else {
					playerTeam.setRemainingTickets(remaining - 1);
				}
			}
		}
		synchronized(player) {
			if(!newBattle && !scoreCapReached) {
				playerWarzone.respawnPlayer(playerTeam, player);
				playerTeam.resetSign();
				war.info(player.getName() + " died and was tp'd back to team " + playerTeam.getName() + "'s spawn");
			} else if (scoreCapReached) { 
				player.teleportTo(playerWarzone.getTeleport());
				playerTeam.resetSign();
				war.info(player.getName() + " died and enemy team reached score cap");
			} else if (newBattle){
				war.info(player.getName() + " died and battle ended in team " + playerTeam.getName() + "'s disfavor");
			}
		}
    	// old
//		Team team = war.getPlayerTeam(player.getName());
//		if(team != null){
//			// teleport to team spawn upon death
//			Warzone zone = war.warzone(player.getLocation());
//			boolean roundOver = false;
//			synchronized(zone) {
//				int remaining = team.getRemainingTickets();
//				if(remaining == 0) { // your death caused your team to lose
//					List<Team> teams = zone.getTeams();
//					for(Team t : teams) {
//						t.teamcast(war.str("The battle is over. Team " + team.getName() + " lost: " 
//								+ player.getName() + " hit the bottom of their life pool." ));
//						t.teamcast(war.str("A new battle begins. The warzone is being reset..."));
//						if(!t.getName().equals(team.getName())) {
//							// all other teams get a point
//							t.addPoint();
//							t.resetSign();
//						}
//					}
//					zone.endRound();
//					zone.getVolume().resetBlocks();
//					zone.initializeZone();
//					roundOver = true;
//				} else {
//					team.setRemainingTickets(remaining - 1);
//				}
//			}
//			if(!roundOver) {
//				zone.respawnPlayer(team, player);
//				player.sendMessage(war.str("You died!"));
//				team.resetSign();
//				war.getLogger().log(Level.INFO, player.getName() + " died and was tp'd back to team " + team.getName() + "'s spawn");
//			} else {
//				war.getLogger().log(Level.INFO, player.getName() + " died and battle ended in team " + team.getName() + "'s disfavor");
//			}
//		}
    }
}
