package de.raindancer.modules.radio.command;

import de.raindancer.modules.radio.RadioServices;
import de.raindancer.modules.radio.model.Station;
import de.raindancer.modules.radio.rules.RadioRule;
import de.raindancer.modules.radio.screen.RadioAnswers;
import de.raindancer.modules.radio.screen.RadioMenu;
import de.raindancer.modules.radio.service.RadioService;
import de.raindancer.modules.radio.util.PermissionNodes;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.function.Supplier;

/**
 * {@code /music} opens the radio; {@code skip}, {@code stop}, {@code on}, {@code station <name>},
 * {@code volume <percent>} and {@code status} do it straight away. A station's name on its own tunes in too.
 */
public final class RadioCommand implements BasicCommand {

    private static final List<String> OPTIONS = List.of("station", "skip", "stop", "on", "volume", "status");

    private final Supplier<RadioServices> services;

    public RadioCommand(Supplier<RadioServices> services) {
        this.services = services;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        RadioServices live = services.get();
        if (!(source.getSender() instanceof Player player)) {
            live.messages().send(source.getSender(), "radio.only-a-player");
            return;
        }
        RadioService radio = live.radio();
        String first = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        switch (first) {
            case "" -> new RadioMenu(live, player, null).open();
            case "skip", "next" -> live.messages().send(player, radio.skip(player) ? "radio.skipped" : "radio.nothing-to-skip");
            case "stop", "off" -> live.messages().send(player, radio.stop(player) ? "radio.stopped" : "radio.was-off");
            case "on", "play" -> {
                Station station = radio.station(player);
                RadioAnswers.tuned(live, player, station, radio.turnOn(player), true);
            }
            case "station" -> {
                if (args.length < 2) {
                    live.messages().send(player, "radio.which-station", "stations", names());
                    return;
                }
                tune(live, player, args[1]);
            }
            case "volume" -> {
                OptionalInt typed = args.length < 2 ? OptionalInt.empty() : RadioRule.typedVolume(args[1]);
                if (typed.isEmpty()) {
                    live.messages().send(player, "radio.volume-usage", "volume", String.valueOf(radio.volume(player)));
                    return;
                }
                live.messages().send(player, "radio.volume", "volume", String.valueOf(radio.volume(player, typed.getAsInt())));
            }
            case "status" -> status(live, player);
            default -> {
                if (Station.find(first).isPresent()) {
                    tune(live, player, first);
                } else {
                    live.messages().send(player, "radio.usage");
                }
            }
        }
    }

    private static void tune(RadioServices live, Player player, String typed) {
        Station.find(typed).ifPresentOrElse(
                station -> RadioAnswers.tuned(live, player, station, live.radio().tune(player, station), true),
                () -> live.messages().send(player, "radio.unknown-station", "typed", typed, "stations", names()));
    }

    private static void status(RadioServices live, Player player) {
        RadioService.Status now = live.radio().status(player);
        if (!now.on()) {
            live.messages().send(player, "radio.status-off", "station", now.station().title());
            return;
        }
        now.track().ifPresentOrElse(
                track -> live.messages().send(player, "radio.status-playing", "title", track.title(),
                        "station", now.station().title(), "volume", String.valueOf(now.volume())),
                () -> live.messages().send(player, "radio.status-waiting", "station", now.station().title()));
    }

    private static String names() {
        return String.join(", ", Arrays.stream(Station.values()).map(Station::id).toList());
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        String typed = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        if (args.length <= 1) {
            return OPTIONS.stream().filter(option -> option.startsWith(typed)).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("station")) {
            return Arrays.stream(Station.values()).map(Station::id).filter(id -> id.startsWith(typed)).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("volume")) {
            return List.of("25", "50", "75", "100");
        }
        return List.of();
    }

    @Override
    public String permission() {
        return PermissionNodes.USE;
    }
}
