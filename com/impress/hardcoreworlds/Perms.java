package com.impress.hardcoreworlds;

//import org.bukkit.World;
import org.bukkit.entity.Player;

public class Perms {
	Player player;
	String[] oldPermissionGroups;
	
	public Perms(Player player, String[] oldPermissionGroups) throws IllegalArgumentException {
		this.player = player;
		this.oldPermissionGroups = oldPermissionGroups;
		if (!valid()) throw new IllegalArgumentException("Null values are not allowed!");
	}
	private boolean valid() {
		if (player == null || oldPermissionGroups == null)
			return false;
		for (String group : oldPermissionGroups)
			if (group == null || group.isEmpty())
				return false;
		return true;
	}
}