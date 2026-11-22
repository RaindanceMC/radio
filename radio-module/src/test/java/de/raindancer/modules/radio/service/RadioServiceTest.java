package de.raindancer.modules.radio.service;

import de.raindancer.core.content.music.Track;
import de.raindancer.core.content.pack.PackStatus;
import de.raindancer.core.content.pack.ResourcePacks;
import de.raindancer.modules.radio.RadioSettings;
import de.raindancer.modules.radio.model.Station;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("the radio service: what is kept for a player")
class RadioServiceTest {

    private final Map<NamespacedKey, Object> kept = new HashMap<>();
    private final Player player = mock(Player.class);
    private final ResourcePacks packs = mock(ResourcePacks.class);

    private RadioService radio() {
        PersistentDataContainer data = mock(PersistentDataContainer.class);
        when(player.getPersistentDataContainer()).thenReturn(data);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        doAnswer(call -> kept.put(call.getArgument(0), call.getArgument(2))).when(data).set(any(), any(), any());
        when(data.get(any(), any())).thenAnswer(call -> kept.get(call.<NamespacedKey>getArgument(0)));
        when(packs.statusOf(any())).thenReturn(PackStatus.DECLINED);
        Track dawn = new Track("rainsradio", "calm.dawn", 60, "Dawn");
        return new RadioService(null, () -> RadioSettings.DEFAULTS,
                station -> station == Station.CALM ? List.of(dawn) : List.of(), packs, null, null, () -> 0L);
    }

    @Test
    @DisplayName("a station with no tracks is not kept: the one before stays, so logging on again still plays")
    void emptyStationNotKept() {
        RadioService radio = radio();
        kept.put(new NamespacedKey("rainsradio", "station"), "calm");

        assertThat(radio.tune(player, Station.COZY)).isEqualTo(RadioService.Tuned.NO_TRACKS);

        assertThat(radio.station(player)).isEqualTo(Station.CALM);
    }

    @Test
    @DisplayName("a station with tracks is kept, even when the pack is missing right now")
    void stationKept() {
        RadioService radio = radio();

        assertThat(radio.tune(player, Station.CALM)).isEqualTo(RadioService.Tuned.NO_PACK);

        assertThat(radio.station(player)).isEqualTo(Station.CALM);
    }
}
