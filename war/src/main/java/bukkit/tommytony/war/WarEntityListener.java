package bukkit.tommytony.war;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageByProjectileEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerMoveEvent;

import com.tommytony.war.LootDropperTask;
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
	}
	
	public void onEntityDeath(EntityDeathEvent event) {
		Entity e = event.getEntity();
		if(e instanceof Player) {
			Player player = (Player)e;
			Team team = war.getPlayerTeam(player.getName());
			if(team != null) {
				Warzone zone =  war.getPlayerTeamWarzone(player.getName());
				handleDeath(player, zone, team);
//				if(zone.isDropLootOnDeath()) {
//					war.getServer().getScheduler().scheduleAsyncDelayedTask(war, 
//							new LootDropperTask(player.getLocation(), event.getDrops()), 
//							750);
//				}	
				event.getDrops().clear();	// no loot
			}
		}
    }
	
//	public void onEntityDamage(EntityDamageEvent event) {
////		Entity defender = event.getEntity();
////		
////		if(defender instanceof Player) {
////			Player d = (Player)defender;
////			if(event.getDamage() >= d.getHealth()) {
////				// Player died
////				Warzone defenderWarzone = war.getPlayerTeamWarzone(d.getName());
////				Team defenderTeam = war.getPlayerTeam(d.getName());
////				if(defenderTeam != null) {
////					
////					handleDeath(d, defenderWarzone, defenderTeam);
////					event.setCancelled(true); // Don't let the killing blow fall down.
////				}
////			}
////		}
//		
//    }
	
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
    	handlerAttackDefend(event);
    }
    
    private void handlerAttackDefend(EntityDamageByEntityEvent event) {
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
//				if(!){
//				// A real attack: handle death scenario. ==> now handled in entity damage as well
//				//synchronized(d) {
////					if(d.getHealth() <= 0) {
//////						// Player died, loot him!
//////						PlayerInventory attackerInv = a.getInventory();
//////						PlayerInventory defenderInv = d.getInventory();
//////						HashMap<Integer, ItemStack> noMorePlace = new HashMap<Integer, ItemStack>();
//////						for(ItemStack stack : defenderInv.getContents()) {
//////							HashMap<Integer, ItemStack> newNoMorePlace = attackerInv.addItem(stack);
//////							noMorePlace.putAll(newNoMorePlace);
//////						}					
//////						
//////						// attacker inventory is full, drop the rest.
//////						if(!noMorePlace.isEmpty()) {
//////							for(Integer key : noMorePlace.keySet()) {
//////								ItemStack toDrop = noMorePlace.get(key);
//////								defender.getWorld().dropItem(defender.getLocation(), toDrop);
//////							}
//////						}
////						
////						handleDeath(d, defenderWarzone, defenderTeam);
////						event.setCancelled(true);
//					//}
//				}
				if(defenderTeam.getSpawnVolume().contains(d.getLocation())) {	// attacking person in spawn
					war.badMsg(a, "Can't attack a player that's inside his team's spawn.");
					event.setCancelled(true);
				} else if(attackerTeam.getSpawnVolume().contains(a.getLocation()) && !attackerTeam.getSpawnVolume().contains(d.getLocation())) {
					// only let a player inside spawn attack an enemy player if that player enters the spawn
					war.badMsg(a, "Can't attack a player from inside your spawn.");
					event.setCancelled(true);
				}
				//}
			} else if (attackerTeam != null && defenderTeam != null 
					&& attackerTeam == defenderTeam 			
					&& attackerWarzone == defenderWarzone
					&& attacker.getEntityId() != defender.getEntityId()) {
				// same team, but not same person
				if(attackerWarzone.getFriendlyFire()) {
					war.badMsg(a, "Friendly fire is on! Please, don't hurt your teammates."); // if ff is on, let the attack go through
				} else {
					war.badMsg(a, "Your attack missed! Your target is on your team.");
					event.setCancelled(true);	// ff is off
				}
			} else if (attackerTeam == null && defenderTeam == null && !war.isPvpInZonesOnly()){
				// let normal PVP through is its not turned off
			} else if (attackerTeam == null && defenderTeam == null && war.isPvpInZonesOnly()) {
				war.badMsg(a, "Your attack missed! Global PVP is turned off. You can only attack other players in warzones. Try /warhub, /zones and /zone.");
				event.setCancelled(true);	// global pvp is off
			} else {
				war.badMsg(a, "Your attack missed!");
				if(attackerTeam == null) {
					war.badMsg(a, "You must join a team " +
						", then you'll be able to damage people " +
						"in the other teams in that warzone.");
				} else if (defenderTeam == null) {
					war.badMsg(a, "Your target is not in a team.");
				} else if (attackerTeam == defenderTeam) {
					war.badMsg(a, "Your target is on your team.");
				} else if (attackerWarzone != defenderWarzone) {
					war.badMsg(a, "Your target is playing in another warzone.");
				}
				event.setCancelled(true); // can't attack someone inside a warzone if you're not in a team
			}
			
