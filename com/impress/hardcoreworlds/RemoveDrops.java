package com.impress.hardcoreworlds;

import java.util.List;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class RemoveDrops implements Listener {
	private List<World> worlds;
	RemoveDrops(HardcoreWorlds plugin) {
		if (plugin == null) throw new IllegalArgumentException("plugin cannot be null");
		worlds = plugin.worlds;
	}
	@EventHandler
	public void onDeath(PlayerDeathEvent e) {
		if (worlds.contains(e.getEntity().getWorld()))
			e.getDrops().clear();
	}
}