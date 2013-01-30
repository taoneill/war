package com.tommytony.war.mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import com.tommytony.war.War;
import java.util.Arrays;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

public class LoadoutYmlMapper {
	
	public static void fromConfigToLoadouts(ConfigurationSection config, HashMap<String, HashMap<Integer, ItemStack>> loadouts) {
		List<String> loadoutNames = config.getStringList("names");
		loadouts.clear();
		for (String name : loadoutNames) {
			HashMap<Integer, ItemStack> newLoadout = new HashMap<Integer, ItemStack>();
			fromConfigToLoadout(config, newLoadout, name);
			loadouts.put(name, newLoadout);
		}
	}
	
	public static void fromConfigToLoadout(ConfigurationSection config, HashMap<Integer, ItemStack> loadout, String loadoutName) {
		List<Integer> slots = config.getIntegerList(loadoutName + ".slots");
		for (Integer slot : slots) {
			String prefix = loadoutName + "." + slot + ".";
			int id = config.getInt(prefix + "id");
			byte data = (byte)config.getInt(prefix + "data");
			int amount = config.getInt(prefix + "amount");
			short durability = (short)config.getInt(prefix + "durability");
			
			ItemStack stack = new ItemStack(id, amount, durability, data);
			stack.setDurability(durability);
			
			if (config.contains(prefix + "enchantments")) {
				List<String> enchantmentStringList = config.getStringList(prefix + "enchantments");
				for (String enchantmentString : enchantmentStringList) {
					String[] enchantmentStringSplit = enchantmentString.split(",");
					if (enchantmentStringSplit.length == 2) {
						int enchantId = Integer.parseInt(enchantmentStringSplit[0]);
						int level = Integer.parseInt(enchantmentStringSplit[1]);
						War.war.safelyEnchant(stack, Enchantment.getById(enchantId), level);
					}
				}
			}
                        if (config.contains(prefix + "armorcolor")) {
                            int rgb = config.getInt(prefix + "armorcolor");
                            Color clr = Color.fromRGB(rgb);
                            LeatherArmorMeta meta = (LeatherArmorMeta) stack.getItemMeta();
                            meta.setColor(clr);
                            stack.setItemMeta(meta);
                        }
                        if (config.contains(prefix + "name")) {
                            String itemName = config.getString(prefix + "name");
                            ItemMeta meta = stack.getItemMeta();
                            meta.setDisplayName(itemName);
                            stack.setItemMeta(meta);
                        }
                        if (config.contains(prefix + "lore")) {
                            List<String> itemLore = config.getStringList(prefix + "lore");
                            ItemMeta meta = stack.getItemMeta();
                            meta.setLore(itemLore);
                            stack.setItemMeta(meta);
                        }
			
			loadout.put(slot, stack);
		}
	}
	
	public static void fromLoadoutsToConfig(HashMap<String, HashMap<Integer, ItemStack>> loadouts, ConfigurationSection section) {
		List<String> sortedNames = sortNames(loadouts);
		
		section.set("names", sortedNames);
		for (String name : sortedNames) {
			fromLoadoutToConfig(name, loadouts.get(name), section);
		}
	}
	
	public static List<String> sortNames(HashMap<String, HashMap<Integer, ItemStack>> loadouts) {
		List<String> sortedNames = new ArrayList<String>();
		
		// default comes first
		if (loadouts.containsKey("default")) {
			sortedNames.add("default");
		}
		
		for (String name : loadouts.keySet()) {
			if (!name.equals("default")) {
				sortedNames.add(name);
			}
		}
		
		return sortedNames; 
	}

	private static List<Integer> toIntList(Set<Integer> keySet) {
		List<Integer> list = new ArrayList<Integer>();
		for (Integer key : keySet) {
			list.add(key);
		}
		return list;
	}

	public static void fromLoadoutToConfig(String loadoutName, HashMap<Integer, ItemStack> loadout, ConfigurationSection section) {
		ConfigurationSection loadoutSection = section.createSection(loadoutName);
		
		if (loadoutSection != null) {
			loadoutSection.set("slots", toIntList(loadout.keySet()));
			for (Integer slot : loadout.keySet()) {
				ConfigurationSection slotSection = loadoutSection.createSection(slot.toString());
				ItemStack stack = loadout.get(slot);
				
				slotSection.set("id", stack.getTypeId());
				slotSection.set("data", stack.getData().getData());
				slotSection.set("amount", stack.getAmount());
				slotSection.set("durability", stack.getDurability());
	
				if (stack.getEnchantments().keySet().size() > 0) {
					List<String> enchantmentStringList = new ArrayList<String>();
					for (Enchantment enchantment : stack.getEnchantments().keySet()) {
						int level = stack.getEnchantments().get(enchantment);
						enchantmentStringList.add(enchantment.getId() + "," + level);
					}
					slotSection.set("enchantments", enchantmentStringList);
				}
                                if (stack.hasItemMeta() && stack.getItemMeta() instanceof LeatherArmorMeta 
                                        && ((LeatherArmorMeta)stack.getItemMeta()).getColor() != null) {
                                    LeatherArmorMeta meta = (LeatherArmorMeta)stack.getItemMeta();
                                    int rgb = meta.getColor().asRGB();
                                    slotSection.set("armorcolor", rgb);
                                }
                                if (stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()) {
                                    ItemMeta meta = stack.getItemMeta();
                                    slotSection.set("name", meta.getDisplayName());
                                }
                                if (stack.hasItemMeta() && stack.getItemMeta().hasLore()) {
                                    ItemMeta meta = stack.getItemMeta();
                                    slotSection.set("lore", meta.getLore());
                                }
			}
		}
	}
}
