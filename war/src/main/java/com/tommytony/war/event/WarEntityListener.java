package com.tommytony.war.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.NoteBlock;
import org.bukkit.block.Sign;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.player.SpoutPlayer;

import com.tommytony.war.Team;
import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import com.tommytony.war.config.TeamConfig;
import com.tommytony.war.config.WarConfig;
import com.tommytony.war.config.WarzoneConfig;
import com.tommytony.war.job.CheckWolfsJob;
import com.tommytony.war.job.DeferredBlockResetsJob;
import com.tommytony.war.spout.SpoutDisplayer;
import com.tommytony.war.structure.Bomb;
import com.tommytony.war.utility.DeferredBlockReset;
import com.tommytony.war.utility.KillstreakMetadata;
import com.tommytony.war.utility.LoadoutSelection;
import com.tommytony.war.utility.PlayerStatTracker;

/**
 * Handles Entity-Events
 *
 * @author tommytony, Tim Düsterhus, grinning
 * @package com.tommytony.war.event
 */
public class WarEntityListener implements Listener {

	private final Random killSeed = new Random();
	private List<Egg> eggsForExplosion = new ArrayList<Egg>();
	private Map<Player, Player> lastDamager = new HashMap<Player, Player>();
			
	/**
	 * Handles PVP-Damage
	 *
	 * @param event
	 *                fired event
	 */
	private void handlerAttackDefend(EntityDamageByEntityEvent event) {
		Entity attacker = event.getDamager();
		Entity defender = event.getEntity();
		
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
			attackerWarzone.updateLastDamager(d, a);
			
			if ((attackerTeam != null && defenderTeam != null && attackerTeam != defenderTeam && attackerWarzone == defenderWarzone)
					|| (attackerTeam != null && defenderTeam != null && attacker.getEntityId() == defender.getEntityId())) {
				
				LoadoutSelection defenderLoadoutState = defenderWarzone.getLoadoutSelections().get(d.getName());
				if (defenderLoadoutState != null && defenderLoadoutState.isStillInSpawn()) {
					War.war.badMsg(a, "The target is still in spawn!");
					event.setCancelled(true);
					return;
				}
				
				LoadoutSelection attackerLoadoutState = attackerWarzone.getLoadoutSelections().get(a.getName());
				if (attackerLoadoutState != null && attackerLoadoutState.isStillInSpawn()) {
					War.war.badMsg(a, "You can't attack while still in spawn!");
					event.setCancelled(true);
					return;
				}
				
				// Make sure none of them are locked in by respawn timer
				if (defenderWarzone.isRespawning(d)) {
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
						if (d.getHealth() != 0) {
							d.setHealth(0);
						}
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
						
						
							if((a.getName().contains("Zacraft210")) || (a.getName().contains("grinning")) || (a.getName().contains("tommytony"))) {
								killMessage = attackerTeam.getKind().getColor() + a.getName() + ChatColor.WHITE + " transmitted AIDS to " + defenderTeam.getKind().getColor() +
										d.getName();
							}
						
						for (Team team : defenderWarzone.getTeams()) {
							team.teamcast(killMessage);
						}
					}
					
					//death stuff
					attackerTeam.incKills(a);
					defenderTeam.zeroKills(d);
					//now check for kill streaks
					this.checkKillStreak(a, attackerTeam);
					
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
						PlayerStatTracker.getStats(d).incDeaths();
						defenderWarzone.respawnPlayer(defenderTeam, d);
					}
					
					// Blow up bomb
					if (!defenderWarzone.getWarzoneConfig().getBoolean(WarzoneConfig.UNBREAKABLE)) {
						defenderWarzone.getWorld().createExplosion(a.getLocation(), 2F);
					}

					// bring back tnt
					bomb.getVolume().resetBlocks();
					bomb.addBombBlocks();
					
					// Notify everyone
					for (Team t : defenderWarzone.getTeams()) {
						if (War.war.isSpoutServer()) {
							for (Player p : t.getPlayers()) {
								SpoutPlayer sp = SpoutManager.getPlayer(p);
								if (sp.isSpoutCraftEnabled()) {
					                sp.sendNotification(
					                		SpoutDisplayer.cleanForNotification(attackerTeam.getKind().getColor() + a.getName() + ChatColor.YELLOW + " made "),
					                		SpoutDisplayer.cleanForNotification(defenderTeam.getKind().getColor() + d.getName() + ChatColor.YELLOW + " blow up!"),
					                		Material.TNT,
					                		(short)0,
					                		10000);
								}
							}
						}
						
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
					if (d.getHealth() != 0) {
						d.setHealth(0);
					}
					return;
				}
								
				if (defenderWarzone.getWarzoneConfig().getBoolean(WarzoneConfig.DEATHMESSAGES)) {
					String deathMessage = "";
					String defenderString = Team.getTeamByPlayerName(d.getName()).getKind().getColor() + d.getName();
					
					if (event.getDamager() instanceof TNTPrimed) {
						deathMessage = defenderString + ChatColor.WHITE + " exploded";
					} else {
						deathMessage = defenderString + ChatColor.WHITE + " died";
					}
					for (Team team : defenderWarzone.getTeams()) {
						team.teamcast(deathMessage);
					}
				}
				//zero defender kills for dying from something that is inhuman
				Team.getTeamByPlayerName(d.getName()).zeroKills(d);
				//lets see if it was an airstrike that was called in by us that killed you
				if(event.getDamager() instanceof TNTPrimed) {
					TNTPrimed tnt = (TNTPrimed) event.getDamager();
					List<MetadataValue> metadata = tnt.getMetadata("WarKillstreak");
					if((!metadata.isEmpty()) && (metadata != null)) {
						int killstreak = (int) (metadata.get(0).asLong() << 32);
						int data = (int) (metadata.get(0).asLong() & 0x00000000FFFFFFFF);
						if(killstreak == 5) {
							Player p = this.getPlayerForId(data);
							Team pTeam = Team.getTeamByPlayerName(p.getDisplayName());
							if(pTeam != null) {
								pTeam.incKills(p);
								this.checkKillStreak(p, pTeam);
								defenderWarzone.updateLastDamager(d, null); //make it so we give no more extra credit
							}
						}
					}
				}
				//lets see if it was one of our killstreak wolves that killed you
				if(event.getDamager() instanceof Wolf) {
					Wolf wolf = (Wolf) event.getDamager();
					List<MetadataValue> metadata = wolf.getMetadata("WarKillstreak");
					if((!metadata.isEmpty()) && (metadata != null)) {
						int killstreak = (int) (metadata.get(0).asLong() << 32);
						int data = (int) (metadata.get(0).asLong() & 0x00000000FFFFFFFF);
						if(killstreak == 7) {
							Player p = this.getPlayerForId(data);
							Team pTeam = Team.getTeamByPlayerName(p.getDisplayName());
							if(pTeam != null) {
								pTeam.incKills(p);
								this.checkKillStreak(p, pTeam);
								defenderWarzone.updateLastDamager(d, null);
							}
						}
					}
				}
				
				
				//lets see if we can give someone partial credit
				Player lastAttacker = defenderWarzone.getLastDamager(d);
				//null checks
				if(lastAttacker != null) {
					Team aTeam = Team.getTeamByPlayerName(lastAttacker.getName());
					if(aTeam != null) {
						//check if they are in the same warzone too
						if(defenderWarzone == Warzone.getZoneByTeam(aTeam)) {
							//give them credit for killing them
							aTeam.incKills(lastAttacker);
							this.checkKillStreak(lastAttacker, aTeam);
						}
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
	@EventHandler
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
				} else if (dont.getState() instanceof InventoryHolder) {
					ItemStack[] contents = ((InventoryHolder)dont.getState()).getInventory().getContents();
					Block worldBlock = dont.getWorld().getBlockAt(dont.getLocation());
					if (worldBlock.getState() instanceof InventoryHolder) {
						((InventoryHolder)worldBlock.getState()).getInventory().clear();
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
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEntityDamage(final EntityDamageEvent event) {
		if (!War.war.isLoaded()) {
			return;
		}

		Entity entity = event.getEntity();
		
		if(!(entity instanceof Player)) {
			return;
		}
		
		if(event instanceof EntityDamageByEntityEvent) {
		    if (entity instanceof Player) {
			    EntityDamageByEntityEvent even = (EntityDamageByEntityEvent) event;
			    //we need to make wolves stronger if they are from killstreaks
			    if(even.getDamager() instanceof Wolf) {
				    List<MetadataValue> metadata = even.getDamager().getMetadata("WarKillstreak");
				    if((metadata.isEmpty()) || (metadata == null)) {
				    	
				    } else {
				        int killstreak = (int) (metadata.get(0).asLong() >> 32);
				        if((killstreak == 7)) {
				            //amp this wolf
					        event.setDamage(event.getDamage() + 5); //2.5 more hearts per hit
				        }
				    }
			    }
			}
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
			Team team = Team.getTeamByPlayerName(player.getName());
			
			if (zone != null && team != null) {
				LoadoutSelection playerLoadoutState = zone.getLoadoutSelections().get(player.getName()); 
				if (team.getSpawnVolume().contains(player.getLocation())
						&& playerLoadoutState != null && playerLoadoutState.isStillInSpawn()) {
					// don't let a player still in spawn get damaged
					event.setCancelled(true);
				} else if (event.getDamage() >= player.getHealth()) {
					if (zone.getReallyDeadFighters().contains(player.getName())) {
						// don't re-count the death points of an already dead person, make sure they are dead though
						// (reason for this is that onEntityDamage sometimes fires more than once for one death)
						if (player.getHealth() != 0) {
							player.setHealth(0);
						}
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
						for (Team teamToMsg : zone.getTeams()) {
							teamToMsg.teamcast(deathMessage);
						}
					}
					
					//we still must 0 your killstreaks for dying stupidly
					team.zeroKills(player);
					
					//lets see if we can give partial credit
					Player lastAttacker = zone.getLastDamager(player);
					//null checks
					if(lastAttacker != null) {
						Team aTeam = Team.getTeamByPlayerName(lastAttacker.getName());
						if(aTeam != null) {
							//check if they are in the same warzone too
							if(zone == Warzone.getZoneByTeam(aTeam)) {
								//give them credit for killing them
								aTeam.incKills(lastAttacker);
								this.checkKillStreak(lastAttacker, aTeam);
							}
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
	}

	@EventHandler
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
				// Stop fire
				player.setFireTicks(0);
				event.setCancelled(true);
			}
		}
	}

	/**
	 * Prevents creatures from spawning in warzones if no creatures is active
	 *
	 * @see EntityListener.onCreatureSpawn()
	 */
	@EventHandler
	public void onCreatureSpawn(final CreatureSpawnEvent event) {
		if (!War.war.isLoaded()) {
			return;
		}

		Location location = event.getLocation();
		Warzone zone = Warzone.getZoneByLocation(location);
		
		if (zone != null && zone.getWarzoneConfig().getBoolean(WarzoneConfig.NOCREATURES)) {
			if(event.getEntityType() == EntityType.WOLF) {
				List<MetadataValue> metadata = event.getEntity().getMetadata("WarKillstreak");
				if((!metadata.isEmpty()) && (metadata != null)) {
					int killstreak = (int) (metadata.get(0).asLong() >> 32);
					if((killstreak == 7)) {
						event.setCancelled(false);
						return; //don't continue down code just to cancel the event
					}
				}
			}
			event.setCancelled(true);
		}
	}

	/**
	 * Prevents health regaining caused by peaceful mode
	 *
	 * @see EntityListener.onEntityRegainHealth()
	 */
	@EventHandler
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
	
	@EventHandler
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
	
	@EventHandler(priority = EventPriority.HIGHEST)
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
				
				if (zone.getWarzoneConfig().getBoolean(WarzoneConfig.DEATHMESSAGES)) {
					for (Team team : zone.getTeams()) {
						team.teamcast(player.getName() + " died");
					}
				}
			}
		}
	}
	
	@EventHandler
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
	
	@EventHandler
	public void onProjectileHit(final ProjectileHitEvent event) {
		//check to see if we are loaded
		if(!War.war.isLoaded()) {
			return;
		}
		//eggs will be our airstrike thing...
		if(event.getEntityType() == EntityType.EGG) {
			if(this.eggsForExplosion.contains((Egg) event.getEntity())) {
			    Location l = event.getEntity().getLocation();
			    Warzone w = Warzone.getZoneByLocation(l);
			    if(w == null) { //we are not in a zone, dont explode
			    	return;
			    }
			    List<MetadataValue> metadata = event.getEntity().getMetadata("WarKillstreak");
				if((!metadata.isEmpty()) && (metadata != null)) {
					int killstreak = (int) (metadata.get(0).asLong() >> 32);
					int data = (int) (metadata.get(0).asLong() & 0x00000000FFFFFFFF);
					if(killstreak == 5) {
			            Player p = this.getPlayerForId(data);
			            event.getEntity().removeMetadata("WarKillstreak", War.war);
			            this.callInAirstrike(l, p);
					}
			    }
			}
		}
	}
	
	@EventHandler
	public void onProjectileLaunch(final ProjectileLaunchEvent event) {
		if(!War.war.isLoaded()) {
			return;
		}
		//now we can add our eggs :D
		if(event.getEntityType() == EntityType.EGG) {
			LivingEntity e = event.getEntity().getShooter();
			if(e instanceof Player) {
				Player p = (Player) e;
				Warzone zone = Warzone.getZoneByLocation(p);
				if(zone != null) {
					Team t = zone.getPlayerTeam(p.getDisplayName());
					if((t != null) && (t.hasFiveKillStreak(p))) {
						event.getEntity().setMetadata("WarKillstreak", new KillstreakMetadata(5, p.getEntityId()));
						this.eggsForExplosion.add((Egg) event.getEntity());
						t.removeFiveKillStreak(p);
						t.teamcast(t.getKind().getColor() + p.getDisplayName() + ChatColor.WHITE + 
								" called in an airstrike!");
					}
				}
			}
		}
	}

	private void checkKillStreak(Player p, Team t) {
		if(!Warzone.getZoneByLocation(p).getWarzoneConfig().getBoolean(WarzoneConfig.KILLSTREAKS)) {
			return;
		}
		int kills = t.getKills(p);
		if(kills == 3) { //EXTRAHEALTH AND REFILL
			this.doThreeKillstreak(p, t);
			War.war.msg(p, "Congratulations on the three killstreak!");
			War.war.msg(p, "You have been awarded 5 extra health, and your health has been refilled!");
			this.broadcastKillstreak(p, t, 3);
		} else if(kills == 5) { //AIRSTRIKE
			t.addFiveKillStreak(p);
			p.getInventory().addItem(new ItemStack(Material.EGG));
			War.war.msg(p, "Congratulations on the five killstreak!");
			War.war.msg(p, "You have been awarded an airstrike! Throw the Egg where you would like the airstrike!");
			this.broadcastKillstreak(p, t, 5);
		} else if(kills == 7) { //DOGGIES
			t.addSevenKillStreak(p);
			p.getInventory().addItem(new ItemStack(Material.RAW_BEEF));
			War.war.msg(p, "Congratulations on the seven killstreak!");
			War.war.msg(p, "You have been awarded dogs! Left Click with the Steak when you would like to call in the dogs!");
			this.broadcastKillstreak(p, t, 7);
		} else if(kills > 7) {
			this.broadcastKillstreak(p, t, kills);
		} else {
			War.war.msg(p, "You have " + kills + " kills this life!");
		}
	}
	
	protected void spawnDogs(Player p, Team t) {
		Location spawnLoc = t.getTeamSpawn();
		Warzone w = Warzone.getZoneByLocation(t.getTeamSpawn());
		//logic for determining all enemies in a warzone
		List<Player> enemies = new ArrayList<Player>();
		for(Team team : w.getTeams()) {
			if(!team.equals(t)) {
				enemies.addAll(team.getPlayers());
			}
		}
		Wolf[] dogs = new Wolf[4];
		int[] indices = new int[4];
		int i = 0;
		for(Wolf f : dogs) {
			f = (Wolf) spawnLoc.getWorld().spawnEntity(spawnLoc.add(new Vector(0, 1, 0)), EntityType.WOLF);
			f.setAdult();
			f.setMaxHealth(16);
			f.setHealth(16); //goes down to 15 after we damage the wolf
			f.setMetadata("WarKillstreak", new KillstreakMetadata(7, p.getEntityId()));
			int index = this.killSeed.nextInt(enemies.size()) - 1;
			if(index < 0) {
				index = 0;
			}
			if(index > enemies.size()) {
				index = enemies.size() - 1;
			}
			f.setTarget(enemies.get(index));
			f.damage(1, enemies.get(index)); //fix to f.setTarget not working
			indices[i] = index;
			dogs[i] = f; //should unnull wolves
			i++;
		}
		Player[] ps = new Player[4];
		for(int j = 0; j < 4; j++) {
			ps[j] = enemies.get(indices[j]);
		}
		BukkitTask z = War.war.getServer().getScheduler().runTaskTimer(War.war, new CheckWolfsJob(dogs, ps), 60, 200);
		t.addDoggyManager(z);
		
	}
	
	private void broadcastKillstreak(Player p, Team t, int streak) {
		Warzone zone = Warzone.getZoneByLocation(p);
		for(Team team : zone.getTeams()) {
			team.teamcast(t.getKind().getColor() + p.getDisplayName() + ChatColor.WHITE + " is on a " + streak + " killstreak");
		}
	}
	
	private void callInAirstrike(Location l, Player p) {
		Location tntPlace = new Location(l.getWorld(), l.getX(), Warzone.getZoneByLocation(l).getVolume().getMaxY(), l.getZ());
		TNTPrimed[] tnt = new TNTPrimed[4];
		tnt[0] = (TNTPrimed) l.getWorld().spawnEntity(tntPlace, EntityType.PRIMED_TNT);
		tnt[1] = (TNTPrimed) l.getWorld().spawnEntity(tntPlace.add(new Vector(2, 0, 0)), EntityType.PRIMED_TNT);
		tnt[2] = (TNTPrimed) l.getWorld().spawnEntity(tntPlace.add(new Vector(2, 0, 2)), EntityType.PRIMED_TNT);
		tnt[3] = (TNTPrimed) l.getWorld().spawnEntity(tntPlace.add(new Vector(0, 0, 2)), EntityType.PRIMED_TNT);
		for(TNTPrimed t : tnt) {
			t.setMetadata("WarKillstreak", new KillstreakMetadata(5, p.getEntityId()));
		}
	}
	
	private void doThreeKillstreak(Player p, Team t) {
		p.setMaxHealth(25);
		p.setHealth(25);
	}
	
	private Player getPlayerForId(int id) {
		Player p = null;
		for(Player pl : War.war.getServer().getOnlinePlayers()) {
			if(pl.getEntityId() == id) {
				p = pl;
				break;
			}
		}
		return p;
	}
}
