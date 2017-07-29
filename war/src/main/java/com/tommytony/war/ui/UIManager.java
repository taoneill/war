package com.tommytony.war.ui;

import com.tommytony.war.War;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Connor on 7/25/2017.
 */
public class UIManager implements Listener {
	private final War plugin;
	private Map<Player, ChestUI> uiMap;
	private Map<Player, StringRunnable> messageMap;

	public UIManager(War plugin) {
		this.plugin = plugin;
		this.uiMap = new HashMap<Player, ChestUI>();
		this.messageMap = new HashMap<Player, StringRunnable>();
	}

	public void assignUI(Player player, ChestUI ui) {
		Inventory inv = Bukkit.getServer().createInventory(null, ui.getSize(), ui.getTitle());
		ui.build(player, inv);
		uiMap.put(player, ui);
		player.closeInventory();
		player.openInventory(inv);
	}

	public void getPlayerMessage(Player player, String prompt, StringRunnable action) {
		messageMap.put(player, action);
		for (int i = 0; i < 10; i++) {
			player.sendMessage("");
		}
		player.sendMessage("CHAT DISABLED WHILE WAITING FOR RESPONSE");
		for (int i = 0; i < 8; i++) {
			player.sendMessage("|");
		}
		player.sendMessage(prompt);
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		Player player = (Player) event.getWhoClicked();
		ItemStack clicked = event.getCurrentItem();
		Inventory inventory = event.getInventory();

		if (uiMap.containsKey(player)) {
			ChestUI ui = uiMap.get(player);
			if (inventory.getName().equals(ui.getTitle())) {
				if (ui.processClick(clicked, inventory)) {
					event.setCancelled(true);
					player.closeInventory();
					uiMap.remove(player);
				}
			} else {
				uiMap.remove(player);
			}
		}
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		ItemStack item = event.getItem();
		if (item != null && item.getType() == Material.TNT
				&& (item.getDurability() == 7 ||
					(item.hasItemMeta() && item.getItemMeta().hasDisplayName()
							&& item.getItemMeta().getDisplayName().contains("War")))) {
			event.setCancelled(true);
			this.assignUI(player, new WarUI());
		}
	}

	@EventHandler
	public void onChat(AsyncPlayerChatEvent event) {
		if (messageMap.containsKey(event.getPlayer())) {
			event.setCancelled(true);
			StringRunnable exe = messageMap.remove(event.getPlayer());
			exe.setValue(event.getMessage());
			War.war.getServer().getScheduler().runTask(War.war, exe);
			return;
		}
		for (Player p : messageMap.keySet()) {
			if (event.getRecipients().contains(p)) {
				event.getRecipients().remove(p);
			}
		}
	}

	@EventHandler
	public void onPlayerLeave(PlayerQuitEvent event) {
		this.uiMap.remove(event.getPlayer());
		this.messageMap.remove(event.getPlayer());
	}

}
