package net.dhcpcord.backend;

import java.net.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Scanner;

import net.dhcpcord.backend.errors.*;

public class DHCPBackend {
	
	private static final String[] IP_RANGES = {"192.168.%d.%d", "10.0.%d.%d"};
	private static HashMap<String, HashMap<String, String>> cache = new HashMap<>();
	
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
				crashed = false;
				String cmd, intent, entity, guild, user;
				String[] cmdParsed = null;
				while((cmd = input.readLine()) != null) {
					System.out.println("Received request: " + cmd);
					try {
						cmdParsed = cmd.split(" ");
						intent = cmdParsed[0];
						entity = cmdParsed[1];
						guild = cmdParsed[2];
						user = cmdParsed[3];
						if(intent.equals("GET")) {
							if(entity.equals("IP")) {
								output.println(getIp(guild, user));
							}
							else if(entity.equals("SERVICE")) {
								output.println(Errors.ERR_IMPLEMENT + " Not Implemented");
							}
							else {
								output.println(Errors.ERR_SYNTAX + " Unknown entity: " + entity);
							}
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
							else {
								output.println(Errors.ERR_SYNTAX + " Unknown entity: " + entity);
							}
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
							else {
								output.println(Errors.ERR_SYNTAX + " Unknown entity: " + entity);
							}
						}
						if(intent.equals("RELEASE") || intent.equals("REMOVE")) {
							if(entity.equals("IP")) {
								try {
									release(guild, user);
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
						}
					}
					catch(Exception e) {
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
	private static boolean initCache() {
		try {
			File file = new File("dhcp");
			if(!file.exists()) {
				file.mkdirs();
			}
			File[] dirs = file.listFiles();
			File[] tmp;
			for(File f : dirs) {
				if(f.isDirectory()) {
					tmp = f.listFiles();
					cache.put(f.getName(), new HashMap<>());
					for(File fl : tmp) {
						getIp(f.getName(), fl.getName());
					}
				}
			}
		}
		catch(Exception e) {
			return false;
		}
		return true;
	}
	private static String getIp(String guild, String user) throws Exception{
		HashMap<String, String> guildCache = cache.get(guild);
		String ip = "";
		if(!(guildCache == null) && guildCache.containsKey(user)) {
			return guildCache.get(user);
		}
		File file = new File("dhcp/" + guild);
		if(!file.exists()) {
			file.mkdirs();
		}
		file = new File("dhcp/" + guild + "/" + user);
		if(!file.exists()) {
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
		cache.get(guild).put(user, ip);
		if(!write) {return;}
		File file = new File("dhcp/" + guild + "/" + user);
		FileWriter fw = new FileWriter(file);
		fw.write(ip);
		fw.close();
	}
	private static String assignIp(String guild, String user) throws Exception{
		String ip = null;
		String range = IP_RANGES[(int)(Long.parseLong(guild)/2L)];
		HashMap<String, String> guildCache = cache.get(guild);
		int x = 0;
		do {
			ip = String.format(range, (x / 255), (x % 254) + 1);
			x++;
		}
		while(!guildCache.containsValue(ip));
		setIp(guild, user, ip, true);
		return ip;
	}
	private static void release(String guild, String user) throws Exception{
		File file = new File("dhcp/" + guild + "/" + user);
		file.delete();
		cache.get(guild).remove(user);
	}
}
