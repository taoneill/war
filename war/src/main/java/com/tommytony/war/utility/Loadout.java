package com.tommytony.war.utility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.inventory.ItemStack;

/**
 * Represents a loadout of items
 *
 * @author cmastudios
 */
public class Loadout implements Comparable<Loadout>, ConfigurationSerializable {

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

	public int compareTo(Loadout ldt) {
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
		HashMap<String, HashMap<Integer, ItemStack>> oldLoadouts = new HashMap<String, HashMap<Integer, ItemStack>>();
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
		Map<String, Object> config = new HashMap<String, Object>();
		config.put("slots", this.toIntList(contents.keySet()));
		for (Integer slot : contents.keySet()) {
			ItemStack stack = contents.get(slot);
			config.put(slot.toString(), stack.serialize());
		}
		config.put("permission", permission);
		return config;
	}

	@SuppressWarnings("unchecked")
	public static Loadout deserialize(Map<String, Object> config) {
		HashMap<Integer, ItemStack> contents = new HashMap<Integer, ItemStack>();
		List<Integer> slots = (List<Integer>) config.get("slots");
		for (Integer slot : slots) {
			contents.put(slot, ItemStack.deserialize((Map<String, Object>) config.get(slot.toString())));
		}
		String permission = "";
		if (config.containsKey("permission")) {
			permission = (String) config.get("permission");
		}
		return new Loadout(null, contents, permission);
	}
}
