package de.raindancer.modules.radio;

import de.raindancer.core.content.music.MusicPack;
import de.raindancer.core.content.music.Track;
import de.raindancer.core.data.settings.SettingsStore;
import de.raindancer.modules.api.FlexModule;
import de.raindancer.modules.api.ModuleCommand;
import de.raindancer.modules.api.ModuleContext;
import de.raindancer.modules.api.ModuleInfo;
import de.raindancer.modules.radio.listener.RadioListener;
import de.raindancer.modules.radio.model.Station;
import de.raindancer.modules.radio.service.RadioService;
import de.raindancer.modules.radio.util.PermissionNodes;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@code /music}: four stations each player can tune in, heard by them alone. The tracks are the owner's,
 * {@code plugins/RainsRadio/music/<station>/*.ogg}, offered to the server's resource pack through Core.
 */
public final class RadioModule implements FlexModule {

    private static final ModuleInfo INFO = ModuleInfo.of("radio", "Radio", "0.2.0")
            .describedAs("Your own radio: four stations, skip, stop and volume, heard by you alone")
            .by("Raindancer118");

    private volatile RadioService radio;

    @Override
    public ModuleInfo info() {
        return INFO;
    }

    @Override
    public void enable(ModuleContext context) {
        SettingsStore<RadioSettings> settings = context.settings(RadioSettings.class, RadioSettings.DEFAULTS);
        context.core().messages().defineFrom(RadioModule.class.getResourceAsStream("messages.yml"),
                context.chat().brand()::chatPrefix);
        PermissionNodes.register(context.plugin().getServer());

        Map<Station, List<Track>> tracks = music(context);
        radio = new RadioService(context.plugin(), settings::current, station -> tracks.getOrDefault(station, List.of()),
                context.core().resourcePacks(), context.core().actionBars(), context.core().messages(),
                System::currentTimeMillis);
        context.listener(new RadioListener(radio));
        RadioCommands.ready(new RadioServices(context.core().messages(), radio));
        for (Player online : context.plugin().getServer().getOnlinePlayers()) {
            radio.joined(online);
        }
        context.log().info("Radio is up: {}.", Arrays.stream(Station.values())
                .map(station -> station.title() + " " + tracks.getOrDefault(station, List.of()).size())
                .collect(Collectors.joining(", ")));
    }

    private static Map<Station, List<Track>> music(ModuleContext context) {
        try {
            MusicPack pack = MusicPack.offer(context.plugin(), context.core().resourcePacks(), "Radio", "rainsradio",
                    "Rain's Radio: music", Arrays.stream(Station.values()).map(Station::id).toList());
            context.closeWith(pack::withdraw);
            return Arrays.stream(Station.values()).collect(Collectors.toMap(station -> station,
                    station -> pack.shelf(station.id())));
        } catch (IOException failure) {
            context.log().warn("The radio's music could not be read ({}); every station is silent.",
                    failure.getMessage());
            return Map.of();
        }
    }

    @Override
    public List<ModuleCommand> commands() {
        return RadioCommands.declared();
    }

    @Override
    public void disable() {
        RadioCommands.stopped();
        RadioService running = radio;
        if (running != null) {
            running.stopAll();
        }
        radio = null;
    }
}
