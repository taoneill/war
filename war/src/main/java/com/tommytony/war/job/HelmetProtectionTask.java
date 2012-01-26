package com.tommytony.war.job;

import java.util.HashMap;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.tommytony.war.Team;
import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import com.tommytony.war.config.TeamKind;
import com.tommytony.war.config.WarzoneConfig;


/**
 * Sets the helmet again onto the players heads. Also limits the number of blocks being held.
 *
 * @author Tim Düsterhus
 */
public class HelmetProtectionTask implements Runnable {

	/**
	 * @see Runnable.run()
	 */
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
						if (playerInv.getHelmet().getType() != teamBlockMaterial) {
							playerInv.setHelmet(this.createBlockHead(team));
						}
						// 2) Get rid of extra blocks in inventory: only keep one
						HashMap<Integer, ? extends ItemStack> blocks = playerInv.all(teamBlockMaterial);
						if (blocks.size() > 1 || (blocks.size() == 1 && blocks.get(blocks.keySet().iterator().next()).getAmount() > 1)) {
							int i = 0;
							int removed = 0;
							for (ItemStack item : playerInv.getContents()) {
								// remove only same colored wool
								if (item != null && item.getType() == teamBlockMaterial && item.getData().getData() == team.getKind().getData()) {
									playerInv.clear(i);
									removed++;
								}
								i++;
							}
							
							int firstEmpty = playerInv.firstEmpty();
							if (firstEmpty > 0) {
								playerInv.setItem(firstEmpty, this.createBlockHead(team));	
							}
							
							if (removed > 1) {
								War.war.badMsg(player, "All that " + team.getName() + " wool must have been heavy!");
							}
						}
					} else {
						// enfore helmet (good thing?)
						if (team.getKind() == TeamKind.GOLD) {
							teamBlockMaterial = Material.GOLD_HELMET;
						} else if (team.getKind() == TeamKind.DIAMOND) {
							teamBlockMaterial = Material.DIAMOND_HELMET;
						} else if (team.getKind() == TeamKind.IRON) {
							teamBlockMaterial = Material.IRON_HELMET;
						} else {
							teamBlockMaterial = Material.LEATHER_HELMET;
						}
						if (playerInv.getHelmet() != null && playerInv.getHelmet().getType() != teamBlockMaterial) {
							playerInv.setHelmet(new ItemStack(teamBlockMaterial));
						}
						HashMap<Integer, ? extends ItemStack> helmets = playerInv.all(teamBlockMaterial);
						if (helmets.size() > 1 || (helmets.size() == 1 && helmets.get(helmets.keySet().iterator().next()).getAmount() > 1)) {
							playerInv.remove(teamBlockMaterial);
							playerInv.setItem(playerInv.firstEmpty(), new ItemStack(teamBlockMaterial));
							War.war.badMsg(player, "All those helmets must have been heavy!");
						}
					}
					
					// check for thieves without their treasure in their hands
					if (zone.isFlagThief(player.getName())) {
						Team victim = zone.getVictimTeamForFlagThief(player.getName());
						player.setItemInHand(null);
						player.getInventory().addItem(new ItemStack(victim.getKind().getMaterial(), 2240, victim.getKind().getData(), victim.getKind().getData()));
					} else if (zone.isBombThief(player.getName())) {
						player.setItemInHand(null);
						player.getInventory().addItem(new ItemStack(Material.TNT, 2240));
					} else if (zone.isCakeThief(player.getName())) {
						player.setItemInHand(null);
						player.getInventory().addItem(new ItemStack(Material.CAKE_BLOCK, 2240));
					}
				}
			}
		}
	}

	public ItemStack createBlockHead(Team team) {
		return new ItemStack(team.getKind().getMaterial(), 1, (short) 1, new Byte(team.getKind().getData()));
	}
}
