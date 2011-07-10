package bukkit.tommytony.war;

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
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
//import org.bukkit.event.entity.EntityRegainHealthEvent;

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
		if(war.isLoaded()) {
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
				if(defenderTeam.getSpawnVolume().contains(d.getLocation())) {	// attacking person in spawn
					if(!defenderWarzone.isFlagThief(d.getName())) { // thiefs can always be attacked
						war.badMsg(a, "Can't attack a player that's inside his team's spawn.");
						event.setCancelled(true);
					}
				} else if(attackerTeam.getSpawnVolume().contains(a.getLocation()) && !attackerTeam.getSpawnVolume().contains(d.getLocation())) {
					// only let a player inside spawn attack an enemy player if that player enters the spawn
					if(!attackerWarzone.isFlagThief(a.getName())) { // thiefs can always attack
						war.badMsg(a, "Can't attack a player from inside your spawn.");
						event.setCancelled(true);
					}
				}
				
				// Detect death, prevent it and respawn the player
				if(event.getDamage() >= d.getHealth()) {
					defenderWarzone.handleDeath(d);
					if(war.getServer().getPluginManager().getPlugin("HeroicDeath") != null) {
						
					}
					event.setCancelled(true);
				}
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
			} else if (attackerTeam == null && defenderTeam == null && (!war.isPvpInZonesOnly() || a.getLocation().getWorld().getName().equals("pvp"))){
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
				} else if (attacker != null && defender != null && attacker.getEntityId() == defender.getEntityId()) {
					// You just hit yourself, probably with a bouncing arrow
				} else if (attackerTeam == defenderTeam) {
					war.badMsg(a, "Your target is on your team.");
				} else if (attackerWarzone != defenderWarzone) {
					war.badMsg(a, "Your target is playing in another warzone.");
				}
				event.setCancelled(true); // can't attack someone inside a warzone if you're not in a team
			}
			
		} else if (defender instanceof Player && event instanceof EntityDamageByProjectileEvent) {
			// attacked by dispenser arrow most probably
			// Detect death, prevent it and respawn the player
			Player d = (Player) defender;
			Warzone defenderWarzone = war.getPlayerTeamWarzone(d.getName());
			if(d != null && defenderWarzone != null && event.getDamage() >= d.getHealth()) {
				defenderWarzone.handleDeath(d);
				event.setCancelled(true);
			}
		}
	}

	public void onEntityExplode(EntityExplodeEvent event) {
		if(war.isLoaded()) {
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
	}
	
	public void onEntityDamage(EntityDamageEvent event) {
		if(war.isLoaded()) {
			Entity entity = event.getEntity();
			if(entity instanceof Player && war.getPlayerTeamWarzone(((Player) entity).getName()) != null) {
				event.setCancelled(false);
			}

			if(event instanceof EntityDamageByEntityEvent || 
					event instanceof EntityDamageByProjectileEvent) {
				handlerAttackDefend((EntityDamageByEntityEvent)event);
			} else {
				// Detect death (from , prevent it and respawn the player
				if(entity instanceof Player) {
					Player player = (Player) entity;
					Warzone zone = war.getPlayerTeamWarzone(player.getName());
					if(zone != null && event.getDamage() >= player.getHealth()) {
						zone.handleDeath(player);
						event.setCancelled(true);
					}
				}
			}
		}
	}

	public void onEntityCombust(EntityCombustEvent event) {
		if(war.isLoaded()) {
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
					}
					event.setCancelled(true);		
				}
			}
		}
	}
	
	public void onCreatureSpawn(CreatureSpawnEvent event) {
		if(war.isLoaded()) {
			Location location = event.getLocation();
			Warzone zone = war.warzone(location);
			if(zone != null && zone.isNoCreatures()) {
				event.setCancelled(true);
				//war.logInfo("Prevented " + event.getMobType().getName() + " from spawning in zone " + zone.getName());
			}
		}
	}
	
	public void onEntityRegainHealth(EntityRegainHealthEvent event) {
		if(war.isLoaded() && event.getRegainReason() == RegainReason.REGEN) {
			Entity entity = event.getEntity();
			if(entity instanceof Player) {
				Player player = (Player) entity;
				Location location = player.getLocation();
				Warzone zone = war.warzone(location);
				if(zone != null) {
					if (((CraftPlayer) player).getHandle().ticksLived % 20 * 12 == 0) {
						event.setCancelled(true);
					}
				}
			}
		}
	}
}
