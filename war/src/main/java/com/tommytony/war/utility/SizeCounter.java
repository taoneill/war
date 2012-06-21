package com.tommytony.war.utility;

import java.io.File;
import java.io.FileFilter;

// Credit: http://sanjaal.com/java/48/java-utilities/calculating-folder-size/
public class SizeCounter implements FileFilter
{
	private long total = 0;
	
	public SizeCounter(){};
	
	public boolean accept(File pathname) {
		if (pathname.isFile()) {
			total += pathname.length();
		} else {
			pathname.listFiles(this);
		}
		
		return false;
	}
	
	public long getTotal()
	{
		return total;
	}
	
	public static long getFileOrDirectorySize(File file) {
		SizeCounter counter = new SizeCounter();
		file.listFiles(counter);
		return counter.getTotal();
	}
}