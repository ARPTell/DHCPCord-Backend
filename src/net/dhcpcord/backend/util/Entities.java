package net.dhcpcord.backend.util;

public enum Entities {

	IP("IP"),
	USER("USER"),
	MASK("MASK"),
	SERVICE("SERVICE");
	
	private final String name;
	private Entities(String name) {
		this.name = name;
	}
	public boolean isEntity(String entity) {
		return EntityChecker.isValid(entity);
	}
	public String toString() {
		return name;
	}
}
