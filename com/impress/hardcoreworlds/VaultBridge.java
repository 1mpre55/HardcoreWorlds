package com.impress.hardcoreworlds;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import net.milkbowl.vault.permission.Permission;

public class VaultBridge {
	public static Permission perms = null;
	public VaultBridge(Server server) {
		setupPermissions(server);
	}
	private boolean setupPermissions(Server server) {
        RegisteredServiceProvider<Permission> rsp = server.getServicesManager().getRegistration(Permission.class);
        perms = rsp.getProvider();
        return perms != null;
    }
	String[] getGroups(Player player) {
		return perms.getPlayerGroups(player);
	}
	boolean clearGroups(Player player) {
		if (perms.getPlayerGroups(player) == null) return false;
		for (String group : perms.getPlayerGroups(player))
			perms.playerRemoveGroup(player, group);
		return true;
	}
	boolean addGroups(Player player, String[] groups) {
		if (groups == null) return false;
		boolean result = true;
		for (String group : groups)
			if (!perms.playerAddGroup(player, group)) result = false;
		return result;
	}
}