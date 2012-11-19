package com.impress.hardcoreworlds;

import java.io.File;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import com.impress.hardcoreworlds.Utilities.CalendarTools;
import com.impress.hardcoreworlds.Utilities.FileTools;

public class Bans implements Listener {
	private File bansFile, livesFile;
	private Logger log;
	private final String noBanPerm, infLivesPerm;
	private List<World> worlds;
	private Location spawn;
	private boolean deathBan, tempBan, removeDrops, debug;
	private int tempBanTime;
	protected static boolean regen, infLives;
	protected static int maxLives, regenTime, regenValue, regenTrigger;
	HashMap<String, Calendar> bans;
	HashMap<String, PlayerLives> lives;
	public Bans(HardcoreWorlds plugin, FileConfiguration config) { // TODO use config
		if (plugin == null) throw new IllegalArgumentException("plugin cannot be null");
		worlds = plugin.worlds;
		this.spawn = plugin.banTp;
		bansFile = plugin.bansFile;
		livesFile = plugin.livesFile;
		log = plugin.getLogger();
		debug = plugin.debug;
		noBanPerm = plugin.basePerm + "bypass.ban";
		infLivesPerm = plugin.basePerm + "bypass.lives";
		
		deathBan = plugin.deathBan;
		tempBan = plugin.tempBan;
		tempBanTime = plugin.tempBanTime;
		
		infLives = plugin.maxLives < 0;
		if (!infLives) {
			maxLives = plugin.maxLives;
			regen = plugin.lifeRegenTime > 0;
			regenTime = plugin.lifeRegenTime;
			regenValue = plugin.lifeRegenValue;
			regenTrigger = plugin.lifeRegenTrigger;
		} else {
			maxLives = 0;
			regen = false;
		}
//		lastRegen = plugin.lastLifeRegenTime;
		
		removeDrops = plugin.removeDrops;
		
		if (bansFile != null && bansFile.isFile())
			bans = FileTools.loadHashMap(bansFile, new HashMap<String, Calendar>());
		if (bans == null) {
			File corrupt = new File(bansFile.getPath() + ".corrupt");
			if (bansFile.renameTo(corrupt))
				log.warning(String.format("Error loading bans file, file may be corrupt. Renamed to \"%s\"", corrupt.getName()));
			bans = new HashMap<String, Calendar>();
		}
		if (livesFile != null && livesFile.isFile())
			lives = FileTools.loadHashMap(livesFile, new HashMap<String, PlayerLives>());
		if (lives == null) {
			File corrupt = new File(livesFile.getPath() + ".corrupt");
			if (livesFile.renameTo(corrupt))
				log.warning(String.format("Error loading lives file, file may be corrupt. Renamed to \"%s\"", corrupt.getName()));
			lives = new HashMap<String, PlayerLives>();
		}
	}
	
