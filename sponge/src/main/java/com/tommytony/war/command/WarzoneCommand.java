package com.tommytony.war.command;

import com.google.common.base.Optional;
import com.tommytony.war.WarPlugin;
import com.tommytony.war.zone.Warzone;
import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.util.command.CommandCallable;
import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Teleport to warzone.
 */
public class WarzoneCommand implements CommandCallable {
    private WarPlugin plugin;

    public WarzoneCommand(WarPlugin plugin) {
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
        if (!(source instanceof Player)) {
            return false;
        }
        String[] argv = arguments.split(" ");
        if (argv.length < 1) {
            return false;
        }
        String zoneName = argv[0];
        Optional<Warzone> zone = plugin.getZone(zoneName);
        if (!zone.isPresent()) {
            return false;
        }
        Player player = (Player) source;
        player.teleport(zone.get().getTeleport());

        return true;
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
        return true;
    }

    /**
     * Get a short one-line description of this command.
     *
     * @return A description, if available
     */
    @Override
    public Optional<String> getShortDescription() {
        return Optional.of("Teleport to a zone");
    }

    /**
     * Get a longer help text about this command.
     *
     * @return A help text, if available
     */
    @Override
    public Optional<String> getHelp() {
        return Optional.of("Use this command to teleport to a zone lobby");
    }

    /**
     * Get the usage string of this command.
     * <p/>
     * <p>A usage string may look like
     * {@code [-w &lt;world&gt;] &lt;var1&gt; &lt;var2&gt;}.</p>
     *
     * @return A usage string
     */
    @Override
    public String getUsage() {
        return "<zone>";
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
        for (String zone : plugin.getZones().keySet()) {
            if (zone.toLowerCase().startsWith(arguments.toLowerCase()))
                suggestions.add(zone);
        }
        return suggestions;
    }
}
