package com.tommytony.war.command;

import org.bukkit.command.CommandSender;


import com.tommytony.war.War;
import com.tommytony.war.WarCommandHandler;
import com.tommytony.war.mapper.WarYmlMapper;

public class SetWarConfigCommand extends AbstractWarAdminCommand {

	public SetWarConfigCommand(WarCommandHandler handler, CommandSender sender, String[] args) throws NotWarAdminException {
		super(handler, sender, args);
	}

	@Override
	public boolean handle() {
		boolean wantsToPrint = false;
		if (this.args.length == 0) {
			return false;
		} else if (this.args.length == 1 && (this.args[0].equals("-p") || this.args[0].equals("print"))) {
			String config = War.war.printConfig();
			this.msg(config);
			return true;
		} else if (this.args.length > 1 && (this.args[0].equals("-p") || this.args[0].equals("print"))) {
			wantsToPrint = true;
		}

		String namedParamReturn = War.war.updateFromNamedParams(this.getSender(), this.args); 
		if (!namedParamReturn.equals("") && !namedParamReturn.equals("PARSE-ERROR")) {
			WarYmlMapper.save();
			if (wantsToPrint) {
				String config = War.war.printConfig();
				this.msg("War config saved." + namedParamReturn + " " + config);
			} else {
				this.msg("War config saved." + namedParamReturn);
			}
		} else if (namedParamReturn.equals("PARSE-ERROR")) {
			this.msg("Failed to read named parameters.");
		} else {
			return false;
		}

		return true;
	}

}
