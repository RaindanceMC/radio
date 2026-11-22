package de.raindancer.modules.radio.screen;

import de.raindancer.core.content.music.Track;
import de.raindancer.core.ui.dialog.DialogPage;
import de.raindancer.core.ui.text.Text;
import de.raindancer.modules.radio.RadioServices;
import de.raindancer.modules.radio.model.Station;
import de.raindancer.modules.radio.rules.RadioRule;
import de.raindancer.modules.radio.service.RadioService;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * {@code /music} as the game's own window: what plays on top, the four stations, then skip, on or off and the
 * volume. Every press does its thing and brings the window back, built fresh; what could not be done is said at
 * the top of it rather than in chat.
 */
public final class RadioDialog {

    static final String HEADING = "<gradient:#6EC1EA:#1F4E8C>Radio</gradient>";

    /** What the buttons do; the window itself holds no state. */
    public interface Controls {
        RadioService.Tuned tune(Station station);

        boolean skip();

        void turnOn();

        void turnOff();

        void volume(int percent);

        /** @param notice why the last press did nothing, or null */
        void reopen(String notice);
    }

    private RadioDialog() {
    }

    public static void open(RadioServices services, Player viewer) {
        RadioService radio = services.radio();
        Map<Station, Integer> counts = Arrays.stream(Station.values())
                .collect(Collectors.toMap(Function.identity(), station -> radio.tracks(station).size()));
        Controls controls = new Controls() {
            @Override
            public RadioService.Tuned tune(Station station) {
                return radio.tune(viewer, station);
            }

            @Override
            public boolean skip() {
                return radio.skip(viewer);
            }

            @Override
            public void turnOn() {
                radio.turnOn(viewer);
            }

            @Override
            public void turnOff() {
                radio.stop(viewer);
            }

            @Override
            public void volume(int percent) {
                radio.volume(viewer, percent);
            }

            @Override
            public void reopen(String notice) {
                DialogPage again = RadioDialog.page(radio.status(viewer), counts, this);
                (notice == null ? again : again.withNotice(notice)).show(viewer);
            }
        };
        page(radio.status(viewer), counts, controls).show(viewer);
    }

    static DialogPage page(RadioService.Status now, Map<Station, Integer> counts, Controls controls) {
        DialogPage page = DialogPage.titled(HEADING).columns(2).text(header(now));
        for (Station station : Station.values()) {
            boolean onAir = now.on() && now.station() == station;
            int count = counts.getOrDefault(station, 0);
            page.button((onAir ? "<green>▶ " : "<white>") + station.title(),
                    "<gray>" + station.blurb() + "\n<dark_gray>" + tracks(count), answers -> {
                        RadioService.Tuned tuned = controls.tune(station);
                        controls.reopen(refusal(tuned, station));
                    });
        }
        page.button(now.on(), "<white>Skip", "<gray>Another track on the same station.", "The radio is off.",
                answers -> {
                    controls.skip();
                    controls.reopen(null);
                });
        if (now.on()) {
            page.button("<red>Turn off", "<gray>It stays off until you turn it on again.", answers -> {
                controls.turnOff();
                controls.reopen(null);
            });
        } else {
            page.button("<green>Turn on", "<gray>Plays " + now.station().title() + ", the station you had last.",
                    answers -> {
                        controls.turnOn();
                        controls.reopen(null);
                    });
        }
        page.button("<white>− Quieter", "<gray>10 % down, now " + now.volume() + "%.", answers -> {
            controls.volume(RadioRule.quieter(now.volume()));
            controls.reopen(null);
        });
        page.button("<white>+ Louder", "<gray>10 % up, now " + now.volume() + "%.", answers -> {
            controls.volume(RadioRule.louder(now.volume()));
            controls.reopen(null);
        });
        return page;
    }

    private static String header(RadioService.Status now) {
        if (!now.on()) {
            return "<gray>The radio is off. Pick a station to listen.\n<dark_gray>Only you hear it, and it stays on"
                    + " until you turn it off.";
        }
        String where = "<gray>On <white>" + now.station().title() + "</white> · volume <white>" + now.volume()
                + "%</white>";
        if (now.track().isEmpty()) {
            return "<white>" + now.station().title() + "\n" + (now.waiting() ? "<gray>Starts in a moment."
                    : "<gray>Between two tracks.") + "\n" + where;
        }
        Track track = now.track().get();
        return "<white>♪ " + Text.literal(track.title()) + "\n" + where + " · " + RadioRule.clock(track.seconds());
    }

    private static String tracks(int count) {
        return count == 0 ? "No tracks yet." : count == 1 ? "1 track" : count + " tracks";
    }

    private static String refusal(RadioService.Tuned tuned, Station station) {
        return switch (tuned) {
            case PLAYING -> null;
            case NO_TRACKS -> station.title() + " has no tracks yet.";
            case OFF_HERE -> "The radio is switched off on this server.";
            case NO_PACK -> "The radio needs the server's resource pack, which has the music in it.";
        };
    }
}
