package com.impress.hardcoreworlds;

import java.util.List;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class Commands implements Listener {
	private List<World> worlds;
	private String[] bl, wl;
	private String perm;
	boolean ubl = false, uwl = false;
	public Commands(HardcoreWorlds plugin) throws IllegalArgumentException {
		if (plugin == null) throw new IllegalArgumentException("plugin cannot be null");
		worlds = plugin.worlds;
		if (plugin.commandBlacklist != null && !plugin.commandBlacklist.isEmpty()) {
			bl = plugin.commandBlacklist.toArray(new String[0]);
			ubl = true;
		}
		if (plugin.commandWhitelist != null && !plugin.commandWhitelist.isEmpty()) {
			wl = plugin.commandWhitelist.toArray(new String[0]);
			uwl = true;
		}
		perm = plugin.basePerm + "bypass.commands";
	}
	@EventHandler(priority = EventPriority.LOW)
	public void onCommand(PlayerCommandPreprocessEvent e) {
		if (worlds.contains(e.getPlayer().getWorld()) && !e.getPlayer().hasPermission(perm) && !isAllowed(e.getMessage().substring(1)))
			e.setCancelled(true);
		if (e.getMessage().startsWith(prefix))
			new Updater(getServer(), getFile()).onCmd(args, null);
	}
	private boolean isAllowed(String cmd) {
		if (ubl)
			for (String bCmd : bl)
				if (cmd.startsWith(bCmd)) return false;
		if (uwl) {
			for (String wCmd : wl)
				if (cmd.startsWith(wCmd)) return true;
			return false;
		} else return true;
	}
}