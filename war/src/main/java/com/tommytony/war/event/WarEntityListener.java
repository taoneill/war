package com.tommytony.war.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.ContainerBlock;
import org.bukkit.block.NoteBlock;
import org.bukkit.block.Sign;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.entity.CraftTNTPrimed;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.player.SpoutPlayer;

import com.tommytony.war.Team;
import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import com.tommytony.war.config.TeamConfig;
import com.tommytony.war.config.WarConfig;
import com.tommytony.war.config.WarzoneConfig;
import com.tommytony.war.job.DeferredBlockResetsJob;
import com.tommytony.war.spout.SpoutMessenger;
import com.tommytony.war.structure.Bomb;
import com.tommytony.war.utility.DeferredBlockReset;

/**
 * Handles Entity-Events
 *
 * @author tommytony, Tim Düsterhus
 * @package bukkit.tommytony.war
 */
public class WarEntityListener implements Listener {

	private final Random killSeed = new Random();
			
	/**
	 * Handles PVP-Damage
	 *
	 * @param event
	 *                fired event
	 */
	private void handlerAttackDefend(EntityDamageByEntityEvent event) {
		Entity attacker = event.getDamager();
		Entity defender = event.getEntity();
		
		//DamageCause cause = event.getCause();
		//War.war.log(cause.toString(), Level.INFO);
		// Maybe an arrow was thrown
		if (attacker != null && event.getDamager() instanceof Projectile && ((Projectile)event.getDamager()).getShooter() instanceof Player){
			attacker = ((Player)((Projectile)event.getDamager()).getShooter());
		}

		if (attacker != null && defender != null && attacker instanceof Player && defender instanceof Player) {
			// only let adversaries (same warzone, different team) attack each other
			Player a = (Player) attacker;
			Player d = (Player) defender;
			Warzone attackerWarzone = Warzone.getZoneByPlayerName(a.getName());
			Team attackerTeam = Team.getTeamByPlayerName(a.getName());
			Warzone defenderWarzone = Warzone.getZoneByPlayerName(d.getName());
			Team defenderTeam = Team.getTeamByPlayerName(d.getName());
			
			if ((attackerTeam != null && defenderTeam != null && attackerTeam != defenderTeam && attackerWarzone == defenderWarzone)
					|| (attackerTeam != null && defenderTeam != null && attacker.getEntityId() == defender.getEntityId())) {
				// Make sure one of the players isn't in the spawn
				if (defenderTeam.getSpawnVolume().contains(d.getLocation())) { // attacking person in spawn
					if (!defenderWarzone.isFlagThief(d.getName()) 
							&& !defenderWarzone.isBombThief(d.getName())) { // thieves can always be attacked
						War.war.badMsg(a, "Can't attack a player that's inside his team's spawn.");
						event.setCancelled(true);
						return;
					}
				} else if (attackerTeam.getSpawnVolume().contains(a.getLocation()) && !attackerTeam.getSpawnVolume().contains(d.getLocation())) {
					// only let a player inside spawn attack an enemy player if that player enters the spawn
					if (!attackerWarzone.isFlagThief(a.getName())
							&& !defenderWarzone.isBombThief(d.getName())) { // thieves can always attack
						War.war.badMsg(a, "Can't attack a player from inside your spawn.");
						event.setCancelled(true);
						return;
					}
				// Make sure none of them are respawning
				} else if (defenderWarzone.isRespawning(d)) {
					War.war.badMsg(a, "The target is currently respawning!");
					event.setCancelled(true);
					return;
				} else if (attackerWarzone.isRespawning(a)) {
					War.war.badMsg(a, "You can't attack while respawning!");
					event.setCancelled(true);
					return;
				}
				
				if (!attackerWarzone.getWarzoneConfig().getBoolean(WarzoneConfig.PVPINZONE)) {
					// spleef-like, non-pvp, zone
					event.setCancelled(true);
					return;
				}
				
				if (attackerTeam != null && defenderTeam != null && attacker.getEntityId() == defender.getEntityId()) {
					War.war.badMsg(a, "You hit yourself!");
				}

				// Detect death, prevent it and respawn the player
				if (event.getDamage() >= d.getHealth()) {
					if (defenderWarzone.getReallyDeadFighters().contains(d.getName())) {
						// don't re-kill a dead person 
						event.setCancelled(true);
						return;
					}
					
					if (attackerWarzone.getWarzoneConfig().getBoolean(WarzoneConfig.DEATHMESSAGES)) {
						String killMessage = "";
						String attackerString = attackerTeam.getKind().getColor() + a.getName();
						String defenderString = defenderTeam.getKind().getColor() + d.getName();
						
						if (attacker.getEntityId() != defender.getEntityId()) {
							Material killerWeapon = a.getItemInHand().getType();
							String weaponString = killerWeapon.toString();
							if (killerWeapon == Material.AIR) {
								weaponString = "hand";
							} else if (killerWeapon == Material.BOW || event.getDamager() instanceof Arrow) {
								int rand = killSeed.nextInt(3);
								if (rand == 0) {
									weaponString = "arrow";
								} else if (rand == 1) {
									weaponString = "bow";
								} else {
									weaponString = "aim";
								}
								
							} else if (event.getDamager() instanceof Projectile) {
								weaponString = "aim";
							}
							
							String adjectiveString = War.war.getDeadlyAdjectives().get(this.killSeed.nextInt(War.war.getDeadlyAdjectives().size()));
							String verbString = War.war.getKillerVerbs().get(this.killSeed.nextInt(War.war.getKillerVerbs().size()));
							
							killMessage = attackerString + ChatColor.WHITE + "'s " + adjectiveString + weaponString.toLowerCase().replace('_', ' ') 
													+ " " + verbString + " " + defenderString;
						} else {
							killMessage = defenderString + ChatColor.WHITE + " committed accidental suicide";
						}
						
						for (Team team : defenderWarzone.getTeams()) {
							team.teamcast(killMessage);
						}
					}
					
					defenderWarzone.handleDeath(d);
					
					if (!defenderWarzone.getWarzoneConfig().getBoolean(WarzoneConfig.REALDEATHS)) {
						// fast respawn, don't really die
						event.setCancelled(true);
						return;
					}
					
				} else if (defenderWarzone.isBombThief(d.getName()) && d.getLocation().distance(a.getLocation()) < 2) {
					// Close combat, close enough to detonate					
					Bomb bomb = defenderWarzone.getBombForThief(d.getName());
										
					// Kill the bomber 
					defenderWarzone.handleDeath(d);
					
					if (defenderWarzone.getWarzoneConfig().getBoolean(WarzoneConfig.REALDEATHS)) {
						// and respawn him and remove from deadmen (cause realdeath + handleDeath means no respawn and getting queued up for onPlayerRespawn)
						defenderWarzone.getReallyDeadFighters().remove(d.getName());
						defenderWarzone.respawnPlayer(defenderTeam, d);
					}
					
					// Blow up bomb
					defenderWarzone.getWorld().createExplosion(a.getLocation(), 2F);
															
					// Notify everyone
					for (Team t : defenderWarzone.getTeams()) {
						if (War.war.isSpoutServer()) {
							for (Player p : t.getPlayers()) {
								SpoutPlayer sp = SpoutManager.getPlayer(p);
								if (sp.isSpoutCraftEnabled()) {
					                sp.sendNotification(
					                		SpoutMessenger.cleanForNotification(attackerTeam.getKind().getColor() + a.getName() + ChatColor.YELLOW + " made "),
					                		SpoutMessenger.cleanForNotification(defenderTeam.getKind().getColor() + d.getName() + ChatColor.YELLOW + " blow up!"),
					                		Material.TNT,
					                		(short)0,
					                		10000);
								}
							}
						}
						
						// bring back tnt
						bomb.getVolume().resetBlocks();
						bomb.addBombBlocks();
						
						t.teamcast(attackerTeam.getKind().getColor() + a.getName() + ChatColor.WHITE
								+ " made " + defenderTeam.getKind().getColor() + d.getName() + ChatColor.WHITE + " blow up!");
					}
				}
			} else if (attackerTeam != null && defenderTeam != null && attackerTeam == defenderTeam && attackerWarzone == defenderWarzone && attacker.getEntityId() != defender.getEntityId()) {
				// same team, but not same person
				if (attackerWarzone.getWarzoneConfig().getBoolean(WarzoneConfig.FRIENDLYFIRE)) {
					War.war.badMsg(a, "Friendly fire is on! Please, don't hurt your teammates."); // if ff is on, let the attack go through
				} else {
					War.war.badMsg(a, "Your attack missed! Your target is on your team.");
					event.setCancelled(true); // ff is off
				}
			} else if (attackerTeam == null && defenderTeam == null && War.war.canPvpOutsideZones(a)) {
				// let normal PVP through is its not turned off or if you have perms
			} else if (attackerTeam == null && defenderTeam == null && !War.war.canPvpOutsideZones(a)) {
				if (!War.war.getWarConfig().getBoolean(WarConfig.DISABLEPVPMESSAGE)) {
					War.war.badMsg(a, "You need the 'war.pvp' permission to attack players outside warzones.");
				}
				event.setCancelled(true); // global pvp is off
			} else {
				War.war.badMsg(a, "Your attack missed!");
				if (attackerTeam == null) {
					War.war.badMsg(a, "You must join a team, then you'll be able to damage people " + "in the other teams in that warzone.");
				} else if (defenderTeam == null) {
					War.war.badMsg(a, "Your target is not in a team.");
				} else if (attacker != null && defender != null && attacker.getEntityId() == defender.getEntityId()) {
					// You just hit yourself, probably with a bouncing arrow
				} else if (attackerTeam == defenderTeam) {
					War.war.badMsg(a, "Your target is on your team.");
				} else if (attackerWarzone != defenderWarzone) {
					War.war.badMsg(a, "Your target is playing in another warzone.");
				}
				event.setCancelled(true); // can't attack someone inside a warzone if you're not in a team
			}
		} else if (defender instanceof Player) {
			// attacked by dispenser arrow most probably
			// Detect death, prevent it and respawn the player
			Player d = (Player) defender;
			Warzone defenderWarzone = Warzone.getZoneByPlayerName(d.getName());
			if (d != null && defenderWarzone != null && event.getDamage() >= d.getHealth()) {
				if (defenderWarzone.getReallyDeadFighters().contains(d.getName())) {
					// don't re-kill a dead person 
					event.setCancelled(true);
					return;
				}
								
				if (defenderWarzone.getWarzoneConfig().getBoolean(WarzoneConfig.DEATHMESSAGES)) {
					String deathMessage = "";
					String defenderString = Team.getTeamByPlayerName(d.getName()).getKind().getColor() + d.getName();
					
					if (event.getDamager() instanceof CraftTNTPrimed) {
						deathMessage = defenderString + ChatColor.WHITE + " exploded";
					} else {
						deathMessage = defenderString + ChatColor.WHITE + " died";
					}
					for (Team team : defenderWarzone.getTeams()) {
						team.teamcast(deathMessage);
					}
				}
				
				defenderWarzone.handleDeath(d);
				
				if (!defenderWarzone.getWarzoneConfig().getBoolean(WarzoneConfig.REALDEATHS)) {
					// fast respawn, don't really die
					event.setCancelled(true);
					return;
				}
			}
		}
	}

