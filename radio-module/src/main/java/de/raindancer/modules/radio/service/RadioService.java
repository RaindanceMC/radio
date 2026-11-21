package de.raindancer.modules.radio.service;

import de.raindancer.core.content.music.MusicSession;
import de.raindancer.core.content.music.MusicStage;
import de.raindancer.core.content.music.Track;
import de.raindancer.core.content.pack.PackStatus;
import de.raindancer.core.content.pack.PackWait;
import de.raindancer.core.content.pack.ResourcePacks;
import de.raindancer.core.platform.util.Scheduling;
import de.raindancer.core.ui.actionbar.ActionBarPriority;
import de.raindancer.core.ui.actionbar.ActionBars;
import de.raindancer.core.ui.messages.Messages;
import de.raindancer.core.ui.profile.PlayerSwitch;
import de.raindancer.modules.radio.RadioSettings;
import de.raindancer.modules.radio.model.Station;
import de.raindancer.modules.radio.rules.RadioRule;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Each player's own radio: one station's tracks shuffled, heard by them alone, on the jukebox slider. The radio is
 * what they chose, so on Core's {@link MusicStage} it stands above the music on logging on and the AFK camera's,
 * and gives way only to a one-off moment such as a cutscene, coming back after it. Whether it is on, the station
 * and the volume are kept on the player, so it comes back on by itself when they log on again.
 */
public final class RadioService {

    /** Whether the radio is on for this player — off unless they turn it on. */
    public static final PlayerSwitch ON = new PlayerSwitch("rainsradio", "on", false);
    private static final NamespacedKey STATION = new NamespacedKey("rainsradio", "station");
    private static final NamespacedKey VOLUME = new NamespacedKey("rainsradio", "volume");
    private static final String BAR = "radio";
    private static final Duration BAR_LASTS = Duration.ofSeconds(6);

    public enum Tuned {
        /** Playing, or about to once the pack is on. */
        PLAYING,
        /** Kept, but the station has no tracks yet. */
        NO_TRACKS,
        /** The radio is switched off on this server. */
        OFF_HERE,
        /** They turned the server's pack down, and the tracks are in it. */
        NO_PACK
    }

    /** What one player's radio is doing, for the page and for /music status. */
    public record Status(boolean on, Station station, int volume, Optional<Track> track, boolean waiting) {
    }

    private final Plugin plugin;
    private final Supplier<RadioSettings> settings;
    private final Function<Station, List<Track>> tracks;
    private final ResourcePacks packs;
    private final ActionBars bars;
    private final Messages messages;
    private final LongSupplier clock;
    private final Map<UUID, Listening> listening = new ConcurrentHashMap<>();

    public RadioService(Plugin plugin, Supplier<RadioSettings> settings, Function<Station, List<Track>> tracks,
                        ResourcePacks packs, ActionBars bars, Messages messages, LongSupplier clock) {
        this.plugin = plugin;
        this.settings = settings;
        this.tracks = tracks;
        this.packs = packs;
        this.bars = bars;
        this.messages = messages;
        this.clock = clock;
    }

    public List<Track> tracks(Station station) {
        return tracks.apply(station);
    }

    public Station station(Player player) {
        return Station.stored(player.getPersistentDataContainer().get(STATION, PersistentDataType.STRING));
    }

    public int volume(Player player) {
        Integer kept = player.getPersistentDataContainer().get(VOLUME, PersistentDataType.INTEGER);
        return RadioRule.volume(kept == null ? settings.get().defaultVolume() : kept);
    }

    public Status status(Player player) {
        Listening now = listening.get(player.getUniqueId());
        return new Status(now != null, station(player), volume(player),
                now == null ? Optional.empty() : now.track(), now != null && now.session == null);
    }

    /** Turns the radio on at {@code station}, or moves it there. */
    public Tuned tune(Player player, Station station) {
        player.getPersistentDataContainer().set(STATION, PersistentDataType.STRING, station.id());
        if (!settings.get().enabled()) {
            return Tuned.OFF_HERE;
        }
        if (tracks(station).isEmpty()) {
            return Tuned.NO_TRACKS;
        }
        PackStatus pack = packs.statusOf(player.getUniqueId());
        if (pack == PackStatus.DECLINED || pack == PackStatus.FAILED) {
            return Tuned.NO_PACK;
        }
        ON.set(player, true);
        Listening now = listening.get(player.getUniqueId());
        if (now != null) {
            now.retune(station);
        } else {
            listen(player, station);
        }
        return Tuned.PLAYING;
    }

    /** Turns it on at the station kept for them. */
    public Tuned turnOn(Player player) {
        return tune(player, station(player));
    }

    /** Another track on the same station. False when nothing is playing. */
    public boolean skip(Player player) {
        Listening now = listening.get(player.getUniqueId());
        return now != null && now.skip();
    }

