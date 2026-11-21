package de.raindancer.modules.radio.screen;

import de.raindancer.modules.radio.RadioServices;
import de.raindancer.modules.radio.model.Station;
import de.raindancer.modules.radio.service.RadioService;
import org.bukkit.entity.Player;

/** The one line a player gets after tuning in, the same from the page and from the command. */
public final class RadioAnswers {

    private RadioAnswers() {
    }

    /** @param chatty also confirm when it simply worked — the page shows that itself */
    public static void tuned(RadioServices services, Player player, Station station, RadioService.Tuned tuned,
                             boolean chatty) {
        switch (tuned) {
            case PLAYING -> {
                if (chatty) {
                    services.messages().send(player, "radio.tuned", "station", station.title());
                }
            }
            case NO_TRACKS -> services.messages().send(player, "radio.no-tracks", "station", station.title());
            case OFF_HERE -> services.messages().send(player, "radio.off-here");
            case NO_PACK -> services.messages().send(player, "radio.no-pack");
        }
    }
}
