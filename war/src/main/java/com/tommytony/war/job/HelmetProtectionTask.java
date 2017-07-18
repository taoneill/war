package com.tommytony.war.job;

import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.tommytony.war.Team;
import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import com.tommytony.war.config.WarzoneConfig;


/**
 * Sets the helmet again onto the players heads. Also limits the number of blocks being held.
 *
 * @author Tim Düsterhus
 */
public class HelmetProtectionTask implements Runnable {

	public void run() {
		if (!War.war.isLoaded()) {
			return;
		}
		for (Warzone zone : War.war.getWarzones()) {
			for (Team team : zone.getTeams()) {
				for (Player player : team.getPlayers()) {
					PlayerInventory playerInv = player.getInventory();
					Material teamBlockMaterial;
					
					if (zone.getWarzoneConfig().getBoolean(WarzoneConfig.BLOCKHEADS)) {
						teamBlockMaterial = team.getKind().getMaterial();
						// 1) Replace missing block head
						if (playerInv.getHelmet() == null || playerInv.getHelmet().getType() != Material.LEATHER_HELMET) {
							playerInv.setHelmet(team.getKind().getHat());
						}
						
						// 2) Get rid of extra blocks in inventory: only keep one
						HashMap<Integer, ? extends ItemStack> blocks = playerInv.all(teamBlockMaterial);
						if (blocks.size() > 1 || (blocks.size() == 1 && blocks.get(blocks.keySet().iterator().next()).getAmount() > 1)) {
							int i = 0;
							int removed = 0;
							for (ItemStack item : playerInv.getContents()) {
								// remove only same colored wool
								if (item != null && item.getType() == teamBlockMaterial && item.getData() == team.getKind().getBlockData()) {
									playerInv.clear(i);
									removed++;
								}
								i++;
							}
							
							int firstEmpty = playerInv.firstEmpty();
							if (firstEmpty > 0) {
								playerInv.setItem(firstEmpty, team.getKind().getBlockHead());
							}
							
							if (removed > 1) {
								War.war.badMsg(player, "All that " + team.getName() + " wool must have been heavy!");
							}
						}
					}
					
					// check for thieves without their treasure in their hands
					if (zone.isFlagThief(player)) {
						Team victim = zone.getVictimTeamForFlagThief(player);
						player.getInventory().setItemInMainHand(null);
						player.getInventory().setItemInOffHand(null);
						player.getInventory().setHeldItemSlot(0);
						player.getInventory().addItem(victim.getKind().getBlockData().toItemStack(2240));
					} else if (zone.isBombThief(player)) {
						player.getInventory().setItemInMainHand(null);
						player.getInventory().setItemInOffHand(null);
						player.getInventory().setHeldItemSlot(0);
						player.getInventory().addItem(new ItemStack(Material.TNT, 2240));
					} else if (zone.isCakeThief(player)) {
						player.getInventory().setItemInMainHand(null);
						player.getInventory().setItemInOffHand(null);
						player.getInventory().setHeldItemSlot(0);
						player.getInventory().addItem(new ItemStack(Material.CAKE_BLOCK, 2240));
					}
				}
			}
		}
	}
}
