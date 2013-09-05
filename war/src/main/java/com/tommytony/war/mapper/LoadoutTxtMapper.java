package com.tommytony.war.mapper;

import java.util.HashMap;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import com.tommytony.war.War;

public class LoadoutTxtMapper {
	
	public static String fromLoadoutToString(HashMap<Integer, ItemStack> loadout) {
		String loadoutString = "";
		for (Integer slot : loadout.keySet()) {
			ItemStack item = loadout.get(slot);
			if (item != null) {
				loadoutString += item.getTypeId() + "," + item.getAmount() + "," + slot + "," + item.getDurability() + "," + item.getData().getData();
				if (item.getEnchantments().keySet().size() > 0) {
					String enchantmentsStr = "";
					for (Enchantment enchantment : item.getEnchantments().keySet()) {
						enchantmentsStr += enchantment.getId() + ":" + item.getEnchantments().get(enchantment) + "::";
					}
					loadoutString += "," + enchantmentsStr;
				}
			}
			loadoutString += ";";
		}
		return loadoutString;
	}
	
	public static void fromStringToLoadout(String loadoutString, HashMap<Integer, ItemStack> destinationLoadout) {
		String[] rewardStrSplit = loadoutString.split(";");
		destinationLoadout.clear();
		for (String itemStr : rewardStrSplit) {
			if (itemStr != null && !itemStr.equals("")) {
				String[] itemStrSplit = itemStr.split(",");
				ItemStack item = null;
				if (itemStrSplit.length == 3) {
					item = new ItemStack(Integer.parseInt(itemStrSplit[0]), Integer.parseInt(itemStrSplit[1]));
				} else if (itemStrSplit.length == 5) {
					short durability = Short.parseShort(itemStrSplit[3]);
					item = new ItemStack(Integer.parseInt(itemStrSplit[0]), Integer.parseInt(itemStrSplit[1]), durability);
					item.setDurability(durability);
				} else if (itemStrSplit.length == 6) {
					short durability = Short.parseShort(itemStrSplit[3]);
					item = new ItemStack(Integer.parseInt(itemStrSplit[0]), Integer.parseInt(itemStrSplit[1]), durability);
					item.setDurability(durability);
					
					// enchantments
					String[] enchantmentsSplit = itemStrSplit[5].split("::");
					for (String enchantmentStr : enchantmentsSplit) {
						if (!enchantmentStr.equals("")) {
							String[] enchantmentSplit = enchantmentStr.split(":");
							int enchantId = Integer.parseInt(enchantmentSplit[0]);
							int level = Integer.parseInt(enchantmentSplit[1]);
							War.war.safelyEnchant(item, Enchantment.getById(enchantId), level);
						}
					}
				}
				destinationLoadout.put(Integer.parseInt(itemStrSplit[2]), item);
			}
		}
	}
}
