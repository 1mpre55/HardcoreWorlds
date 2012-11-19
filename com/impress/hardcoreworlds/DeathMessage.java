package com.impress.hardcoreworlds;

import java.util.List;

import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.EventPriority;

public class DeathMessage implements Listener {
	final String addOption = "custom-death-message";
	String add;
	boolean replace;
	Bans lives;
	private List<World> worlds;
	public DeathMessage(HardcoreWorlds plugin, Bans lives, FileConfiguration config) {
		worlds = plugin.worlds;
		add = config.getString(addOption);
		this.lives = lives;
		replace = lives != null && !Bans.infLives;
	}
	@EventHandler (priority = EventPriority.HIGH)
	public void onDeath(PlayerDeathEvent e) {
		try {
			if (worlds.contains(e.getEntity().getWorld())) {
				System.out.println("1");
				System.out.println(add);
				System.out.println("2");
				System.out.println("");
				String message = add.replaceAll("%original%", e.getDeathMessage()).replaceAll("%player%", e.getEntity().getName());
				if (replace) message = message.replaceAll("%lives%", "" + lives.lives.get(e.getEntity().getName()).lives);
				e.setDeathMessage(message);
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
}