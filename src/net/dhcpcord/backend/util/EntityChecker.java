package net.dhcpcord.backend.util;

public class EntityChecker {

	public static boolean isValid(String entity) {
		for(Entities e : Entities.values()) {
			if(e.toString().equals(entity)) {
				return true;
			}
		}
		return false;
	}
}
