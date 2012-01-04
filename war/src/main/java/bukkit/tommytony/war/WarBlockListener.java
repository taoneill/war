package bukkit.tommytony.war;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.getspout.spoutapi.player.SpoutPlayer;

import com.tommytony.war.FlagReturn;
import com.tommytony.war.Monument;
import com.tommytony.war.Team;
import com.tommytony.war.Warzone;

/**
 *
 * @author tommytony
 *
 */
public class WarBlockListener extends BlockListener {

	/**
	 * @see BlockListener.onBlockPlace()
	 */
	@Override
	public void onBlockPlace(BlockPlaceEvent event) {
		if (!War.war.isLoaded()) {
			return;
		}

		Player player = event.getPlayer();
		Block block = event.getBlock();
		if (player == null || block == null) {
			return;
		}

		Team team = Team.getTeamByPlayerName(player.getName());
		Warzone zone = Warzone.getZoneByLocation(player);
		// Monument capturing
		if (team != null && block != null && zone != null && zone.isMonumentCenterBlock(block) && block.getType() == team.getKind().getMaterial() && block.getData() == team.getKind().getData()) {
			Monument monument = zone.getMonumentFromCenterBlock(block);
			if (monument != null && !monument.hasOwner()) {
				monument.capture(team);
				List<Team> teams = zone.getTeams();
				for (Team t : teams) {
					t.teamcast("Monument " + monument.getName() + " has been captured by team " + team.getName() + ".");
				}
				event.setCancelled(false);
				return; // important otherwise cancelled down a few line by isImportantblock
			} else {
				War.war.badMsg(player, "You can't capture a monument without a block of your team's material. Get one from your team spawn.");
				event.setCancelled(true);
				return;
			}
		}

		boolean isZoneMaker = War.war.isZoneMaker(player);
		// prevent build in important parts
		if (zone != null && zone.isImportantBlock(block) && (!isZoneMaker || (isZoneMaker && team != null))) {
			War.war.badMsg(player, "Can't build here.");
			event.setCancelled(true);
			return;
		}

		// protect warzone lobbies
		for (Warzone wz : War.war.getWarzones()) {
			if (wz.getLobby() != null && wz.getLobby().getVolume() != null && wz.getLobby().getVolume().contains(block)) {
				War.war.badMsg(player, "Can't build here.");
				event.setCancelled(true);
				return;
			}
		}

		// protect the hub
		if (War.war.getWarHub() != null && War.war.getWarHub().getVolume().contains(block)) {
			War.war.badMsg(player, "Can't build here.");
			event.setCancelled(true);
			return;
		}

		// buildInZonesOnly
		if (zone == null && War.war.isBuildInZonesOnly() && !War.war.canBuildOutsideZone(player)) {
			if (!War.war.isDisableBuildMessage()) {
				War.war.badMsg(player, "You can only build inside warzones. Ask for the 'war.build' permission to build outside.");
			}
			event.setCancelled(true);
			return;
		}

		// can't place a block of your team's color
		if (team != null && block.getType() == team.getKind().getMaterial() && block.getData() == team.getKind().getData()) {
			War.war.badMsg(player, "You can only use your team's blocks to capture monuments.");
			event.setCancelled(true);
			return;
		}

		// a flag thief can't drop his flag
		if (team != null && zone != null && zone.isFlagThief(player.getName())) {
			War.war.badMsg(player, "Can't drop the flag. What are you doing? Run!");
			event.setCancelled(true);

		}

		// unbreakableZoneBlocks
		if (zone != null && zone.isUnbreakableZoneBlocks() && (!isZoneMaker || (isZoneMaker && team != null))) {
			// if the zone is unbreakable, no one but zone makers can break blocks (even then, zone makers in a team can't break blocks)
			War.war.badMsg(player, "The blocks in this zone are unbreakable - this also means you can't build!");
			event.setCancelled(true);
			return;
		}
	}
	
