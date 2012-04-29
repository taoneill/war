package com.tommytony.war.utility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import com.tommytony.war.War;

/**
 * 
 * @author cmastudios
 * 
 */
public class ModifyPermissions {
	private static HashMap<Player, List<String>> permissions = new HashMap<Player, List<String>>();
	private static HashMap<Player, PermissionAttachment> attachments = new HashMap<Player, PermissionAttachment>();

	public static void removePermissions(Player player) {
		if (attachments.containsKey(player)) {
			removeAttachment(player);
		}
		PermissionAttachment attachment = player.addAttachment(War.war);
		attachments.put(player, attachment);
		List<String> permissionsToReAdd = new ArrayList<String>();
		for (String permission : War.war.getRemovePermissions()) {
			if (player.hasPermission(permission)) {
				permissionsToReAdd.add(permission);
				attachment.setPermission(permission, false);
			}
		}
		permissions.put(player, permissionsToReAdd);
	}

	public static void readdPermissions(Player player) {
		if (attachments.containsKey(player)) {
			PermissionAttachment attachment = attachments.get(player);
			for (String permission : permissions.get(player)) {
				attachment.setPermission(permission, true);
			}
			permissions.remove(player);
		}
	}

	public static void removeAttachment(Player player) {
		if (attachments.containsKey(player)) {
			PermissionAttachment attachment = attachments.get(player);
			readdPermissions(player);
			player.removeAttachment(attachment);
			attachments.remove(player);
		}
	}

	public static void removeAttachments() {
		for (Player player : attachments.keySet()) {
			removeAttachment(player);
		}
		attachments.clear();
	}
}
