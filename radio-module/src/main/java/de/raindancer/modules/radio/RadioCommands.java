package de.raindancer.modules.radio;

import de.raindancer.modules.api.ModuleCommand;
import de.raindancer.modules.radio.command.RadioCommand;

import java.util.List;

/**
 * Declared at bootstrap, filled in when the module starts — Paper registers commands before any
 * plugin is enabled, so the command points at {@link #require} until then.
 */
public final class RadioCommands {

    private static volatile RadioServices services;

    private RadioCommands() {
    }

    public static List<ModuleCommand> declared() {
        return List.of(ModuleCommand.of("music", "Your own radio: four stations, skip, stop, volume",
                        new RadioCommand(RadioCommands::require))
                .aliased("radio")
                .taking("(nothing): the radio's page", "station <calm|adventure|cozy|night>", "skip", "stop | on",
                        "volume <percent>", "status")
                .needing("rainsradio.use"));
    }

    static void ready(RadioServices live) {
        services = live;
    }

    static void stopped() {
        services = null;
    }

    private static RadioServices require() {
        RadioServices live = services;
        if (live == null) {
            throw new IllegalStateException("the radio module is not running");
        }
        return live;
    }
}
