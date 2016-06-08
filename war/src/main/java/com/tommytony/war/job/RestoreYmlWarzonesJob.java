package com.tommytony.war.job;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.Locale;

import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import com.tommytony.war.config.WarzoneConfig;
import com.tommytony.war.mapper.WarzoneYmlMapper;

import org.bukkit.Bukkit;

import org.mcstats.Metrics;
import org.mcstats.Metrics.Graph;

public class RestoreYmlWarzonesJob implements Runnable {

	private final List<String> warzones;

	public RestoreYmlWarzonesJob(List<String> warzones) {
		this.warzones = warzones;
	}

	public void run() {
		War.war.getWarzones().clear();
		if (this.warzones != null) {
			for (String warzoneName : this.warzones) {
				if (warzoneName != null && !warzoneName.equals("")) {
					War.war.log("Loading zone " + warzoneName + "...", Level.INFO);
					Warzone zone = WarzoneYmlMapper.load(warzoneName);
					if (zone != null) { // could have failed, would've been logged already
						War.war.getWarzones().add(zone);
						try {
							zone.getVolume().loadCorners();
						} catch (SQLException ex) {
							War.war.log("Failed to load warzone " + warzoneName + ": " + ex.getMessage(), Level.WARNING);
							throw new RuntimeException(ex);
						}
						if (zone.getLobby() != null) {
							zone.getLobby().getVolume().resetBlocks();
						}
						if (zone.getWarzoneConfig().getBoolean(WarzoneConfig.RESETONLOAD)) {
							zone.getVolume().resetBlocks();
						}
						zone.initializeZone();
					}
				}
			}
			if (War.war.getWarzones().size() > 0) {
				War.war.log("Warzones ready.", Level.INFO);
				final int zones = War.war.getWarzones().size();
				try {
					Metrics metrics = new Metrics(War.war);
					Graph warzoneCount = metrics.createGraph("Warzones");
					warzoneCount.addPlotter(new FixedPlotter("Count", zones));
					Graph language = metrics.createGraph("Language");
					String langName = War.war.getLoadedLocale().getDisplayLanguage(Locale.ENGLISH);
					if (langName.isEmpty()) {
						langName = "English";
					}
					language.addPlotter(new PlotterEnabled(langName));
					Graph plugins = metrics.createGraph("Extensions");
					if (War.war.isSpoutServer()) {
						plugins.addPlotter(new PlotterEnabled("Spout"));
					}
					if (War.war.isTagServer()) {
						plugins.addPlotter(new PlotterEnabled("TagAPI"));
					}
					if (Bukkit.getPluginManager().isPluginEnabled("WorldEdit")) {
						plugins.addPlotter(new PlotterEnabled("WorldEdit"));
					}
					plugins.addPlotter(new PlotterEnabled("War")); // of course
					metrics.start();
				} catch (Exception ignored) {
				}
			}
		}
	}

	private static class FixedPlotter extends Metrics.Plotter {

		private final int value;

		public FixedPlotter(final String name, final int value) {
			super(name);
			this.value = value;
		}

		@Override
		public int getValue() {
			return value;
		}
	}

	private static class PlotterEnabled extends Metrics.Plotter {

		public PlotterEnabled(final String name) {
			super(name);
		}

		@Override
		public int getValue() {
			return 1;
		}
	}
}
