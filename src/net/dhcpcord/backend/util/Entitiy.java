package net.dhcpcord.backend.util;

public enum Entitiy {

	IP("IP"),
	USER("USER"),
	MASK("MASK"),
	PORT("PORT"),
	SERVICE("SERVICE");
	
	private final String name;
	private Entitiy(String name) {
		this.name = name;
	}
	public boolean isEntity(String entity) {
		return EntityChecker.isValid(entity);
	}
	public String toString() {
		return name;
	}
}
