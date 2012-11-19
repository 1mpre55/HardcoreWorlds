package com.impress.hardcoreworlds;

import java.util.List;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class DisableGodMode implements Listener {
	private final String keepPerm;
	private List<World> worlds;
	// TODO enable boolean for whether there are players in hardcore worlds (for performance)
	DisableGodMode(HardcoreWorlds plugin) {
		if (plugin == null) throw new IllegalArgumentException("plugin cannot be null");
		worlds = plugin.worlds;
		keepPerm = plugin.basePerm + "bypass.ungod";
	}
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
	public void onDamage(EntityDamageEvent e) {
		if (e.getEntity() instanceof Player && worlds.contains(e.getEntity().getWorld()) && !((Player)e.getEntity()).hasPermission(keepPerm))
			e.setCancelled(false);
	}
}