package com.tommytony.war.structure;

import java.util.logging.Level;

import org.bukkit.Material;

import com.tommytony.war.War;

public class WarzoneMaterials {
	private int mainId;
	private byte mainData;
	
	private int standId;
	private byte standData;

	private int lightId;
	private byte lightData;
	
	public WarzoneMaterials(int mainId, byte mainData, int standId, byte standData, int lightId, byte lightData) {
		// Make sure we are using legal blocks or AIR as material
		if (isBlockOrAir(mainId)) {
			this.setMainId(mainId);
			this.setMainData(mainData);
		} else {
			this.setMainId(49);	// default obsidian
			this.setMainData((byte)0);
		}
		
		if (isBlockOrAir(standId)) {
			this.setStandId(standId);
			this.setStandData(standData);
		} else {
			this.setStandId(85);	// default ladder
			this.setStandData((byte)0);
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
			War.war.log("Can't use item with id:" + itemId + " as warzone material.", Level.WARNING);
			return false;
		}
	}

	public int getMainId() {
		return mainId;
	}

	public byte getMainData() {
		return mainData;
	}

	public int getLightId() {
		return lightId;
	}

	public byte getLightData() {
		return lightData;
	}

	public int getStandId() {
		return standId;
	}

	public byte getStandData() {
		return standData;
	}

	public void setMainId(int mainId) {
		this.mainId = mainId;
	}

	public void setMainData(byte mainData) {
		this.mainData = mainData;
	}

	public void setStandId(int standId) {
		this.standId = standId;
	}

	public void setStandData(byte standData) {
		this.standData = standData;
	}

	public void setLightId(int lightId) {
		this.lightId = lightId;
	}

	public void setLightData(byte lightData) {
		this.lightData = lightData;
	}
	
}