    /** Off, and kept off — until they turn it on again. False when it was not on. */
    public boolean stop(Player player) {
        ON.set(player, false);
        Listening now = listening.remove(player.getUniqueId());
        if (now == null) {
            return false;
        }
        now.end();
        return true;
    }

    /** @return the volume it is at now, kept within bounds */
    public int volume(Player player, int percent) {
        int kept = RadioRule.volume(percent);
        player.getPersistentDataContainer().set(VOLUME, PersistentDataType.INTEGER, kept);
        Listening now = listening.get(player.getUniqueId());
        if (now != null) {
            now.volume(kept);
        }
        return kept;
    }

    /** Logging on with the radio left on: it comes back once their pack is. */
    public void joined(Player player) {
        if (settings.get().enabled() && ON.isOn(player) && !tracks(station(player)).isEmpty()) {
            listen(player, station(player));
        }
    }

    /** They left: what plays ends, what they chose stays kept. */
    public void left(UUID player) {
        Listening now = listening.remove(player);
        if (now != null) {
            now.end();
        }
    }

    public void stopAll() {
        for (UUID player : List.copyOf(listening.keySet())) {
            left(player);
        }
    }

    private void listen(Player player, Station station) {
        Listening now = new Listening(player, station);
        Listening before = listening.put(player.getUniqueId(), now);
        if (before != null) {
            before.end();
        }
        now.begin();
    }

    /**
     * One player listening. On the stage from the first moment, before the pack is even on, so the music on
     * logging on sees the radio coming and stays quiet instead of starting and being cut off.
     */
    private final class Listening implements MusicStage.Part {

        private final Player player;
        private final long since = clock.getAsLong();
        private Station station;
        private MusicStage.Cue cue;
        private MusicSession session;
        private boolean packOn;
        private boolean ended;

        Listening(Player player, Station station) {
            this.player = player;
            this.station = station;
        }

        void begin() {
            cue = MusicStage.enter(player.getUniqueId(), MusicStage.Layer.CHOSEN, this);
            packOn = packs.isWearing(player.getUniqueId());
            if (packOn) {
                play();
            }
            Scheduling.entityTimer(plugin, player, 10, 10, task -> {
                if (!tick()) {
                    task.cancel();
                }
            }, this::retired);
        }

        /** The entity went away: they left, or (on some servers) died and came back as a new one. */
        private void retired() {
            UUID id = player.getUniqueId();
            if (!listening.remove(id, this)) {
                return;
            }
            end();
            Scheduling.globalLater(plugin, 20, () -> {
                Player again = plugin.getServer().getPlayer(id);
                if (again != null && !listening.containsKey(id)) {
                    Scheduling.entity(plugin, again, () -> joined(again));
                }
            });
        }

        /** @return whether to keep ticking */
        private synchronized boolean tick() {
            if (ended || !player.isOnline()) {
                return false;
            }
            if (!packOn) {
                switch (PackWait.step(packs.statusOf(player.getUniqueId()), (clock.getAsLong() - since) / 1000.0)) {
                    case WAIT -> {
                        return true;
                    }
                    case GIVE_UP -> {
                        listening.remove(player.getUniqueId(), this);
                        end();
                        messages.send(player, "radio.no-pack");
                        return false;
                    }
                    case GO -> {
                        packOn = true;
                        play();
                    }
                }
            }
            if (session != null) {
                session.tick();
            }
            return true;
        }

        private synchronized void play() {
            if (ended || !packOn || !cue.live()) {
                return;
            }
            if (session != null) {
                session.stop();
            }
            List<Track> list = tracks(station);
            session = list.isEmpty() ? null
                    : new MusicSession(player, player, list, RadioService.this.volume(player) / 100f, clock, this::nowPlaying);
        }

        private void nowPlaying(Track track) {
            if (settings.get().nowPlaying()) {
                bars.show(player.getUniqueId(), BAR, messages.get("radio.now-playing", "title", track.title(),
                        "station", station.title(), "length", RadioRule.clock(track.seconds())), BAR_LASTS,
                        ActionBarPriority.NORMAL);
            }
        }

        synchronized void retune(Station to) {
            station = to;
            play();
        }

        synchronized boolean skip() {
            return session != null && session.skip();
        }

        synchronized void volume(int percent) {
            if (session != null) {
                session.volume(percent / 100f);
            }
        }

        synchronized Optional<Track> track() {
            return session == null ? Optional.empty() : session.current();
        }

        synchronized void end() {
            if (ended) {
                return;
            }
            ended = true;
            silence();
            if (cue != null) {
                cue.leave();
            }
        }

        private void silence() {
            if (session != null) {
                session.stop();
                session = null;
            }
            bars.clear(player.getUniqueId(), BAR);
        }

        @Override
        public synchronized void hush() {
            silence();
        }

        @Override
        public void resume() {
            play();
        }
    }
}
