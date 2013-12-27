package com.tommytony.war.utility;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class WarLogFormatter extends Formatter {
	
	@Override
    public String format(LogRecord arg0) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        StringBuilder b = new StringBuilder();
        b.append(dateFormat.format(new Date()));
        b.append(" [");
        b.append(arg0.getLevel());
        b.append("] ");
        b.append(formatMessage(arg0));
        b.append(System.getProperty("line.separator"));
        return b.toString();
    }
}
