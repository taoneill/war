package com.tommytony.war.utils;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;

import bukkit.tommytony.war.War;

public class SignHelper {

	public static void setToSign(War war, Block block, byte data, String[] lines) {
		if(block.getType() != Material.SIGN_POST) {
			block.setType(Material.SIGN_POST);
		}
		if(block.getData() != data) {
			block.setData(data);
		}
		BlockState state = block.getState();
		if(state instanceof Sign) {
			Sign sign = (Sign) state;
			try {
				if(sign.getLines() != null) {
					sign.setLine(0, lines[0]);
					sign.setLine(1, lines[1]);
					sign.setLine(2, lines[2]);
					sign.setLine(3, lines[3]);
					sign.update(true);
				}
			} catch (Exception e) {
				
				// just can't stand this anymore
			}
		}
	}
	
}