//			if(event.isCancelled() && event instanceof EntityDamageByProjectileEvent) {
//				//((EntityDamageByProjectileEvent)event).setBounce(true);
//			}
		}
	}

	public void onEntityDamageByProjectile(EntityDamageByProjectileEvent event) {
    	handlerAttackDefend(event);
    }
	
	public void onEntityExplode(EntityExplodeEvent event) {
		// protect zones elements, lobbies and warhub from creepers
		List<Block> explodedBlocks = event.blockList();
		for(Block block : explodedBlocks) {
			if(war.getWarHub() != null && war.getWarHub().getVolume().contains(block)) {
				event.setCancelled(true);
				war.logInfo("Explosion prevented at warhub.");
				return;
			}
			for(Warzone zone : war.getWarzones()) {
				if(zone.isImportantBlock(block)) {
					event.setCancelled(true);
					war.logInfo("Explosion prevented in zone " + zone.getName() + ".");
					return;
				} else if (zone.getLobby() != null && zone.getLobby().getVolume().contains(block)) {
					event.setCancelled(true);
					war.logInfo("Explosion prevented at zone " + zone.getName() + " lobby.");
					return;
				}
			}
		}
    }
	
	private void handleDeath(Player player, Warzone playerWarzone, Team playerTeam) {
    	// teleport to team spawn upon death
		war.msg(player, "You died.");
		boolean newBattle = false;
		boolean scoreCapReached = false;
		//synchronized(playerWarzone) {
			//synchronized(player) {
				int remaining = playerTeam.getRemainingLifes();
				if(remaining == 0) { // your death caused your team to lose
					List<Team> teams = playerWarzone.getTeams();
					String scorers = "";
					for(Team t : teams) {
						t.teamcast("The battle is over. Team " + playerTeam.getName() + " lost: " 
								+ player.getName() + " died and there were no lives left in their life pool.");
						
						if(!t.getName().equals(playerTeam.getName())) {
							// all other teams get a point
							t.addPoint();
							t.resetSign();
							scorers += "Team " + t.getName() + " scores one point. ";
						}
					}
					if(!scorers.equals("")){
						for(Team t : teams) {
							t.teamcast(scorers);
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
						String winnersStr = "";
						for(Team winner : scoreCapTeams) {
							winnersStr += winner.getName() + " ";
						}
						if(playerWarzone.hasPlayerInventory(player.getName())){
							playerWarzone.restorePlayerInventory(player);
						}
						
						playerWarzone.handleScoreCapReached(player, winnersStr);
						player.teleportTo(playerWarzone.getTeleport());
						scoreCapReached = true;
					} else {
						// A new battle starts. Reset the zone but not the teams.
						for(Team t : teams) {
							t.teamcast("A new battle begins. Warzone reset.");
						}
						playerWarzone.getVolume().resetBlocks();
						playerWarzone.initializeZone();
						newBattle = true;
					}
				} else {
					// player died without causing his team's demise
					if(playerWarzone.isFlagThief(player.getName())) {
						// died while carrying flag.. dropped it
						Team victim = playerWarzone.getVictimTeamForThief(player.getName());
						victim.getFlagVolume().resetBlocks();
						victim.initializeTeamFlag();
						playerWarzone.removeThief(player.getName());
						for(Team t : playerWarzone.getTeams()) {
							t.teamcast(player.getName() + " died and dropped team " + victim.getName() + "'s flag.");
						}
					}
					playerTeam.setRemainingLives(remaining - 1);
					if(remaining - 1 == 0) {
						for(Team t : playerWarzone.getTeams()) {
							t.teamcast("Team " + t.getName() + "'s life pool is empty. One more death and they lose the battle!");
						}
					}
				}
			//}
		//}
		//synchronized(player) {
			if(!newBattle && !scoreCapReached) {
				playerTeam.resetSign();
				playerWarzone.respawnPlayer(playerTeam, player);
			} 
//			else if (scoreCapReached) {
//				
//				war.info(player.getName() + " died and enemy team reached score cap");
//			} else if (newBattle){
//				war.info(player.getName() + " died and battle ended in team " + playerTeam.getName() + "'s disfavor");
//			}
		//}
	}
	
	public void onEntityDamage(EntityDamageEvent event) {
		if(event.getCause() == DamageCause.FIRE_TICK) {
			Entity entity =  event.getEntity();
			if(entity instanceof Player) {
				Player player = (Player) entity;
				Team team = war.getPlayerTeam(player.getName());
				if(team != null && team.getSpawnVolume().contains(player.getLocation())) {
					// smother out the fire that didn't burn out when you respawned
					// Stop fire (upcast, watch out!)
					if(player instanceof CraftPlayer) {
						net.minecraft.server.Entity playerEntity = ((CraftPlayer)player).getHandle();
						playerEntity.fireTicks = 0;
//						playerEntity.r(); // force refresh (?)
					}
					event.setCancelled(true);		
				}
				
			}
			
		} else if (event.getCause() == DamageCause.DROWNING) {
			Entity entity =  event.getEntity();
			if(entity instanceof Player) {
				Player player = (Player) entity;
				Team team = war.getPlayerTeam(player.getName());
				if(team != null && player.getHealth() <= 0) {
					// don't keep killing drowing player: trying to stop "Player moved wrongly!" error at respawn.
					event.setCancelled(true);		
				}
				
			}
		}
    }

	public void onEntityCombust(EntityCombustEvent event) {
		Entity entity =  event.getEntity();
		if(entity instanceof Player) {
			Player player = (Player) entity;
			Team team = war.getPlayerTeam(player.getName());
			if(team != null && team.getSpawnVolume().contains(player.getLocation())) {
				// smother out the fire that didn't burn out when you respawned
				//Stop fire (upcast, watch out!)
				if(player instanceof CraftPlayer) {
					net.minecraft.server.Entity playerEntity = ((CraftPlayer)player).getHandle();
					playerEntity.fireTicks = 0;
//					playerEntity.r(); // force refresh (?)
				}
				event.setCancelled(true);		
			}
			
		}
    }

    
//    private void handleDeath(Player player, Warzone playerWarzone, Team playerTeam) {
//    	// teleport to team spawn upon death
//		player.sendMessage(war.str("You died."));
//		boolean newBattle = false;
//		boolean scoreCapReached = false;
//		synchronized(playerWarzone) {
//			synchronized(player) {
//				int remaining = playerTeam.getRemainingTickets();
//				if(remaining == 0) { // your death caused your team to lose
//					List<Team> teams = playerWarzone.getTeams();
//					for(Team t : teams) {
//						t.teamcast(war.str("The battle is over. Team " + playerTeam.getName() + " lost: " 
//								+ player.getName() + " died and there were no lives left in their life pool." ));
//						
//						if(!t.getName().equals(playerTeam.getName())) {
//							// all other teams get a point
//							t.addPoint();
//							t.resetSign();
//						}
//					}
//					// detect score cap
//					List<Team> scoreCapTeams = new ArrayList<Team>();
//					for(Team t : teams) {
//						if(t.getPoints() == playerWarzone.getScoreCap()) {
//							scoreCapTeams.add(t);
//						}
//					}
//					if(!scoreCapTeams.isEmpty()) {
//						String winnersStr = "Score cap reached! Winning team(s): ";
//						for(Team winner : scoreCapTeams) {
//							winnersStr += winner.getName() + " ";
//						}
//						winnersStr += ". The warzone is being reset... Please choose a new team.";
//						// Score cap reached. Reset everything.
//						for(Team t : teams) {
//							t.teamcast(war.str(winnersStr));
//							for(Player tp : t.getPlayers()) {
//								if(tp.getName() != player.getName()) {
//									tp.teleportTo(playerWarzone.getTeleport());
//								}
//							}
//							t.setPoints(0);
//							t.getPlayers().clear();	// empty the team
//						}
//						playerWarzone.getVolume().resetBlocks();
//						playerWarzone.initializeZone();
//						scoreCapReached = true;
//					} else {
//						// We can keep going
//						for(Team t : teams) {
//							t.teamcast(war.str("A new battle begins. The warzone is being reset..."));
//						}
//						playerWarzone.getVolume().resetBlocks();
//						playerWarzone.initializeZone();
//						newBattle = true;
//						playerTeam.setRemainingTickets(playerTeam.getRemainingTickets()); // TODO get rid of this dirty workaround for the twice move-on-death bug
//					}
//				} else {
//					playerTeam.setRemainingTickets(remaining - 1);
//				}
//			}
//		}
//		synchronized(player) {
//			if(!newBattle && !scoreCapReached) {
//				playerWarzone.respawnPlayer(playerTeam, player);
//				playerTeam.resetSign();
//				war.info(player.getName() + " died and was tp'd back to team " + playerTeam.getName() + "'s spawn");
//			} else if (scoreCapReached) { 
//				player.teleportTo(playerWarzone.getTeleport());
//				playerTeam.resetSign();
//				war.info(player.getName() + " died and enemy team reached score cap");
//			} else if (newBattle){
//				war.info(player.getName() + " died and battle ended in team " + playerTeam.getName() + "'s disfavor");
//			}
//		}
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
    //}
}
