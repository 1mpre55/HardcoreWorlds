package com.impress.hardcoreworlds;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.impress.hardcoreworlds.Utilities.FileTools;

public class Permissions implements Listener {
	private boolean surv, up, debug;
	
	private List<World> worlds;
	private HashMap<String, String[]> players;
	private List<String> pg;
	private VaultBridge perm;
	private File file;
	private Logger log;
	private final String groupsPerm, survPerm;
	Permissions(HardcoreWorlds plugin) {
		if (plugin == null) throw new IllegalArgumentException("plugin cannot be null");
		worlds = plugin.worlds;
		
		up = plugin.usePerm;
		surv = plugin.survival;
		if (up && plugin.permissionGroups != null)
			pg = plugin.permissionGroups;
		else up = false;
		log = plugin.getLogger();
		debug = plugin.debug;
		file = plugin.permsFile;
		groupsPerm = plugin.basePerm + "bypass.permissiongroups";
		survPerm = plugin.basePerm + "bypass.survival";
		if (up) {
			perm = new VaultBridge(plugin.getServer());
			if (perm == null && up) throw new IllegalArgumentException("cannot load Vault");
			if (file != null && file.isFile())
				try {
					players = FileTools.loadHashMap(file, new HashMap<String, String[]>());
				} catch (Exception e) {
					File corrupt = new File(file.getPath() + ".corrupt");
					file.renameTo(corrupt);
					log.warning(String.format("Error loading player permissions file, file may be corrupt. Renamed to \"%s\"", corrupt.getName()));
				}
			if (players == null) players = new HashMap<String, String[]>();
		}
	}
	@EventHandler (priority = EventPriority.HIGH)
	public void onJoin(PlayerJoinEvent e) {
		if (worlds.contains(e.getPlayer().getWorld())) {
			addPlayer(e.getPlayer());
		} else removePlayer(e.getPlayer());
	}
	@EventHandler
	public void onWorldChange(PlayerChangedWorldEvent e) {
		if (up && worlds.contains(e.getFrom()))
			removePlayer(e.getPlayer());
		if (worlds.contains(e.getPlayer().getWorld()))
			addPlayer(e.getPlayer());
	}
	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		if (up && worlds.contains(e.getPlayer().getWorld()))
			removePlayer(e.getPlayer());
	}
	private void addPlayer(Player player) {
		if (surv && !player.hasPermission(survPerm)) player.setGameMode(GameMode.SURVIVAL);
		if (up && !player.hasPermission(groupsPerm) && !players.containsKey(player.getName())) {
			players.put(player.getName(), perm.getGroups(player));
			perm.clearGroups(player);
			perm.addGroups(player, pg.toArray(new String[0]));
			save();
		}
	}
	private void removePlayer(Player player) {
		if (up && players.containsKey(player.getName())) {
			perm.clearGroups(player);
			perm.addGroups(player, players.get(player.getName()));
			players.remove(player.getName());
			save();
		}
	}
	void save() {
		if (players != null && !players.isEmpty() && file != null && !FileTools.saveHashMap(players, file) && debug && log != null)
			log.warning("Permissions file could not be saved. File could be corrupted");
	}
}