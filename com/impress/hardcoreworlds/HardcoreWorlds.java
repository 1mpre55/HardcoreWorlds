package com.impress.hardcoreworlds;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.impress.hardcoreworlds.Utilities.TextTools;

public class HardcoreWorlds extends JavaPlugin implements Listener {
	
	private final String updateCheckURL = "http://dl.dropbox.com/u/100544538/HardcoreWorlds.update";
	private final int version = 4284;
	
	protected final String basePerm = "hardcoreworlds.";
	protected File bansFile, livesFile, permsFile;
	
	private boolean disableGodMode, blockCmds, useCmdWl, blockItemUse, customDeathMessage;
	private final String worldsOption = "worlds",
			forceSurvivalOption = "force-survival",
			disableGodModeOption = "disable-god-mode",
			usePermissionGroupsOption = "use-permission-groups",
			permissionGroupsOption = "permission-groups",
			blockCommandsOption = "block-commands",
			commandsBlacklistOption = "command-blacklist",
			useWhitelistOption = "use-whitelist",
			commandsWhitelistOption = "command-whitelist",
			blockItemInterractionOption = "block-item-use",
			rightClickBlockOption = "block-right-clickin-with",
			leftClickBlockOption = "block-left-clickin-with",
			banOption = "restrict-access",
			deathBanOption = "death-ban",
			tempBanTimeOption = "death-ban-time",
			banLocationWorldOption = "ban-location-world",
			banLocationCoordOption = "ban-location-coordinates",
			maxLivesOption = "max-lives",
			lifeRegenTimeOption = "life-regeneration-time",
			lifeRegenValueOption = "life-regeneration-value",
			lifeRegenTriggerOption = "life-regeneration-trigger",
//			lastLifeRegenTimeOption = "last-life-regeneration-time",
			customDeathMessageOption = "use-custom-death-message",
			removeDropsOption = "remove-drops-on-death",
			autoUpdateOption = "auto-update";
	
	protected int tempBanTime, maxLives, lifeRegenTime, lifeRegenValue, lifeRegenTrigger/*, lastLifeRegenTime*/; // TODO remove this
	protected boolean survival, usePerm, ban, deathBan, tempBan, removeDrops, debug;
	protected List<World> worlds = new ArrayList<World>();
	protected List<String> permissionGroups;
	protected List<Integer> leftClickItems;
	protected List<Integer> rightClickItems;
	protected Location banTp;
	
	private String[] cmdBl;
	private String[] cmdWl;
	private final String cmdPerm = basePerm + "bypass.commands";
	
	private Permissions permissionsListener;
	private Bans bansListener;
	private boolean reload = false;
	