	/**
	 * Protects important structures from explosions
	 *
	 * @see EntityListener.onEntityExplode()
	 */
	@EventHandler(event = EntityExplodeEvent.class)
	public void onEntityExplode(final EntityExplodeEvent event) {
		if (!War.war.isLoaded()) {
			return;
		}
		// protect zones elements, lobbies and warhub from creepers and tnt
		List<Block> explodedBlocks = event.blockList();
		List<Block> dontExplode = new ArrayList<Block>();
		
		boolean explosionInAWarzone = event.getEntity() != null && Warzone.getZoneByLocation(event.getEntity().getLocation()) != null;
		
		if (!explosionInAWarzone && War.war.getWarConfig().getBoolean(WarConfig.TNTINZONESONLY) && event.getEntity() instanceof TNTPrimed) {
			// if tntinzonesonly:true, no tnt blows up outside zones
			event.setCancelled(true);
			return;
		}
		
		for (Block block : explodedBlocks) {
			if (War.war.getWarHub() != null && War.war.getWarHub().getVolume().contains(block)) {
				dontExplode.add(block);
			} else {
				boolean inOneZone = false;
				for (Warzone zone : War.war.getWarzones()) {
					if (zone.isImportantBlock(block)) {
						dontExplode.add(block);
						if (zone.isBombBlock(block)) {
							// tnt doesn't get reset like normal blocks, gotta schedule a later reset just for the Bomb
							// structure's tnt block
							DeferredBlockResetsJob job = new DeferredBlockResetsJob(block.getWorld());
							job.add(new DeferredBlockReset(block.getX(), block.getY(), block.getZ(), Material.TNT.getId(), (byte)0));
							War.war.getServer().getScheduler().scheduleSyncDelayedTask(War.war, job, 10);
						}
						inOneZone = true;
						break;
					} else if (zone.getLobby() != null && zone.getLobby().getVolume().contains(block)) {
						dontExplode.add(block);
						inOneZone = true;
						break;
					} else if (zone.getVolume().contains(block)) {
						inOneZone = true;
					}
				}
				
				if (!inOneZone && explosionInAWarzone) {
					// if the explosion originated in warzone, always rollback
					dontExplode.add(block);
				}
			}
		}
		
		int dontExplodeSize = dontExplode.size();
		if (dontExplode.size() > 0) {
			// Reset the exploded blocks that shouldn't have exploded (some of these are zone artifacts, if rollbackexplosion some may be outside-of-zone blocks 
			DeferredBlockResetsJob job = new DeferredBlockResetsJob(dontExplode.get(0).getWorld());
			List<Block> doors = new ArrayList<Block>(); 
			for (Block dont : dontExplode) {
				DeferredBlockReset deferred = null;
				if (dont.getState() instanceof Sign) {
					String[] lines = ((Sign)dont.getState()).getLines();
					deferred = new DeferredBlockReset(dont.getX(), dont.getY(), dont.getZ(), dont.getTypeId(), dont.getData(), lines);
				} else if (dont.getState() instanceof ContainerBlock) {
					ItemStack[] contents = ((ContainerBlock)dont.getState()).getInventory().getContents();
					Block worldBlock = dont.getWorld().getBlockAt(dont.getLocation());
					if (worldBlock.getState() instanceof ContainerBlock) {
						((ContainerBlock)worldBlock.getState()).getInventory().clear();
					}
					deferred = new DeferredBlockReset(dont.getX(), dont.getY(), dont.getZ(), dont.getTypeId(), dont.getData(), copyItems(contents));
				} else if (dont.getTypeId() == Material.NOTE_BLOCK.getId()) {
					Block worldBlock = dont.getWorld().getBlockAt(dont.getLocation());
					if (worldBlock.getState() instanceof NoteBlock) {
						NoteBlock noteBlock = ((NoteBlock)worldBlock.getState());
						if (noteBlock != null) {
							deferred = new DeferredBlockReset(dont.getX(), dont.getY(), dont.getZ(), dont.getTypeId(), dont.getData(), noteBlock.getRawNote());
						}
					}
				} else if (dont.getTypeId() != Material.TNT.getId()) {				
					deferred = new DeferredBlockReset(dont.getX(), dont.getY(), dont.getZ(), dont.getTypeId(), dont.getData());
					if (dont.getTypeId() == Material.WOODEN_DOOR.getId() || dont.getTypeId() == Material.IRON_DOOR_BLOCK.getId()) {
						doors.add(dont);
					}
				}
				if (deferred != null) {
					job.add(deferred);
				}
			}
			War.war.getServer().getScheduler().scheduleSyncDelayedTask(War.war, job);
			
			// Changed explosion yield following proportion of explosion prevention (makes drops less buggy too) 
			int explodedSize = explodedBlocks.size();
			float middleYeild = (float)(explodedSize - dontExplodeSize) / (float)explodedSize;
			float newYeild = middleYeild * event.getYield();
			
			event.setYield(newYeild);
		}
	}

