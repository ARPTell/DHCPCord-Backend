package net.dhcpcord.backend;

import java.net.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Scanner;

public class DHCPBackend {
	
	private static final String[] IP_RANGES = {"192.168.%d.%d", "10.0.%d.%d"};
	
	public static void main(String[] args) throws Exception{
		boolean crashed = false;
		ServerSocket server = new ServerSocket(47606);
		System.out.println("Started DHCP server on " + server.getInetAddress().getHostAddress() + ":" + server.getLocalPort());
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
						if(entity.equals("IP")) {
							if(intent.equals("GET")) {
								output.println(getIp(guild, user));
							}
							else if(intent.equals("ASSIGN")) {
								output.println("Not Implemented");
							}
							else if(intent.equals("SET")) {
								output.println("Not Implemented");
							}
							else {
								output.println("UNKNOWN OPERATION " + intent);
							}
						}
						else {
							output.println("UNKNOWN ENTITY " + entity);
						}
					}
					catch(Exception e) {
						e.printStackTrace(output);
					}
					output.flush();
				}
				System.out.println("Connection closed");
				
			}
			catch(Exception e) {
				System.out.println("An error occured:");
				e.printStackTrace();
				if(crashed) {
					System.exit(1);
				}
				crashed = true;
			}
		}
	}
	private static String getIp(String guild, String user) {
		String ip = "";
		File file = new File("dhcp/" + guild);
		if(!file.exists()) {
			file.mkdirs();
		}
		file = new File("dhcp/" + guild + "/" + user);
		if(!file.exists()) {
			
		}
		return "test";
	}
	private static String assignIp(String guild, String user) {
		String ip = "";
		String range = IP_RANGES[(int)(Long.parseLong(guild)/2L)];
		return ip;
	}
}
