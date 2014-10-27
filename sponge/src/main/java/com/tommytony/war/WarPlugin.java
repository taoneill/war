package com.tommytony.war;

import org.apache.logging.log4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.event.SpongeEventHandler;
import org.spongepowered.api.event.state.PreInitializationEvent;
import org.spongepowered.api.event.state.ServerStartedEvent;
import org.spongepowered.api.event.state.ServerStartingEvent;
import org.spongepowered.api.plugin.Plugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.SQLException;

@Plugin(id = "war", name = "War", version = "2.0-SNAPSHOT")
public class WarPlugin {
    private Game game;
    private Logger logger;
    private File dataDir;
    private WarConfig config;

    @SpongeEventHandler
    public void onConstruction(PreInitializationEvent event) throws InstantiationException {
        game = event.getGame();
        logger = event.getPluginLog();
        dataDir = event.getConfigurationDirectory();
        try {
            Class.forName("com.tommytony.war.sqlite.JDBC").newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new InstantiationException("Failed to load SQLite database");
        }
    }

    @SpongeEventHandler
    public void onStartUp(ServerStartingEvent event) throws FileNotFoundException, SQLException {
        if (!dataDir.exists() && !dataDir.mkdirs())
            throw new FileNotFoundException("Failed to make War data folder at " + dataDir.getPath());
        config = new WarConfig(this, new File(dataDir, "war.sl3"));
    }

    @SpongeEventHandler
    public void onStart(ServerStartedEvent event) {
        // register commands
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
}