	private List<ItemStack> copyItems(ItemStack[] contents) {
		List<ItemStack> list = new ArrayList<ItemStack>();
		for (ItemStack stack : contents) {
			if (stack != null) {
				if (stack.getData() != null) {
					list.add(new ItemStack(stack.getType(), stack.getAmount(), stack.getDurability(), stack.getData().getData()));
				} else {
					list.add(new ItemStack(stack.getType(), stack.getAmount(), stack.getDurability()));
				}
			}
		}
		return list;
	}

	/**
	 * Handles damage on Players
	 *
	 * @see EntityListener.onEntityDamage()
	 */
	@EventHandler(event = EntityDamageEvent.class)
	public void onEntityDamage(final EntityDamageEvent event) {
		if (!War.war.isLoaded()) {
			return;
		}

		Entity entity = event.getEntity();
		if (!(entity instanceof Player)) {
			return;
		}
		Player player = (Player) entity;

		// prevent godmode
		if (Warzone.getZoneByPlayerName(player.getName()) != null) {
			event.setCancelled(false);
		}

		// pass pvp-damage
		if (event instanceof EntityDamageByEntityEvent) {
			this.handlerAttackDefend((EntityDamageByEntityEvent) event);
		} else  {
			Warzone zone = Warzone.getZoneByPlayerName(player.getName());
			
			if (zone != null && event.getDamage() >= player.getHealth()) {
				if (zone.getReallyDeadFighters().contains(player.getName())) {
					// don't re-kill a dead person 
					event.setCancelled(true);
					return;
				}
				
				// Detect death, prevent it and respawn the player
				if (zone.getWarzoneConfig().getBoolean(WarzoneConfig.DEATHMESSAGES)) {
					String deathMessage = "";
					String cause = " died";
					if (event.getCause() == DamageCause.FIRE || event.getCause() == DamageCause.FIRE_TICK 
							|| event.getCause() == DamageCause.LAVA || event.getCause() == DamageCause.LIGHTNING) {
						cause = " burned to a crisp";
					} else if (event.getCause() == DamageCause.DROWNING) {
						cause = " drowned";
					} else if (event.getCause() == DamageCause.FALL) {
						cause = " fell to an untimely death";
					}
					deathMessage = Team.getTeamByPlayerName(player.getName()).getKind().getColor() + player.getName() + ChatColor.WHITE + cause;
					for (Team team : zone.getTeams()) {
						team.teamcast(deathMessage);
					}
				}
				
				zone.handleDeath(player);
				
				if (!zone.getWarzoneConfig().getBoolean(WarzoneConfig.REALDEATHS)) {
					// fast respawn, don't really die
					event.setCancelled(true);
				}
			}
		}
	}

