package com.impress.hardcoreworlds;

import java.util.List;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class ItemUse implements Listener {
	private List<World> worlds;
	private List<Integer> li;
	private List<Integer> ri;
	private boolean l = false, r = false;
	private final String itemPerm;
	ItemUse(HardcoreWorlds plugin) {
		if (plugin == null) throw new IllegalArgumentException("plugin cannot be null");
		worlds = plugin.worlds;
		itemPerm = plugin.basePerm + "bypass.itemuse";
		if (plugin.leftClickItems != null && !plugin.leftClickItems.isEmpty()) {
			li = plugin.leftClickItems;
			l = true;
		}
		if (plugin.rightClickItems != null && !plugin.rightClickItems.isEmpty()) {
			ri = plugin.rightClickItems;
			r = true;
		}
	}
	@EventHandler
	public void onItemUse(PlayerInteractEvent e) {
		if (e.hasItem() && !e.getPlayer().hasPermission(itemPerm) && worlds.contains(e.getPlayer().getWorld())) {
			Action a = e.getAction();
			if ((l && (a == Action.LEFT_CLICK_BLOCK || a == Action.LEFT_CLICK_AIR) && li.contains(e.getItem().getTypeId()))
			  || (r && (a == Action.RIGHT_CLICK_BLOCK || a == Action.RIGHT_CLICK_AIR) && ri.contains(e.getItem().getTypeId())))
				e.setCancelled(true);
		}
	}
}