	// Do not allow moving of block into or from important zones
	public void onBlockPistonExtend(BlockPistonExtendEvent event) {
		Warzone zone = Warzone.getZoneByLocation(event.getBlock().getLocation());
		if (zone!=null) {
			for (Block b : event.getBlocks()) {
				if (zone.isImportantBlock(b)) {
					event.setCancelled(true);
					return;
				}
			}
			if (zone.isImportantBlock(event.getBlock().getRelative(event.getDirection(), event.getLength()+1))) {
				event.setCancelled(true);
				return;
			}
		}
	}
	public void onBlockPistonRetract(BlockPistonRetractEvent event) {
		Warzone zone = Warzone.getZoneByLocation(event.getBlock().getLocation());
		if (zone!=null) {
			Block b = event.getBlock().getRelative(event.getDirection(), 2);
			if (zone.isImportantBlock(b)) {
				event.setCancelled(true);
				return;
			}
		}
	}
	
	/**
	 * @see BlockListener.onBlockBreak()
	 */
	@Override
	public void onBlockBreak(BlockBreakEvent event) {
		if (!War.war.isLoaded()) {
			return;
		}
		Player player = event.getPlayer();
		Block block = event.getBlock();
		if (player != null && block != null) {
			this.handleBreakOrDamage(player, block, event);
		}
	}
	
	public void onBlockDamage(BlockDamageEvent event) {
		if (!War.war.isLoaded()) {
			return;
		}
		Player player = event.getPlayer();
		Block block = event.getBlock();
		Warzone playerZone = Warzone.getZoneByLocation(player);
		if (player != null && block != null && playerZone != null && playerZone.isInstaBreak()) {
			Warzone blockZone = Warzone.getZoneByLocation(new Location(block.getWorld(), block.getX(), block.getY(), block.getZ()));
			if (blockZone != null && blockZone == playerZone) {
				event.setInstaBreak(true);
			}
		}
	}

