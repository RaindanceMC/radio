package de.raindancer.modules.radio.screen;

import de.raindancer.core.content.music.Track;
import de.raindancer.core.ui.menu.Arrow;
import de.raindancer.core.ui.menu.Icons;
import de.raindancer.core.ui.menu.Menu;
import de.raindancer.core.ui.menu.MenuLayout;
import de.raindancer.modules.radio.RadioServices;
import de.raindancer.modules.radio.model.Station;
import de.raindancer.modules.radio.rules.RadioRule;
import de.raindancer.modules.radio.service.RadioService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** /music: the four stations on top, what plays in the middle, the buttons below. */
public final class RadioMenu extends Menu {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final int[] STATION_COLUMNS = {1, 3, 5, 7};

    private final RadioServices services;

    public RadioMenu(RadioServices services, Player viewer, Menu parent) {
        super(viewer, services.brand(), parent);
        this.services = services;
    }

    @Override
    protected Component title() {
        return MINI.deserialize("<dark_gray>Radio");
    }

    @Override
    protected void render() {
        RadioService radio = services.radio();
        RadioService.Status now = radio.status(viewer);
        Station[] stations = Station.values();
        for (int at = 0; at < stations.length; at++) {
            Station station = stations[at];
            band(MenuLayout.WHO, STATION_COLUMNS[at], stationIcon(station, now), click -> {
                RadioAnswers.tuned(services, viewer, station, services.radio().tune(viewer, station), false);
                refresh();
            });
        }

        band(MenuLayout.RULES, 4, playingIcon(now));

        band(MenuLayout.LAND, 2, Icons.arrow(Arrow.LEFT, "<white>Quieter",
                "<gray>Volume <white>" + now.volume() + "%", "<dark_gray>Click: 10 % down"), click -> {
            radio.volume(viewer, RadioRule.quieter(radio.volume(viewer)));
            refresh();
        });
        if (now.on()) {
            band(MenuLayout.LAND, 3, Icons.of(Material.BARRIER, "<red>Turn the radio off",
                    "<gray>It stays off until you turn it on again."), click -> {
                radio.stop(viewer);
                refresh();
            });
        } else {
            band(MenuLayout.LAND, 3, Icons.of(Material.JUKEBOX, "<green>Turn the radio on",
                    "<gray>Plays <white>" + now.station().title() + "</white>, the station you had last."), click -> {
                RadioAnswers.tuned(services, viewer, now.station(), radio.turnOn(viewer), false);
                refresh();
            });
        }
        band(MenuLayout.LAND, 5, Icons.of(Material.FEATHER, now.track().isPresent() ? "<white>Skip"
                : "<gray>Skip", "<gray>Another track on the same station."), click -> {
            if (!radio.skip(viewer)) {
                services.messages().send(viewer, "radio.nothing-to-skip");
            }
            refresh();
        });
        band(MenuLayout.LAND, 6, Icons.arrow(Arrow.RIGHT, "<white>Louder",
                "<gray>Volume <white>" + now.volume() + "%", "<dark_gray>Click: 10 % up"), click -> {
            radio.volume(viewer, RadioRule.louder(radio.volume(viewer)));
            refresh();
        });
    }

    private ItemStack stationIcon(Station station, RadioService.Status now) {
        List<String> lore = new ArrayList<>();
        lore.add("<gray>" + station.blurb());
        int count = services.radio().tracks(station).size();
        lore.add(count == 0 ? "<dark_gray>No tracks yet." : "<dark_gray>" + count + (count == 1 ? " track" : " tracks"));
        boolean onAir = now.on() && now.station() == station;
        lore.add(onAir ? "<green>▶ On now" : "<yellow>Click to tune in");
        ItemStack icon = Icons.of(station.icon(), (onAir ? "<green>" : "<white>") + station.title(), lore);
        if (onAir) {
            icon.editMeta(meta -> meta.setEnchantmentGlintOverride(true));
        }
        return icon;
    }

    private static ItemStack playingIcon(RadioService.Status now) {
        if (!now.on()) {
            return Icons.of(Material.NOTE_BLOCK, "<gray>The radio is off",
                    "<gray>Pick a station above to listen.", "<dark_gray>Only you hear it.");
        }
        if (now.track().isEmpty()) {
            return Icons.of(Material.NOTE_BLOCK, "<white>" + now.station().title(),
                    now.waiting() ? "<gray>Starts in a moment." : "<gray>Between two tracks.");
        }
        Track track = now.track().get();
        return Icons.of(Material.NOTE_BLOCK, "<white>♪ " + MINI.escapeTags(track.title()),
                "<gray>On <white>" + now.station().title() + "</white>, " + RadioRule.clock(track.seconds()) + " long",
                "<gray>Volume <white>" + now.volume() + "%");
    }

    @Override
    protected List<String> helpLines() {
        return List.of("<gray>Pick a station to listen. Only you hear it.",
                "<gray>The radio stays on, even after you log off,",
                "<gray>until you turn it off. <white>/music skip</white>, <white>/music stop</white>.");
    }

    @Override
    public String breadcrumb() {
        return "radio";
    }
}