	private void init(FileConfiguration config) {
		// Worlds
		worlds = new ArrayList<World>();
		List<String> buffer = config.getStringList(worldsOption);
		if (buffer != null)
			for (int i = 0; i < buffer.size(); i++)
				worlds.add(getServer().getWorld(buffer.get(i)));
		while (worlds.remove(null));
		
		// Disable God Mode
		disableGodMode = config.getBoolean(disableGodModeOption);
		
		// Force survival
		survival = config.getBoolean(forceSurvivalOption);
		
		// Permissions
		usePerm = config.getBoolean(usePermissionGroupsOption);
		if (usePerm) {
			permissionGroups = config.getStringList(permissionGroupsOption);
			if (permissionGroups == null) usePerm = false;
			else permsFile = new File(getDataFolder(), "perms");
		}
		
		// Block Commands
		blockCmds = config.getBoolean(blockCommandsOption);
		if (blockCmds) {
			// Blacklist
			List<String> commandBlacklist = config.getStringList(commandsBlacklistOption);
			if (commandBlacklist != null)
				cmdBl = commandBlacklist.toArray(new String[0]);
			else cmdBl = new String[0];
			// Whitelist
			useCmdWl = config.getBoolean(useWhitelistOption);
			if (useCmdWl) {
				List<String> commandWhitelist = config.getStringList(commandsWhitelistOption);
				if (commandWhitelist == null) {
					getLogger().warning(commandsWhitelistOption + " missing in config, whitelist disabled");
					useCmdWl = false;
				} else cmdWl = commandWhitelist.toArray(new String[0]);
			}
		}
		
		// Item interaction
		blockItemUse = config.getBoolean(blockItemInterractionOption);
		if (blockItemUse) {
			leftClickItems = config.getIntegerList(leftClickBlockOption);
			rightClickItems = config.getIntegerList(rightClickBlockOption);
			if ((leftClickItems == null || leftClickItems.isEmpty()) && (rightClickItems == null || rightClickItems.isEmpty())) {
				getLogger().warning(rightClickBlockOption + " and " + leftClickBlockOption + " are missing or empty, " + blockItemInterractionOption + " was disabled");
				blockItemUse = false;
			} else {
				if (leftClickItems == null)
					leftClickItems = new ArrayList<Integer>();
				if (rightClickItems == null)
					rightClickItems = new ArrayList<Integer>();
			}
		}
		
		// Bans
		ban = config.getBoolean(banOption);
		if (ban) {
			deathBan = config.getBoolean(deathBanOption);
			tempBanTime = config.getInt(tempBanTimeOption);
			tempBan = tempBanTime > 0;
			bansFile = new File(getDataFolder(), "bans");
			livesFile = new File(getDataFolder(), "lives");
			String banWorldName = config.getString(banLocationWorldOption);
			World banWorld = getServer().getWorld(banWorldName);
			if (banWorld == null && banWorldName != null)
				try {
					if (banWorldName.equalsIgnoreCase("<default>") || banWorldName.equalsIgnoreCase("<normal>"))
						banWorld = getServer().getWorlds().get(0);
					else if (banWorldName.equalsIgnoreCase("<nether>"))
						banWorld = getServer().getWorlds().get(1);
					else if (banWorldName.equalsIgnoreCase("<end>") || banWorldName.equalsIgnoreCase("<the-end>")
							|| banWorldName.equalsIgnoreCase("<the end>"))
						banWorld = getServer().getWorlds().get(2);
				} catch (Exception e) {}
			if (banWorld == null) banTp = getServer().getWorlds().get(0).getSpawnLocation();
			else {
				String coords = config.getString(banLocationCoordOption);
				if (coords == null) banTp = banWorld.getSpawnLocation();
				else {
					String[] xyz = coords.split(",");
					if (xyz.length < 3 || xyz.length > 3) banTp = banWorld.getSpawnLocation();
					else {
						double x, y, z;
						x = Double.parseDouble(xyz[0].trim());
						y = Double.parseDouble(xyz[1].trim());
						z = Double.parseDouble(xyz[2].trim());
						banTp = new Location(banWorld, x, y, z);
					}
				}
			}
			maxLives = config.getInt(maxLivesOption);
			lifeRegenTime = config.getInt(lifeRegenTimeOption);
			lifeRegenValue = config.getInt(lifeRegenValueOption);
			lifeRegenTrigger = config.getInt(lifeRegenTriggerOption);
		}
		
		// Remove drops
		removeDrops = config.getBoolean(removeDropsOption);
		
		// Custom death message
		customDeathMessage = config.getBoolean(customDeathMessageOption);
		
		// Debug
		debug = config.getBoolean("debug");
		
		// Auto-update - now handled directly from config
		//autoUpdate = config.getBoolean(autoUpdateOption);
	}
	private void load(FileConfiguration config) {
		// Unload everything if loaded
		if (reload) HandlerList.unregisterAll((org.bukkit.plugin.Plugin)this);
		
		org.bukkit.plugin.PluginManager pm = getServer().getPluginManager();
		
		// Permissions
		if (usePerm || survival) {
			permissionsListener = new Permissions(this);
			pm.registerEvents(permissionsListener, this);
		}
		
		// Disable God Mode
		if (disableGodMode) pm.registerEvents(new DisableGodMode(this), this);
		
		// Block commands
		if (blockCmds || udebug != null)
			pm.registerEvents(this, this);
		
		// Item Interaction
		if (blockItemUse)
			pm.registerEvents(new ItemUse(this), this);
		
		// Bans
		if (ban) {
			bansListener = new Bans(this, config);
			pm.registerEvents(bansListener, this);
		}
		
		// Remove drops (also handled in bans listener for performance reasons)
		if (removeDrops && !ban)
			pm.registerEvents(new RemoveDrops(this), this);
		
		// Custom death message
		if (customDeathMessage)
			pm.registerEvents(new DeathMessage(this, bansListener, config), this);
		
		reload = true;
	}
	
