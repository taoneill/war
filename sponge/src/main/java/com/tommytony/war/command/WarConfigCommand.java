package com.tommytony.war.command;

import com.google.common.collect.ImmutableList;
import com.tommytony.war.WarConfig;
import com.tommytony.war.WarPlugin;
import org.spongepowered.api.entity.Player;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.util.command.CommandCallable;
import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.util.command.Description;

import javax.annotation.Nullable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class WarConfigCommand implements CommandCallable {
    private final WarPlugin plugin;

    public WarConfigCommand(WarPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Execute the command based on input arguments.
     * <p/>
     * <p>The implementing class must perform the necessary permission
     * checks.</p>
     *
     * @param source    The caller of the command
     * @param arguments The raw arguments for this command
     * @param parents   A stack of parent commands, where the first entry is
     *                  the root command
     * @return Whether a command was processed
     * @throws org.spongepowered.api.util.command.CommandException Thrown on a command error
     */
    @Override
    public boolean call(CommandSource source, String arguments, List<String> parents) throws CommandException {
        if (!testPermission(source)) {
            source.sendMessage("You do not have permission for this command.");
            return true;
        }
        source.sendMessage("you do have permission");
        return false;
    }

    /**
     * Get a description of the command, detailing usage information.
     *
     * @return The command description
     */
    @Override
    public Description getDescription() {
        return new Description() {
            @Nullable
            @Override
            public String getShortDescription() {
                return "View/modify war config";
            }

            @Nullable
            @Override
            public String getHelp() {
                return "Allows viewing of the war server config or changing various settings.";
            }

            @Override
            public String getUsage() {
                return "[-p] setting:value...";
            }

            @Override
            public List<String> getPermissions() {
                return ImmutableList.of("war.admin", "war.admin.config");
            }
        };
    }

    /**
     * Test whether this command can probably be executed by the given source.
     * <p/>
     * <p>If implementations are unsure if the command can be executed by
     * the source, {@code true} should be returned. Return values of this method
     * may be used to determine whether this command is listed in command
     * listings.</p>
     *
     * @param source The caller of the command
     * @return Whether permission is (probably) granted
     */
    @Override
    public boolean testPermission(CommandSource source) {
        if (source instanceof Player) {
            try {
                if (plugin.getConfig().getZoneMakers().contains(source)) {
                    source.sendMessage("You are a zone maker.");
                    return true;
                }
            } catch (SQLException e) {
                plugin.getLogger().error("Loading zone makers for testing permission", e);
            }
        }
        if (source instanceof Subject && ((Subject) source).isPermitted("war.admin.config")) {
            source.sendMessage("You are a war admin.");
            return true;
        }
        if (!(source instanceof Player) && !(source instanceof Subject)) {
            source.sendMessage("You are console or something.");
            return true;
        }
        return false;
    }

    /**
     * Get a list of suggestions based on input.
     * <p/>
     * <p>If a suggestion is chosen by the user, it will replace the last
     * word.</p>
     *
     * @param source    The command source
     * @param arguments The arguments entered up to this point
     * @return A list of suggestions
     * @throws org.spongepowered.api.util.command.CommandException Thrown if there was a parsing error
     */
    @Override
    public List<String> getSuggestions(CommandSource source, String arguments) throws CommandException {
        ArrayList<String> suggestions = new ArrayList<>();
        for (WarConfig.WarSetting setting : WarConfig.WarSetting.values()) {
            if (setting.name().toLowerCase().startsWith(arguments.toLowerCase()))
                suggestions.add(setting.name().toLowerCase() + ":");
        }
        return suggestions;
    }
}
