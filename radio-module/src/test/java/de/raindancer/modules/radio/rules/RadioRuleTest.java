package de.raindancer.modules.radio.rules;

import de.raindancer.modules.radio.model.Station;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("the radio's rules: finding a station by what was typed, the volume")
class RadioRuleTest {

    @Test
    @DisplayName("a station by its name, any case, or by the start of it")
    void find() {
        assertThat(Station.find("calm")).contains(Station.CALM);
        assertThat(Station.find("NIGHT")).contains(Station.NIGHT);
        assertThat(Station.find("adv")).contains(Station.ADVENTURE);
        assertThat(Station.find(" cozy ")).contains(Station.COZY);
        assertThat(Station.find("")).isEmpty();
        assertThat(Station.find("jazz")).isEmpty();
        assertThat(Station.find(null)).isEmpty();
    }

    @Test
    @DisplayName("a stored station that is no longer there falls back to the first")
    void stored() {
        assertThat(Station.stored("night")).isEqualTo(Station.NIGHT);
        assertThat(Station.stored("gone")).isEqualTo(Station.CALM);
        assertThat(Station.stored(null)).isEqualTo(Station.CALM);
    }

    @Test
    @DisplayName("the volume stays between 5 and 100 %")
    void volume() {
        assertThat(RadioRule.volume(0)).isEqualTo(5);
        assertThat(RadioRule.volume(250)).isEqualTo(100);
        assertThat(RadioRule.volume(40)).isEqualTo(40);
    }

    @Test
    @DisplayName("louder and quieter go in tens, landing on a ten")
    void steps() {
        assertThat(RadioRule.louder(45)).isEqualTo(50);
        assertThat(RadioRule.louder(100)).isEqualTo(100);
        assertThat(RadioRule.quieter(45)).isEqualTo(40);
        assertThat(RadioRule.quieter(10)).isEqualTo(5);
        assertThat(RadioRule.quieter(5)).isEqualTo(5);
    }

    @Test
    @DisplayName("a typed volume: a number, with or without %, else nothing")
    void typed() {
        assertThat(RadioRule.typedVolume("60")).hasValue(60);
        assertThat(RadioRule.typedVolume("60%")).hasValue(60);
        assertThat(RadioRule.typedVolume("500")).hasValue(100);
        assertThat(RadioRule.typedVolume("loud")).isEmpty();
    }

    @Test
    @DisplayName("a minute and a half reads 1:30")
    void length() {
        assertThat(RadioRule.clock(90.4)).isEqualTo("1:30");
        assertThat(RadioRule.clock(3725)).isEqualTo("62:05");
    }
}
