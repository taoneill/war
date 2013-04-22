package com.tommytony.war.utility;

import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

import com.tommytony.war.War;

public class KillstreakMetadata implements MetadataValue {

	private long data;
	
	/*
	 * killstreak = how many kills to get the killstreak
	 * value = int representing a state
	 */
	public KillstreakMetadata(int killstreak, int value) {
		this.data = this.pack(killstreak, value);
	}
	
	private long pack(int killstreak, int value) {
		return (killstreak << 32) | value;
		
	}
	
	public Object value() {
		return this.data;
	}

	public int asInt() {
		throw new NullPointerException();
	}

	public float asFloat() {
		throw new NullPointerException();
	}

	public double asDouble() {
		throw new NullPointerException();
	}

	public long asLong() {
		return this.data;
	}

	public short asShort() {
		throw new NullPointerException();
	}

	public byte asByte() {
		throw new NullPointerException();
	}

	public boolean asBoolean() {
		throw new NullPointerException();
	}

	public String asString() {
		return "packed_meta-" + this.data;
	}

	public Plugin getOwningPlugin() {
		return War.war;
	}

	public void invalidate() {
		return;
	}

}
