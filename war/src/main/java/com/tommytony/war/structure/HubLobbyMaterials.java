package com.tommytony.war.structure;

import java.util.logging.Level;

import org.bukkit.Material;

import com.tommytony.war.War;

public class HubLobbyMaterials {
	private int floorId;
	private byte floorData;
	
	private int outlineId;
	private byte outlineData;
	
	private int gateId;
	private byte gateData;
	
	private int lightId;
	private byte lightData;
	
	public HubLobbyMaterials(int floorId, byte floorData, int outlineId, byte outlineData, int gateId, byte gateData, int lightId, byte lightData) {
		// Make sure we are using legal blocks or AIR as material
		if (isBlockOrAir(floorId)) {
			this.setFloorId(floorId);
			this.setFloorData(floorData);
		} else {
			this.setFloorId(20);	// default glass
			this.setFloorData((byte)0);
		}
		
		if (isBlockOrAir(outlineId)) {
			this.setOutlineId(outlineId);
			this.setOutlineData(outlineData);
		} else {
			this.setOutlineId(5);	// default planks
			this.setOutlineData((byte)0);
		}
		
		if (isBlockOrAir(gateId)) {
			this.setGateId(gateId);
			this.setGateData(gateData);
		} else {
			this.setGateId(49);	// default obsidian
			this.setGateData((byte)0);
		}
		
		if (isBlockOrAir(lightId)) {
			this.setLightId(lightId);
			this.setLightData(lightData);
		} else {
			this.setLightId(89);	// default glowstone
			this.setLightData((byte)0);
		}
	}

	private boolean isBlockOrAir(int itemId) {
		Material material = Material.getMaterial(itemId);
		if (material.isBlock() || material.equals(Material.AIR)) {
			return true;
		} else {
			War.war.log("Can't use item with id:" + itemId + " as lobby or warhub material.", Level.WARNING);
			return false;
		}
	}

	public int getFloorId() {
		return floorId;
	}

	public byte getFloorData() {
		return floorData;
	}

	public int getGateId() {
		return gateId;
	}

	public byte getGateData() {
		return gateData;
	}

	public int getLightId() {
		return lightId;
	}

	public byte getLightData() {
		return lightData;
	}

	public int getOutlineId() {
		return outlineId;
	}

	public byte getOutlineData() {
		return outlineData;
	}

	public void setFloorId(int floorId) {
		this.floorId = floorId;
	}

	public void setFloorData(byte floorData) {
		this.floorData = floorData;
	}

	public void setOutlineId(int outlineId) {
		this.outlineId = outlineId;
	}

	public void setOutlineData(byte outlineData) {
		this.outlineData = outlineData;
	}

	public void setGateId(int gateId) {
		this.gateId = gateId;
	}

	public void setGateData(byte gateData) {
		this.gateData = gateData;
	}

	public void setLightId(int lightId) {
		this.lightId = lightId;
	}

	public void setLightData(byte lightData) {
		this.lightData = lightData;
	}
	
}