	private void handleBreakOrDamage(Player player, Block block, Cancellable event) {
		Warzone warzone = Warzone.getZoneByLocation(player);
		Team team = Team.getTeamByPlayerName(player.getName());
		boolean isZoneMaker = War.war.isZoneMaker(player);

		if (warzone != null && team == null && !isZoneMaker) {
			// can't actually destroy blocks in a warzone if not part of a team
			War.war.badMsg(player, "Can't destroy part of a warzone if you're not in a team.");
			event.setCancelled(true);
			return;
		}
		// monument's center is destroyed
		if (team != null && block != null && warzone != null && warzone.isMonumentCenterBlock(block)) {
			Monument monument = warzone.getMonumentFromCenterBlock(block);
			if (monument.hasOwner()) {
				List<Team> teams = warzone.getTeams();
				for (Team t : teams) {
					t.teamcast("Team " + monument.getOwnerTeam().getName() + " loses control of monument " + monument.getName());
				}
				monument.uncapture();
			}
			event.setCancelled(false);
			return;
		}
		// changes in parts of important areas
		if (warzone != null && warzone.isImportantBlock(block) && (!isZoneMaker || (isZoneMaker && team != null))) {
			// breakage of spawn
			if (team != null && team.getSpawnVolume().contains(block)) {
				ItemStack teamKindBlock = new ItemStack(team.getKind().getMaterial(), team.getKind().getData());
				// let team members loot one block the spawn for monument captures
				if (player.getInventory().contains(teamKindBlock)) {
					War.war.badMsg(player, "You already have a " + team.getName() + " block.");
					event.setCancelled(true);
					return;
				} else {
					event.setCancelled(false); // very important, otherwise could get cancelled but unbreakableZoneBlocks further down
					return;
				}
			}
			// stealing of flag
			if (team != null && warzone.isEnemyTeamFlagBlock(team, block)) {
				if (warzone.isFlagThief(player.getName())) {
					// detect audacious thieves
					War.war.badMsg(player, "You can only steal one flag at a time!");
				} else {
					Team lostFlagTeam = warzone.getTeamForFlagBlock(block);
					if (lostFlagTeam.getPlayers().size() != 0) {
						// player just broke the flag block of other team: cancel to avoid drop, give player the block, set block to air
						ItemStack teamKindBlock = new ItemStack(lostFlagTeam.getKind().getMaterial(), 1, (short) 1, new Byte(lostFlagTeam.getKind().getData()));
						player.getInventory().clear();
						player.getInventory().addItem(teamKindBlock);
						warzone.addFlagThief(lostFlagTeam, player.getName());
						block.setType(Material.AIR);

						String spawnOrFlag = "spawn or flag";
						if (warzone.getFlagReturn() == FlagReturn.FLAG || warzone.getFlagReturn() == FlagReturn.SPAWN) {
							spawnOrFlag = warzone.getFlagReturn().toString();
						}

						for (Team t : warzone.getTeams()) {
							t.teamcast(team.getKind().getColor() + player.getName() + ChatColor.WHITE + " stole team " + lostFlagTeam.getName() + "'s flag.");
							if (t.getName().equals(lostFlagTeam.getName())) {
								if (War.war.isSpoutServer()) {
									for (Player p : t.getPlayers()) {
										SpoutPlayer sp = (SpoutPlayer) p;
										if (sp.isSpoutCraftEnabled()) {
											String tn = team.getName();
							                sp.sendNotification(tn.substring(0,1).toUpperCase()+tn.substring(1).toLowerCase()+" stole your Flag!","Stolen by "+player.getName(),lostFlagTeam.getKind().getMaterial(),lostFlagTeam.getKind().getData(),3000);
										}
									}
								}
								t.teamcast("Prevent " + team.getKind().getColor() + player.getName() + ChatColor.WHITE
										+ " from reaching team " + team.getName() + "'s " + spawnOrFlag + ".");
							}
						}


						War.war.msg(player, "You have team " + lostFlagTeam.getName() + "'s flag. Reach your team " + spawnOrFlag + " to capture it!");
					} else {
						War.war.msg(player, "You can't steal team " + lostFlagTeam.getName() + "'s flag since no players are on that team.");
					}
				}
				event.setCancelled(true);
				return;
			} else if (!warzone.isMonumentCenterBlock(block)) {
				War.war.badMsg(player, "Can't destroy this.");
				event.setCancelled(true);
				return;
			}
		}

		// protect warzone lobbies
		if (block != null) {
			for (Warzone zone : War.war.getWarzones()) {
				if (zone.getLobby() != null && zone.getLobby().getVolume() != null && zone.getLobby().getVolume().contains(block)) {
					War.war.badMsg(player, "Can't destroy this.");
					event.setCancelled(true);
					return;
				}
			}
		}

		// protect the hub
		if (War.war.getWarHub() != null && War.war.getWarHub().getVolume().contains(block)) {
			War.war.badMsg(player, "Can't destroy this.");
			event.setCancelled(true);
			return;
		}

		// buildInZonesOnly
		Warzone blockZone = Warzone.getZoneByLocation(new Location(block.getWorld(), block.getX(), block.getY(), block.getZ()));
		if (blockZone == null && War.war.isBuildInZonesOnly() && !War.war.canBuildOutsideZone(player)) {
			if (!War.war.isDisableBuildMessage()) {
				War.war.badMsg(player, "You can only build inside warzones. Ask for the 'war.build' permission to build outside.");
			}
			event.setCancelled(true);
			return;
		}

		// unbreakableZoneBlocks
		if (blockZone != null && blockZone.isUnbreakableZoneBlocks() && (!isZoneMaker || (isZoneMaker && team != null))) {
			// if the zone is unbreakable, no one but zone makers can break blocks (even then, zone makers in a team can't break blocks
			War.war.badMsg(player, "The blocks in this zone are unbreakable!");
			event.setCancelled(true);
			return;
		}
	}
}
