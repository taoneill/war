package com.tommytony.war.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Connor on 7/27/2017.
 */
public class UIFormatter {
	private int i;
	private List<Integer> pattern;

	UIFormatter(int slots) {
		this.i = 0;
		this.pattern = new ArrayList<Integer>();
		int row = 0;
		while (slots > 9) {
			List<Integer> pattern = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8);
			for (int i = 0; i < pattern.size(); i++) {
				pattern.set(i, pattern.get(i) + row * 9);
			}
			this.pattern.addAll(pattern);
			row++;
		}
		List<Integer> pattern;
		switch (slots) {
			case 0:
				pattern = Arrays.asList(0);
				break;
			case 1:
				pattern = Arrays.asList(4);
				break;
			case 2:
				pattern = Arrays.asList(2, 6);
				break;
			case 3:
				pattern = Arrays.asList(1, 4, 7);
				break;
			case 4:
				pattern = Arrays.asList(1, 3, 5, 7);
				break;
			case 5:
				pattern = Arrays.asList(0, 2, 4, 6, 8);
				break;
			case 6:
				pattern = Arrays.asList(1, 2, 3, 5, 6, 7);
				break;
			case 7:
				pattern = Arrays.asList(1, 2, 3, 4, 5, 6, 7);
				break;
			case 8:
				pattern = Arrays.asList(0, 1, 2, 3, 5, 6, 7, 8);
				break;
			case 9:
				pattern = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8);
				break;
			default:
				throw new IllegalStateException();
		}
		for (int i = 0; i < pattern.size(); i++) {
			pattern.set(i, pattern.get(i) + row * 9);
		}
		this.pattern.addAll(pattern);
	}

	int next() {
		return this.pattern.get(i++);
	}
}