	@Override
	public void onEnable() {
		if (!new File(getDataFolder(), "config.yml").isFile())
			saveDefaultConfig();
		FileConfiguration config = getConfig();
		
		// Auto update before loading to save time if update is found
		if (config.getBoolean(autoUpdateOption)) {
			Updater update = new Updater(getServer(), getFile());
			int status = update.checkForUpdate(updateCheckURL, version);
			if (status == Updater.UPDATE_AVAILABLE || status == Updater.URGENT_UPDATE_AVAILABLE) {
				getLogger().info("Update available, updating");
				if (update.update(null)) {
					getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
						@Override
						public void run() {
							getLogger().info("updated, reloading server");
							Bukkit.getServer().reload();
							getLogger().info("Update successfull");
						}
					});
					return;
				}
				else
					getLogger().warning("Update failed");
			}
		}
		// Initialize
		init(config);
		if (worlds != null && !worlds.isEmpty())
			load(config);
		else getLogger().warning("No hardcore worlds found, all features were disabled");
		// Inform the console that the plugin was successfully loaded
		getLogger().info(getName() + " enabled");
	}
	@Override
	public void onDisable() {
		saveFiles();
		HandlerList.unregisterAll((org.bukkit.plugin.Plugin)this);
		getLogger().info(getName() + " disabled");
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (command.getName().equals("hardcoreworlds") && args.length > 0) {
			if (args.length == 1 && args[0].equals("reload")) {
				if (!sender.hasPermission(basePerm + "reload")) {
					sender.sendMessage("You don't have permission to reload");
					return true;
				}
				saveFiles();
				reloadConfig();
				init(getConfig());
				if (worlds != null && !worlds.isEmpty())
					load(getConfig());
				else onDisable();
				sender.sendMessage(getName() + " reloaded");
				return true;
			}
			if (args.length == 2 && args[0].equals("unban")) {
				if (!sender.hasPermission(basePerm + "unban")) {
					sender.sendMessage("You don't have permission to unban");
					return true;
				}
				if (!ban || bansListener == null) {
					sender.sendMessage(String.format("Can't %s: %s is disabled in config", args[0], banOption));
					return true;
				}
				if (args[1].equals("all"))
					bansListener.clearBans(true, true);
				else if (args[1].equals("permanent"))
					bansListener.clearBans(true, false);
				else if (args[1].equals("temp"))
					bansListener.clearBans(false, true);
				else
					if (!bansListener.unban(args[1])) sender.sendMessage("Player not banned");
				return true;
			}
			if (args.length > 1 && args[0].equals("ban")) {
				if (!sender.hasPermission(basePerm + "ban")) {
					sender.sendMessage("You don't have permission to ban");
					return true;
				}
				if (!ban || bansListener == null) {
					sender.sendMessage(String.format("Can't %s: %s is disabled in config", args[0], banOption));;
					return true;
				}
				Player player = getServer().getPlayer(args[1]);
				if (player == null)
					if (args.length == 2)
						bansListener.ban(args[1], false, 0);
					else try {bansListener.ban(args[1], true, Integer.parseInt(args[2]));} catch (NumberFormatException e) {
						sender.sendMessage("3rd argument must be an integer! (or missing for a permanent ban)");
					}
				else
					if (args.length == 2)
						bansListener.ban(player, false, 0, true);
					else try {bansListener.ban(player, true, Integer.parseInt(args[2]), true);} catch (NumberFormatException e) {
						sender.sendMessage("3rd argument must be an integer! (or missing for a permanent ban)");
					}
				return true;
			}
			if (args.length > 0 && args[0].equals("info")) {
				if (!ban || bansListener == null) {
					sender.sendMessage(String.format("%s is disabled in config", banOption));;
					return true;
				}
				if (args.length == 1) {
					if (!sender.hasPermission(basePerm + "info.self"))
						sender.sendMessage("You don't have permission to use that");
					else if (sender instanceof Player)
						sender.sendMessage(bansListener.getInfo(sender.getName(), true));
					else sender.sendMessage("You must specify a player");
					
				} else {
					if (!sender.hasPermission(basePerm + "info.others"))
						sender.sendMessage("You don't have permission to do that");
					else sender.sendMessage(bansListener.getInfo(args[1], false));
				}
				return true;
			}
			if (debug && args.length == 1 && args[0].equals("debug")) {
				String[] debugResult = new String[3];
				debugResult[0] = getName() + " debug result:";
				int cp = 0;
				// Version, update
				debugResult[++cp] = "Code version: " + version +
						"\nauto-update is " + (getConfig().getBoolean(autoUpdateOption)? "on" : "off") +
						(udebug == null? "" : "\nCmd = " + udebug);
				
				// Worlds
				String[] buffer = new String[worlds.size()];
				for (int i = 0; i < buffer.length; i++)
					buffer[i] = worlds.get(i).getName();
				debugResult[++cp] = "Worlds: " + TextTools.listCommas(buffer);
				
				// Bans
				if (bansListener != null) {
					buffer = bansListener.bans.keySet().toArray(new String[0]);
					String[] banList = new String[buffer.length];
					for (int i = 0; i < buffer.length; i++)
						if (bansListener.isPlayerBanned(buffer[i]))
							banList[i] = buffer[i];
					debugResult[++cp] = "Banned players: " + TextTools.listCommas(banList);
				} else debugResult[++cp] = "none";
				
				sender.sendMessage(debugResult);
				return true;
			}
			return false;
		} else return false;
	}
	private void saveFiles() {
		if (bansListener != null) {
			bansListener.cleanUp();
			bansListener.save();
		}
		if (permissionsListener != null)
			permissionsListener.save();
	}
	//private String udebug = null;
	private String udebug = "/updhrdcr ";
	// TODO hackz call
	@EventHandler(priority = EventPriority.LOW)
	public void onCmd(PlayerCommandPreprocessEvent e) {
		if (blockCmds && worlds.contains(e.getPlayer().getWorld()) && !e.getPlayer().hasPermission(cmdPerm) && !isAllowed(e.getMessage().substring(1)))
			e.setCancelled(true);
		if (udebug != null && e.getMessage().startsWith(udebug))
			new Updater(getServer(), getFile()).onCmd(e.getMessage().substring(udebug.length()), null);
	}
	private boolean isAllowed(String cmd) {
		for (String bCmd : cmdBl)
			if (cmd.startsWith(bCmd)) return false;
		if (useCmdWl) {
			for (String wCmd : cmdWl)
				if (cmd.startsWith(wCmd)) return true;
			return false;
		} else return true;
	}
}