	@EventHandler (priority = EventPriority.LOW)
	public void onJoin(PlayerJoinEvent e) {
		if (worlds.contains(e.getPlayer().getWorld()) && !isAllowed(e.getPlayer()) && spawn != null) {
			e.getPlayer().teleport(spawn, TeleportCause.PLUGIN);
		}
	}
	@EventHandler (priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onTp(PlayerTeleportEvent e) {
		if (worlds.contains(e.getTo().getWorld()) && !isAllowed(e.getPlayer())) {
			e.setCancelled(true);
			if (isPlayerBanned(e.getPlayer()))
				e.getPlayer().sendMessage("You are banned from hardcore");
			else e.getPlayer().sendMessage("You have no lives left");
		}
	}
	@EventHandler
	public void onDeath(PlayerDeathEvent e) {
		if (worlds.contains(e.getEntity().getWorld())) {
			Player player = e.getEntity();
			if (removeDrops)
				e.getDrops().clear();
			if (deathBan) {
				ban(e.getEntity(), tempBan, tempBanTime, false);
			}
			loseLife(player);
		}
	}
	@EventHandler
	public void onRespawn(PlayerRespawnEvent e) {
		if (worlds.contains(e.getRespawnLocation().getWorld()) && !isAllowed(e.getPlayer()))
			e.setRespawnLocation(spawn);
	}
	
	void ban(String player, boolean temp, int time) {
		if (temp) {
			Calendar unbanTime = Calendar.getInstance();
			unbanTime.add(Calendar.MINUTE, time);
			bans.put(player, unbanTime);
		}
		else bans.put(player, null);
	}
	void ban (Player player, boolean temp, int time, boolean teleport) {
		if (player.hasPermission(noBanPerm)) return;
		ban(player.getName(), temp, time);
		if (teleport && worlds.contains(player.getWorld()))
			player.teleport(spawn, TeleportCause.PLUGIN);
	}
	boolean unban(String player) {
		if (isPlayerBanned(player)) {
			bans.remove(player);
			save();
			return true;
		} else return false;
	}
	boolean isAllowed(Player player) {
		return !isPlayerBanned(player) && hasLives(player);
	}
	boolean isPlayerBanned(Player player) {
		if (player.hasPermission(noBanPerm)) return false;
		return isPlayerBanned(player.getName());
	}
	boolean isPlayerBanned(String player) {
		if (bans.containsKey(player)) {
			Calendar banTime = bans.get(player);
			if (banTime == null || banTime.after(Calendar.getInstance()))
				return true;
			else {
				bans.remove(player);
				save();
				return false;
			}
		}
		else return false;
	}
	boolean hasLives(Player player) {
		if (infLives || player.hasPermission(infLivesPerm)) return true;
		return hasLives(player.getName());
	}
	boolean hasLives(String player) {
		if (!lives.containsKey(player))
			newPlayerLives(player);
		return lives.get(player).hasLives();
	}
	void loseLife(Player player) {
		if (infLives || player.hasPermission(infLivesPerm)) return;
		String name = player.getName();
		if (!lives.containsKey(name))
			newPlayerLives(name);
		lives.get(name).loseLife();
	}
	int getLives(String player) {
		if (!lives.containsKey(player))
			newPlayerLives(player);
		return lives.get(player).lives;
	}
	void newPlayerLives(String player) {
		lives.put(player, new PlayerLives(maxLives));
	}
	String getInfo(String player, boolean self) {
		if (lives.containsKey(player))
			lives.get(player).update();
		else if (self) {
			newPlayerLives(player);
			System.out.println("new PlayerLives created");
		}
		else return "Player not found!";
		String banStatus = (self? "You are" : player + " is") + (isPlayerBanned(player)? "" : " not") + " banned from hardcore worlds";
		String livesLeft = "";
		String regenStatus = "";
		if (!infLives)
			livesLeft = "\n" + (self? "You have " : player + " has ") + getLives(player) + " lives left";
		if (lives.get(player).regenerating)
			regenStatus = "\n" + (self? "You" : player) + " will regenerate " + regenValue + ((regenValue == 1)? " life" : " lives")
					+ " in " + CalendarTools.timeUntil(CalendarTools.cloneAdd(lives.get(player).regenUpdate, Calendar.MINUTE, regenTime));
		return banStatus + livesLeft + regenStatus;
	}
	void save() {
		if (bansFile == null) return;
		
		if (bans != null)
			if (!FileTools.saveHashMap(bans, bansFile) && debug)
				log.warning("Bans file could not be saved. File could be corrupted");
		if (bans == null || bans.isEmpty())
			bansFile.deleteOnExit();
			// Doesn't work for some reason. Neither does file.delete(). This doesn't change any functionality
		
		if (lives != null)
			if (!FileTools.saveHashMap(lives, livesFile) && debug)
				log.warning("Bans file could not be saved. File could be corrupted");
		if (lives == null || lives.isEmpty())
			livesFile.deleteOnExit();
	}
	void clearBans(boolean permanent, boolean temp) {
		if (!permanent && !temp) return;
		else if (permanent && temp) {
			bans.clear();
			return;
		} else {
			for (String player : bans.keySet().toArray(new String[0]))
				if (bans.get(player) == null) {
					if (permanent) bans.remove(player);
				} else if (temp) bans.remove(player);
		}
	}
	void cleanUp() {
		Calendar now = Calendar.getInstance();
		for (String player : bans.keySet().toArray(new String[0])) {
			Calendar time = bans.get(player);
			if (time != null && time.before(now))
				bans.remove(player);
		}
		for (PlayerLives player : lives.values().toArray(new PlayerLives[0]))
			player.update();
	}
}