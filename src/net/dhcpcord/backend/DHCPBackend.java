package net.dhcpcord.backend;

import java.net.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import net.dhcpcord.backend.errors.*;

public class DHCPBackend {
	
	private static final String[] IP_RANGES = {"192.168.%d.%d", "10.0.%d.%d"};
	private static HashMap<String, HashMap<String, String>> cache = new HashMap<>();
	private static HashMap<String, ArrayList<String>> freedIps = new HashMap<>();
	
	public static void main(String[] args) throws Exception{
		boolean crashed = false;
		ServerSocket server = new ServerSocket(47606);
		System.out.println("Started DHCP server on " + server.getInetAddress().getHostAddress() + ":" + server.getLocalPort());
		if(!initCache()) {
			System.err.println("WARNING: Failed to initialize cache");
		}
		while(true) {
			try {
				Socket conn = server.accept();
				System.out.println("Received connection from " + conn.getRemoteSocketAddress());
				PrintWriter output = new PrintWriter(conn.getOutputStream());
				BufferedReader input = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				output.println();
				output.flush();
				crashed = false;
				String cmd, intent, entity, guild, user = null;
				String[] cmdParsed = null;
				System.out.println("Waiting for requests...");
				while((cmd = input.readLine()) != null) {
					System.out.println("Received request: " + cmd);
					try {
						cmdParsed = cmd.split(" ");
						intent = cmdParsed[0];
						System.out.println("Intent: " + intent);
						if(intent.equals("PING")) {
							output.println("Pong!");
							continue;
						}
						entity = cmdParsed[1];
						System.out.println("Entity: " + entity);
						guild = cmdParsed[2];
						System.out.println("Guild: " + guild);
						user = cmdParsed[3];
						System.out.println("User: " + user);
						if(intent.equals("GET")) {
							if(entity.equals("IP")) {
								output.println(getIp(guild, user));
							}
							else if(entity.equals("USER")) {
								try {
									output.println(getUser(guild, user)); //In this case, the "user" is actually an IP address instead of an ID
								}
								catch(Exception e) {
									output.println(Errors.ERR_IP_UNREG + " " + e.getMessage());
								}
							}
							else if(entity.equals("SERVICE")) {
								output.println(Errors.ERR_IMPLEMENT + " Not Implemented");
							}
							else {
								output.println(Errors.ERR_SYNTAX + " Unknown entity: " + entity);
							}
							output.flush();
							continue;
						}
						if(intent.equals("FLUSH")){
							if(entity.equals("IP")) {
								try {
									flush(guild);
									output.println("true");
								}
								catch(Exception e) {
									output.println(Errors.ERR_FLUSH + " " + e);
								}
							}
							else if(entity.equals("SERVICE")) {
								output.println(Errors.ERR_IMPLEMENT + " Not Implemented");
							}
							else if(entity.equals("USER")) {
								output.println(Errors.ERR_IMPLEMENT + " 'FLUSH' operator not defined for entity 'USER'");
							}
							else {
								output.println(Errors.ERR_SYNTAX + " Unknown entity: " + entity);
							}
							output.flush();
							continue;
						}
						if(intent.equals("SET")) {
							if(entity.equals("IP")) {
								try {
									setIp(guild, user, cmdParsed[4], true);
									output.println("SUCCESS");
								}
								catch(Exception e) {
									if(e instanceof ArrayIndexOutOfBoundsException) {
										output.println(Errors.ERR_ARGS + " Missing argument: ip. Syntax: SET IP guild user ip");
									}
									else {
										output.println(Errors.ERR_UNKNOWN + " " + e);
									}
								}
							}
							else if(entity.equals("SERVICE")) {
								output.println(Errors.ERR_IMPLEMENT + " Not Implemented");
							}
							else if(entity.equals("USER")) {
								output.println(Errors.ERR_IMPLEMENT + " 'SET' operator not defined for entity 'USER'");
							}
							else {
								output.println(Errors.ERR_SYNTAX + " Unknown entity: " + entity);
							}
							output.flush();
							continue;
						}
						if(intent.equals("ASSIGN")) {
							if(entity.equals("IP")) {
								try {
									output.println(assignIp(guild, user));
								}
								catch(Exception e) {
									output.println(Errors.ERR_UNKNOWN + " " + e);
								}
							}
							else if(entity.equals("SERVICE")) {
								output.println(Errors.ERR_IMPLEMENT + " 'ASSIGN' operator not defined for entity 'SERVICE'");
							}
							else if(entity.equals("USER")) {
								output.println(Errors.ERR_IMPLEMENT + " 'ASSIGN' operator not defined for entity 'USER'");
							}
							else {
								output.println(Errors.ERR_SYNTAX + " Unknown entity: " + entity);
							}
							output.flush();
							continue;
						}
						if(intent.equals("ASSIGNBULK")) {
							if(entity.equals("IP")) {
								try {
									assignIPBulk(guild, user);
									output.println();
								}
								catch(Exception e) {
									output.println(Errors.ERR_IP_ASSIGN + " " + e);
								}
							}
							else if(entity.equals("SERVICE")) {
								output.println(Errors.ERR_IMPLEMENT + " 'ASSIGNBULK' operator not defined for entity 'SERVICE'");
							}
							else if(entity.equals("USER")) {
								output.println(Errors.ERR_IMPLEMENT + " 'ASSIGNBULK' operator not defined for entity 'USER'");
							}
							else {
								output.println(Errors.ERR_SYNTAX + " Unknown entity: " + entity);
							}
							output.flush();
							continue;
						}
						if(intent.equals("RELEASE") || intent.equals("REMOVE")) {
							if(entity.equals("IP")) {
								try {
									release(guild, user, true);
									output.println("true");
								}
								catch(Exception e) {
									output.println(Errors.ERR_UNKNOWN + " " + e);
								}
							}
							else if(entity.equals("SERVICE")) {
								output.println(Errors.ERR_IMPLEMENT + " Not Implemented");
							}
							else {
								output.println(Errors.ERR_SYNTAX + " Unknown entity: " + entity);
							}
							output.flush();
							continue;
						}
					}
					catch(Exception e) {
						e.printStackTrace();
						if(e instanceof ArrayIndexOutOfBoundsException) {
							String arg;
							switch(e.getMessage()) {
							case "0": arg = "intent"; break;
							case "1": arg = "entity"; break;
							case "2": arg = "guild"; break;
							case "3": arg = "user"; break;
							default: arg = "unknown";
							}
							output.println(Errors.ERR_ARGS + " Missing argument: " + arg);
						}
						else {
							output.println(Errors.ERR_UNKNOWN + " " + e.getMessage());
						}
					}
					output.flush();
				}
				System.out.println("Connection closed");
				
			}
			catch(Exception e) {
				System.out.println("An error occured:");
				e.printStackTrace();
				if(crashed) {
					server.close();
					System.exit(1);
				}
				crashed = true;
			}
		}
	}
	private static void createFolder(String guild) {
		new File("dhcp/" + guild).mkdirs();
	}
	private static boolean initCache() {
		System.out.println("Loading cache...");
		try {
			File file = new File("dhcp/");
			if(!file.exists()) {
				System.out.println("Creating DHCP Directory...");
				file.mkdirs();
			}
			File[] dirs = file.listFiles();
			File[] tmp;
			for(File f : dirs) {
				if(f.isDirectory()) {
					System.out.println("Scanning " + f.getPath());
					tmp = f.listFiles();
					cache.put(f.getName(), new HashMap<>());
					for(File fl : tmp) {
						System.out.println("Found file " + fl.getName());
						System.out.println("IP: " + getIp(f.getName(), fl.getName()));
					}
				}
			}
		}
		catch(Exception e) {
			return false;
		}
		System.out.println("Done!");
		return true;
	}
	private static String getUser(String guild, String ip) throws Exception{
		System.out.println("Getting user associated with IP " + ip + "... (Guild: " + guild + ")");
		HashMap<String, String> guildCache = cache.get(guild);
		for(Map.Entry<String, String> entry : guildCache.entrySet()) {
			System.out.println(entry.getKey() + ":" + entry.getValue());
			if(entry.getValue().equals(ip)) {
				return entry.getKey();
			}
		}
		System.out.println("IP " + ip + " not registered");
		throw new Exception("IP not assigned to a user");
	}
	private static String getIp(String guild, String user) throws Exception {
		return getIp(guild, user, true);
	}
	private static String getIp(String guild, String user, boolean assign) throws Exception{
		HashMap<String, String> guildCache = cache.get(guild);
		String ip = "";
		if(!(guildCache == null) && guildCache.containsKey(user)) {
			return guildCache.get(user);
		}
		File file = new File("dhcp/" + guild);
		if(!file.exists()) {
			file.mkdirs();
			cache.put(guild, new HashMap<>());
		}
		file = new File("dhcp/" + guild + "/" + user);
		if(!file.exists() && assign) {
			return assignIp(guild, user);
		}
		try {
			Scanner sc = new Scanner(file);
			ip = sc.nextLine();
			sc.close();
			cache.get(guild).put(user, ip);
		}
		catch(FileNotFoundException e) {
			ip = null;
		}
		return ip;
	}
	private static void setIp(String guild, String user, String ip, boolean write) throws Exception{
		try {
			createFolder(guild);
		}
		catch(Exception e) {}
		cache.get(guild).put(user, ip);
		try {
			freedIps.remove(ip);
		}
		catch(Exception e) {}
		System.out.println("Assigned IP " + ip + " to user " + user);
		if(!write) {return;}
		File file = new File("dhcp/" + guild + "/" + user);
		FileWriter fw = new FileWriter(file);
		fw.write(ip);
		fw.close();
	}
	private static void assignIPBulk(String guild, String userStr) throws Exception{
		try {
			createFolder(guild);
		}
		catch(Exception e) {}
		String[] users = userStr.split(",");
		String ip = null;
		String range = IP_RANGES[(int)(Long.parseLong(guild) % 2L)];
		HashMap<String, String> guildCache = cache.get(guild);
		if(guildCache == null) {
			cache.put(guild, new HashMap<>());
			guildCache = new HashMap<>();
		}
		int complete = 0;
		int x = 0;
		while(x < 255*255 && complete < users.length) {
			if(complete == users.length) { //Just in case
				return;
			}
			if(!freedIps.get(guild).isEmpty()) {
				ip = freedIps.get(guild).remove(0);
				setIp(guild, users[complete], ip, true);
				complete++;
				continue;
			}
			ip = String.format(range, (x / 255), (x % 254) + 1);
			x++;
			if(!guildCache.containsValue(ip)) {
				setIp(guild, users[complete], ip, true);
				complete++;
			}
			if(complete == users.length) {
				return;
			}
		}
	}
	private static String assignIp(String guild, String user) throws Exception{
		try {
			createFolder(guild);
		}
		catch(Exception e) {}
		String ip = null;
		String range = IP_RANGES[(int)(Long.parseLong(guild) % 2L)];
		HashMap<String, String> guildCache = cache.get(guild);
		if(guildCache == null) {
			cache.put(guild, new HashMap<>());
			guildCache = new HashMap<>();
		}
		if(freedIps.get(guild) == null) {
			freedIps.put(guild, new ArrayList<>());
		}
		if(freedIps.get(guild).isEmpty()) {
			int x = 0;
			boolean brk = false;
			do {
				brk = true;
				ip = String.format(range, (x / 255), (x % 254) + 1);
				System.out.println("Trying IP " + ip + "...");
				for(Map.Entry<String, String> entry : guildCache.entrySet()) {
					if(entry.getValue().equals(ip)) {
						brk = false;
					}
				}
				if(brk) {
					break;
				}
				x++;
			}
			while(x < 255*255);
			if(ip == null) {
				throw new Exception(Errors.ERR_IP_ASSIGN + " IP range full for this guild!");
			}
		}
		else {
			ip = freedIps.get(guild).remove(0);
		}
		setIp(guild, user, ip, true);
		System.out.println(ip + " was free!");
		return ip;
	}
	private static void release(String guild, String user, boolean modify) throws Exception{
		File file = new File("dhcp/" + guild + "/" + user);
		file.delete();
		if(modify) {
			if(freedIps.get(guild) == null) {
				freedIps.put(guild, new ArrayList<>());
			}
			freedIps.get(guild).add(cache.get(guild).remove(user));
		}
	}
	private static void flush(String guild) throws Exception{
		HashMap<String, String> guildCache = cache.get(guild);
		for(Map.Entry<String, String> entry : guildCache.entrySet()) {
			System.out.println("Releasing " + entry);
			release(guild, entry.getKey(), true);
		}
		cache.remove(guild);
		System.out.println("Done!");
	}
}
