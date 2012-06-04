package com.tommytony.war.utility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import com.tommytony.war.War;

/**
 * Change permissions on players during war matches.
 * @author Connor Monahan, tommytony
 */
public class ModifyPermissions {
	private static HashMap<Player, List<String>> addedPermissions = new HashMap<Player, List<String>>();
	private static HashMap<Player, List<String>> removedPermissions = new HashMap<Player, List<String>>();
	private static HashMap<Player, PermissionAttachment> attachments = new HashMap<Player, PermissionAttachment>();

	public static void applyModifications(Player player) {
		if (attachments.containsKey(player)) {
			clearModifications(player);
		}
		PermissionAttachment attachment = player.addAttachment(War.war);
		attachments.put(player, attachment);

		List<String> permissionsThatGotAdded = new ArrayList<String>();
		List<String> permissionsThatGotRemoved = new ArrayList<String>();

		// add
		for (String permission : War.war.getAddPermissions()) {
			if (!player.hasPermission(permission)) {
				attachment.setPermission(permission, true);
				permissionsThatGotAdded.add(permission);
			}
		}

		addedPermissions.put(player, permissionsThatGotAdded);

		// remove
		for (String permission : War.war.getRemovePermissions()) {
			if (player.hasPermission(permission)) {
				attachment.setPermission(permission, false);
				permissionsThatGotRemoved.add(permission);
			}
		}

		removedPermissions.put(player, permissionsThatGotRemoved);
	}


	public static void clearModifications(Player player) {
		if (attachments.containsKey(player)) {
			PermissionAttachment attachment = attachments.get(player);
			applyOppositeModifications(player);
			player.removeAttachment(attachment);
			attachments.remove(player);
		}
	}

	public static void clearAllModifications() {
		for (Player player : attachments.keySet()) {
			clearModifications(player);
		}
		attachments.clear();
	}


	private static void applyOppositeModifications(Player player) {
		if (attachments.containsKey(player)) {
			PermissionAttachment attachment = attachments.get(player);

			for (String permission : addedPermissions.get(player)) {
				attachment.unsetPermission(permission);
			}
			addedPermissions.remove(player);

			for (String permission : removedPermissions.get(player)) {
				attachment.unsetPermission(permission);
			}
			removedPermissions.remove(player);
		}
	}
}