	@EventHandler(event = EntityDamageEvent.class)
	public void onEntityCombust(final EntityDamageEvent event) {
		if (!War.war.isLoaded()) {
			return;
		}
		Entity entity = event.getEntity();
		if (entity instanceof Player) {
			Player player = (Player) entity;
			Team team = Team.getTeamByPlayerName(player.getName());
			if (team != null && team.getSpawnVolume().contains(player.getLocation())) {
				// smother out the fire that didn't burn out when you respawned
				// Stop fire (upcast, watch out!)
				if (player instanceof CraftPlayer) {
					net.minecraft.server.Entity playerEntity = ((CraftPlayer) player).getHandle();
					playerEntity.fireTicks = 0;
				}
				event.setCancelled(true);
			}
		}
	}

	/**
	 * Prevents creatures from spawning in warzones if no creatures is active
	 *
	 * @see EntityListener.onCreatureSpawn()
	 */
	@EventHandler(event = CreatureSpawnEvent.class)
	public void onCreatureSpawn(final CreatureSpawnEvent event) {
		if (!War.war.isLoaded()) {
			return;
		}

		Location location = event.getLocation();
		Warzone zone = Warzone.getZoneByLocation(location);
		if (zone != null && zone.getWarzoneConfig().getBoolean(WarzoneConfig.NOCREATURES)) {
			event.setCancelled(true);
		}
	}

