package com.tommytony.war;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.tommytony.war.command.WarConfigCommand;
import com.tommytony.war.command.WarzoneCommand;
import com.tommytony.war.zone.Warzone;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.event.Subscribe;
import org.spongepowered.api.event.state.PreInitializationEvent;
import org.spongepowered.api.event.state.ServerStartedEvent;
import org.spongepowered.api.event.state.ServerStartingEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.service.config.DefaultConfig;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@Plugin(id = "war", name = "War", version = "2.0-SNAPSHOT")
public class WarPlugin {
    private Game game;

    @Inject
    private Logger logger;

    @Inject
    @DefaultConfig(sharedRoot = false)
    private File dataDir;

    private WarConfig config;
    private Map<String, Warzone> zones;

    @Subscribe
    public void onConstruction(PreInitializationEvent event) throws InstantiationException {
        game = event.getGame();
        try {
            Class.forName("com.tommytony.war.sqlite.JDBC").newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new InstantiationException("Failed to load SQLite database");
        }
        zones = new HashMap<>();
    }

    @Subscribe
    public void onStartUp(ServerStartingEvent event) throws FileNotFoundException, SQLException {
        if (!dataDir.exists() && !dataDir.mkdirs())
            throw new FileNotFoundException("Failed to make War data folder at " + dataDir.getPath());
        config = new WarConfig(this, new File(dataDir, "war.sl3"));
        for (String zoneName : config.getZones()) {
            Warzone zone = new Warzone(this, zoneName);
            zones.put(zoneName, zone);
        }
    }

    @Subscribe
    public void onStart(ServerStartedEvent event) {
        // register commands
        game.getCommandDispatcher().register(this, new WarzoneCommand(this), "warzone", "zone");
        game.getCommandDispatcher().register(this, new WarConfigCommand(this), "warcfg", "warconfig");
    }

    public Game getGame() {
        return game;
    }

    public Logger getLogger() {
        return logger;
    }

    public File getDataDir() {
        return dataDir;
    }

    public WarConfig getConfig() {
        return config;
    }

    public Optional<Warzone> getZone(String zoneName) {
        if (zones.containsKey(zoneName)) {
            return Optional.of(zones.get(zoneName));
        }
        return Optional.absent();
    }

    public Map<String, Warzone> getZones() {
        return zones;
    }
}
