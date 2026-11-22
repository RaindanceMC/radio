package de.raindancer.modules.radio.screen;

import de.raindancer.core.content.music.Track;
import de.raindancer.core.ui.dialog.DialogAnswers;
import de.raindancer.core.ui.dialog.DialogPage;
import de.raindancer.modules.radio.model.Station;
import de.raindancer.modules.radio.service.RadioService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("the radio as the game's own window")
class RadioDialogTest {

    private static final Map<Station, Integer> COUNTS = Map.of(Station.CALM, 19, Station.ADVENTURE, 17,
            Station.COZY, 0, Station.NIGHT, 11);
    private static final Track MOON = new Track("rainsradio", "night.moon_yd", 105, "Moon - yd");

    private final Player viewer = mock(Player.class);
    private final List<DialogPage> shown = new ArrayList<>();
    private final List<String> done = new ArrayList<>();

    @BeforeEach
    void standIn() {
        when(viewer.isOnline()).thenReturn(true);
        DialogPage.runtime(new DialogPage.Runtime() {
            @Override
            public void show(Player player, DialogPage page) {
                shown.add(page);
            }

            @Override
            public void onPlayerThread(Player player, Runnable task) {
                task.run();
            }
        });
    }

    @AfterEach
    void game() {
        DialogPage.runtime(null);
    }

    private final RadioDialog.Controls controls = new RadioDialog.Controls() {
        @Override
        public RadioService.Tuned tune(Station station) {
            done.add("tune " + station.id());
            return station == Station.COZY ? RadioService.Tuned.NO_TRACKS : RadioService.Tuned.PLAYING;
        }

        @Override
        public boolean skip() {
            done.add("skip");
            return true;
        }

        @Override
        public void turnOn() {
            done.add("on");
        }

        @Override
        public void turnOff() {
            done.add("off");
        }

        @Override
        public void volume(int percent) {
            done.add("volume " + percent);
        }

        @Override
        public void reopen(String notice) {
            done.add(notice == null ? "reopen" : "reopen: " + notice);
        }
    };

    private static String plain(Component text) {
        return PlainTextComponentSerializer.plainText().serialize(text);
    }

    private static List<String> labels(DialogPage page) {
        return page.pressable().stream().map(button -> plain(button.label())).toList();
    }

    private DialogPage playing() {
        return RadioDialog.page(new RadioService.Status(true, Station.NIGHT, 60, Optional.of(MOON), false), COUNTS,
                controls);
    }

    @Test
    @DisplayName("playing: the track, station and volume on top, four stations, then skip, off and the volume")
    void layout() {
        DialogPage page = playing();

        assertThat(plain(page.title())).contains("Radio");
        assertThat(page.plainText()).contains("Moon - yd").contains("Night").contains("1:45").contains("60%");
        assertThat(labels(page)).containsExactly("Calm", "Adventure", "Cozy", "▶ Night", "Skip", "Turn off",
                "− Quieter", "+ Louder");
        assertThat(plain(page.button("Cozy").tooltip())).contains("No tracks yet");
        assertThat(plain(page.button("Calm").tooltip())).contains("19 tracks");
    }

    @Test
    @DisplayName("off: says so, offers to turn on, and skip is greyed with the reason")
    void off() {
        DialogPage page = RadioDialog.page(new RadioService.Status(false, Station.CALM, 60, Optional.empty(), false),
                COUNTS, controls);

        assertThat(page.plainText()).contains("off");
        assertThat(labels(page)).contains("Calm", "Turn on").doesNotContain("▶ Calm", "Turn off");
        page.button("Skip").press(viewer, DialogAnswers.none());
        assertThat(done).doesNotContain("skip");
        assertThat(shown).singleElement().satisfies(again -> assertThat(again.notice()).contains("off"));

        page.button("Turn on").press(viewer, DialogAnswers.none());
        assertThat(done).containsExactly("on", "reopen");
    }

    @Test
    @DisplayName("a station tunes in and the window comes back; one without tracks says why in it")
    void tune() {
        playing().button("Calm").press(viewer, DialogAnswers.none());
        playing().button("Cozy").press(viewer, DialogAnswers.none());

        assertThat(done).containsExactly("tune calm", "reopen", "tune cozy", "reopen: Cozy has no tracks yet.");
    }

    @Test
    @DisplayName("quieter and louder go in tens and stay between 5 and 100 %")
    void volume() {
        playing().button("− Quieter").press(viewer, DialogAnswers.none());
        playing().button("+ Louder").press(viewer, DialogAnswers.none());
        RadioDialog.page(new RadioService.Status(true, Station.NIGHT, 100, Optional.of(MOON), false), COUNTS, controls)
                .button("+ Louder").press(viewer, DialogAnswers.none());

        assertThat(done).containsExactly("volume 50", "reopen", "volume 70", "reopen", "volume 100", "reopen");
    }

    @Test
    @DisplayName("skip and off do what they say and come back")
    void skipAndOff() {
        playing().button("Skip").press(viewer, DialogAnswers.none());
        playing().button("Turn off").press(viewer, DialogAnswers.none());

        assertThat(done).containsExactly("skip", "reopen", "off", "reopen");
    }
}