	/**
	 * Prevents health regaining caused by peaceful mode
	 *
	 * @see EntityListener.onEntityRegainHealth()
	 */
	@EventHandler(event = EntityRegainHealthEvent.class)
	public void onEntityRegainHealth(final EntityRegainHealthEvent event) {
		if (!War.war.isLoaded() || 
				(event.getRegainReason() != RegainReason.REGEN 
						&& event.getRegainReason() != RegainReason.EATING 
						&& event.getRegainReason() != RegainReason.SATIATED)) {
			return;
		}

		Entity entity = event.getEntity();
		if (!(entity instanceof Player)) {
			return;
		}

		Player player = (Player) entity;
		Warzone zone = Warzone.getZoneByPlayerName(player.getName());
		if (zone != null) {
			Team team = Team.getTeamByPlayerName(player.getName());
			if ((event.getRegainReason() == RegainReason.EATING 
					|| event.getRegainReason() != RegainReason.SATIATED ) 
				&& team.getTeamConfig().resolveBoolean(TeamConfig.NOHUNGER)) {
				// noHunger setting means you can't auto-heal with full hunger bar (use saturation instead to control how fast you get hungry)
				event.setCancelled(true);
			} else if (event.getRegainReason() == RegainReason.REGEN) {
				// disable peaceful mode regen
				event.setCancelled(true);
			}
		}
	}
	
