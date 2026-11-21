package de.raindancer.modules.radio.model;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/** The four stations. Each has a folder of its own tracks: {@code plugins/RainsRadio/music/<id>}. */
public enum Station {

    CALM("calm", "Calm", Material.MUSIC_DISC_FAR, "Slow and soft, for building and thinking."),
    ADVENTURE("adventure", "Adventure", Material.MUSIC_DISC_PIGSTEP, "Big and bold, for exploring and fights."),
    COZY("cozy", "Cozy", Material.MUSIC_DISC_CAT, "Warm and light, for farming and home."),
    NIGHT("night", "Night", Material.MUSIC_DISC_13, "Quiet and dark, for late hours and caves.");

    private final String id;
    private final String title;
    private final Material icon;
    private final String blurb;

    Station(String id, String title, Material icon, String blurb) {
        this.id = id;
        this.title = title;
        this.icon = icon;
        this.blurb = blurb;
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public Material icon() {
        return icon;
    }

    public String blurb() {
        return blurb;
    }

    /** What a player typed: the whole name or the start of it, any case. */
    public static Optional<Station> find(String typed) {
        if (typed == null || typed.isBlank()) {
            return Optional.empty();
        }
        String wanted = typed.strip().toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(station -> station.id.equals(wanted)).findFirst()
                .or(() -> Arrays.stream(values()).filter(station -> station.id.startsWith(wanted)).findFirst());
    }

    /** What was kept for a player; the first station when nothing, or one no longer there. */
    public static Station stored(String id) {
        return Arrays.stream(values()).filter(station -> station.id.equals(id)).findFirst().orElse(CALM);
    }
}
