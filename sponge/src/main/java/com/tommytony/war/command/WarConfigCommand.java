package com.tommytony.war.command;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.tommytony.war.WarConfig;
import com.tommytony.war.WarPlugin;
import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.util.command.CommandCallable;
import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandResult;
import org.spongepowered.api.util.command.CommandSource;

import java.sql.SQLException;
import java.util.List;

public class WarConfigCommand implements CommandCallable {

    private final Optional<Text> desc = Optional.of((Text) Texts.of("View/modify War config"));
    private final Optional<Text> help = Optional.of((Text) Texts.of("Allows viewing of the server config or changing various settings."));
    private final Text usage = (Text) Texts.of("[-p] setting value");

    private final WarPlugin plugin;

    public WarConfigCommand(WarPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String arguments) throws CommandException {
        ImmutableList.Builder<String> list = ImmutableList.builder();
        for (WarConfig.WarSetting setting : WarConfig.WarSetting.values()) {
            if (setting.name().toLowerCase().startsWith(arguments.toLowerCase()))
                list.add(setting.name().toLowerCase() + ":");
        }
        return list.build();
    }

    @Override
    public boolean testPermission(CommandSource source) {
        try {
            if (source instanceof Player && plugin.getConfig().getZoneMakers().contains(source)) {
                return true;
            }
        } catch (SQLException e) {
            plugin.getLogger().error("Loading zone makers for testing permission", e);
        }
        if (source.hasPermission("war.config")) {
            return true;
        }
        return false;
    }

    @Override
    public Optional<? extends Text> getShortDescription(CommandSource source) {
        return desc;
    }

    @Override
    public Optional<? extends Text> getHelp(CommandSource source) {
        return help;
    }

    @Override
    public Text getUsage(CommandSource source) {
        return usage;
    }
}
