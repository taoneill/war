package com.tommytony.war.jobs;

import java.util.logging.Level;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.tommytony.war.Team;
import com.tommytony.war.TeamKind;
import com.tommytony.war.Warzone;

import bukkit.tommytony.war.War;

/**
 * Sets the helmet again onto the players heads
 *
 * @author Tim Düsterhus
 */
public class HelmetProtectionTask implements Runnable {

	/**
	 * @see Runnable.run()
	 */
	public void run() {
		while (true) {
			for (Warzone zone : War.war.getWarzones()) {
				for (Team team : zone.getTeams()) {
					for (Player player : team.getPlayers()) {
						PlayerInventory playerInv = player.getInventory();
						if (zone.isBlockHeads()) {
							playerInv.setHelmet(new ItemStack(team.getKind().getMaterial(), 1, (short) 1, new Byte(team.getKind().getData())));
						} else {
							if (team.getKind() == TeamKind.GOLD) {
								playerInv.setHelmet(new ItemStack(Material.GOLD_HELMET));
							} else if (team.getKind() == TeamKind.DIAMOND) {
								playerInv.setHelmet(new ItemStack(Material.DIAMOND_HELMET));
							} else if (team.getKind() == TeamKind.IRON) {
								playerInv.setHelmet(new ItemStack(Material.IRON_HELMET));
							} else {
								playerInv.setHelmet(new ItemStack(Material.LEATHER_HELMET));
							}
						}
					}
				}
			}
			try {
				Thread.sleep((War.war.isLoaded()) ? 500 : 10000);
			} catch (InterruptedException e) {
			}
		}
	}
}
