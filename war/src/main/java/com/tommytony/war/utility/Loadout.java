package com.tommytony.war.utility;

import com.tommytony.war.War;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.Color;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.material.MaterialData;

/**
 * Represents a loadout of items
 *
 * @author cmastudios
 */
public class Loadout implements Comparable, ConfigurationSerializable {

	private String name;
	private HashMap<Integer, ItemStack> contents;
	private String permission;

	public Loadout(String name, HashMap<Integer, ItemStack> contents, String permission) {
		this.name = name;
		this.contents = contents;
		this.permission = permission;
	}

	static {
		ConfigurationSerialization.registerClass(Loadout.class);
	}

	public int compareTo(Object o) {
		if (!(o instanceof Loadout)) {
			throw new ClassCastException(this.getClass().getCanonicalName()
					+ " is not comparable to a " + o.getClass().getCanonicalName());
		}
		Loadout ldt = (Loadout) o;
		if ("default".equals(ldt.getName()) && !"default".equals(this.getName())) {
			return -1;
		} else if ("default".equals(this.getName()) && !"default".equals(ldt.getName())) {
			return 1;
		} else {
			return 0;
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public HashMap<Integer, ItemStack> getContents() {
		return contents;
	}

	public void setContents(HashMap<Integer, ItemStack> contents) {
		this.contents = contents;
	}

	public String getPermission() {
		return permission;
	}

	public void setPermission(String permission) {
		this.permission = permission;
	}

	public boolean requiresPermission() {
		return permission != null && !permission.isEmpty();
	}

	private List<Integer> toIntList(Set<Integer> keySet) {
		List<Integer> list = new ArrayList<Integer>();
		for (Integer key : keySet) {
			list.add(key);
		}
		return list;
	}

	public static HashMap<String, HashMap<Integer, ItemStack>> toLegacyFormat(List<Loadout> loadouts) {
		HashMap<String, HashMap<Integer, ItemStack>> oldLoadouts = new HashMap();
		for (Loadout ldt : loadouts) {
			oldLoadouts.put(ldt.getName(), ldt.getContents());
		}
		return oldLoadouts;
	}

	public static Loadout getLoadout(List<Loadout> loadouts, String name) {
		for (Loadout ldt : loadouts) {
			if (ldt.getName().equals(name)) {
				return ldt;
			}
		}
		return null;
	}

	// For future use
	public Map<String, Object> serialize() {
		Map<String, Object> config = new HashMap();
		config.put("slots", this.toIntList(contents.keySet()));
		for (Integer slot : contents.keySet()) {
			Map<String, Object> slotConfig = new HashMap();
			ItemStack stack = contents.get(slot);
			slotConfig.put("id", stack.getTypeId());
			slotConfig.put("data", stack.getData().getData());
			slotConfig.put("amount", stack.getAmount());
			slotConfig.put("durability", stack.getDurability());

			if (stack.getEnchantments().keySet().size() > 0) {
				List<String> enchantmentStringList = new ArrayList<String>();
				for (Enchantment enchantment : stack.getEnchantments().keySet()) {
					int level = stack.getEnchantments().get(enchantment);
					enchantmentStringList.add(enchantment.getId() + "," + level);
				}
				slotConfig.put("enchantments", enchantmentStringList);
			}
			if (stack.hasItemMeta() && stack.getItemMeta() instanceof LeatherArmorMeta
					&& ((LeatherArmorMeta) stack.getItemMeta()).getColor() != null) {
				LeatherArmorMeta meta = (LeatherArmorMeta) stack.getItemMeta();
				int rgb = meta.getColor().asRGB();
				slotConfig.put("armorcolor", rgb);
			}
			if (stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()) {
				ItemMeta meta = stack.getItemMeta();
				slotConfig.put("name", meta.getDisplayName());
			}
			if (stack.hasItemMeta() && stack.getItemMeta().hasLore()) {
				ItemMeta meta = stack.getItemMeta();
				slotConfig.put("lore", meta.getLore());
			}
			config.put(slot.toString(), slotConfig);
		}
		config.put("permission", permission);
		return config;
	}

	public static Loadout deserialize(Map<String, Object> config) {
		HashMap<Integer, ItemStack> contents = new HashMap();
		List<Integer> slots = (List<Integer>) config.get("slots");
		for (Integer slot : slots) {
			Map<String, Object> slotConfig = (Map<String, Object>) config.get(slot.toString());
			int id = (Integer) slotConfig.get("id");
			byte data = (Byte) slotConfig.get("data");
			int amount = (Integer) slotConfig.get("amount");
			short durability = (Short) slotConfig.get("durability");

			ItemStack stack = new ItemStack(id, amount, durability);
			stack.setData(new MaterialData(id, data));

			if (slotConfig.containsKey("enchantments")) {
				List<String> enchantmentStringList = (List<String>) slotConfig.get("enchantments");
				for (String enchantmentString : enchantmentStringList) {
					String[] enchantmentStringSplit = enchantmentString.split(",");
					if (enchantmentStringSplit.length == 2) {
						int enchantId = Integer.parseInt(enchantmentStringSplit[0]);
						int level = Integer.parseInt(enchantmentStringSplit[1]);
						War.war.safelyEnchant(stack, Enchantment.getById(enchantId), level);
					}
				}
			}
			if (slotConfig.containsKey("armorcolor")) {
				int rgb = (Integer) slotConfig.get("armorcolor");
				Color clr = Color.fromRGB(rgb);
				LeatherArmorMeta meta = (LeatherArmorMeta) stack.getItemMeta();
				meta.setColor(clr);
				stack.setItemMeta(meta);
			}
			if (slotConfig.containsKey("name")) {
				String itemName = (String) slotConfig.get("name");
				ItemMeta meta = stack.getItemMeta();
				meta.setDisplayName(itemName);
				stack.setItemMeta(meta);
			}
			if (slotConfig.containsKey("lore")) {
				List<String> itemLore = (List<String>) slotConfig.get("lore");
				ItemMeta meta = stack.getItemMeta();
				meta.setLore(itemLore);
				stack.setItemMeta(meta);
			}
			contents.put(slot, stack);
		}
		String permission = "";
		if (config.containsKey("permission")) {
			permission = (String) config.get("permission");
		}
		return new Loadout(null, contents, permission);
	}
}
