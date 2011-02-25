package bukkit.tommytony.war;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageByProjectileEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerMoveEvent;

import com.tommytony.war.Team;
import com.tommytony.war.Warzone;
import com.tommytony.war.jobs.LootDropperTask;

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
				zone.handleDeath(player);
//				if(zone.isDropLootOnDeath()) {
//					war.getServer().getScheduler().scheduleAsyncDelayedTask(war, 
//							new LootDropperTask(player.getLocation(), event.getDrops()), 
//							750);
//				}	
				event.getDrops().clear();	// no loot
			}
		}
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
	
	public void onEntityDamage(EntityDamageEvent event) {
		if(event instanceof EntityDamageByEntityEvent || 
				event instanceof EntityDamageByProjectileEvent) {
			handlerAttackDefend((EntityDamageByEntityEvent)event);
		} else if(event.getCause() == DamageCause.FIRE_TICK) {
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
	
	public void onCreatureSpawn(CreatureSpawnEvent event) {
		Location location = event.getLocation();
		Warzone zone = war.warzone(location);
		if(zone != null && zone.isNoCreatures()) {
			event.setCancelled(true);
			war.logInfo("Prevented " + event.getMobType().getName() + " from spawning in zone " + zone.getName());
		}
    }

}