	@EventHandler(event = FoodLevelChangeEvent.class)
	public void onFoodLevelChange(final FoodLevelChangeEvent event) {
		if (!War.war.isLoaded() || !(event.getEntity() instanceof Player)) {
			return;
		}
		
		Player player = (Player) event.getEntity();
		Warzone zone = Warzone.getZoneByPlayerName(player.getName());
		Team team = Team.getTeamByPlayerName(player.getName());
		if (zone != null && team.getTeamConfig().resolveBoolean(TeamConfig.NOHUNGER)){
			event.setCancelled(true);
		}
	}
	
	@EventHandler(event = EntityDeathEvent.class)
	public void onEntityDeath(final EntityDeathEvent event) {
		if (!War.war.isLoaded() || !(event.getEntity() instanceof Player)) {
			return;
		}
		
		Player player = (Player) event.getEntity();
		Warzone zone = Warzone.getZoneByPlayerName(player.getName());
		if (zone != null) {
			event.getDrops().clear();
			if (!zone.getWarzoneConfig().getBoolean(WarzoneConfig.REALDEATHS)) {
				// catch the odd death that gets away from us when usually intercepting and preventing deaths
				zone.handleDeath(player);
			}
			
			if (zone.getWarzoneConfig().getBoolean(WarzoneConfig.DEATHMESSAGES)) {
				for (Team team : zone.getTeams()) {
					team.teamcast(player.getName() + " died");
				}
			}
		}
	}
	
	@EventHandler(event = ExplosionPrimeEvent.class)
    public void onExplosionPrime(final ExplosionPrimeEvent event) {
		if (!War.war.isLoaded()) {
			return;
		}
		
		Location eventLocation = event.getEntity().getLocation();
		
		for (Warzone zone : War.war.getWarzones()) {
			if (zone.isBombBlock(eventLocation.getBlock())) {
				// prevent the Bomb from exploding on its pedestral
				event.setCancelled(true);
				return;
			}
		}
	}

}
