package de.raindancer.modules.radio;

import de.raindancer.core.data.settings.Describe;
import de.raindancer.core.data.settings.In;
import de.raindancer.core.data.settings.Key;
import de.raindancer.core.data.settings.Range;
import de.raindancer.core.data.settings.Settings;
import de.raindancer.core.data.settings.Title;
import de.raindancer.core.data.settings.Topic;
import org.bukkit.Material;

/** The radio for the whole server. Which station and how loud is each player's own, kept on the player. */
@Settings(id = "radio", topics = {
        @Topic(path = "radio", title = "Radio", icon = Material.JUKEBOX,
                description = "Four stations each player can tune in with /music, heard by them alone."),
})
public record RadioSettings(

        @In("radio") @Title("Radio on this server")
        @Describe("Whether /music plays anything. Each station plays the .ogg (Vorbis) files in "
                + "plugins/RainsRadio/music/<station>; new tracks come with the next restart.")
        @Key("enabled") boolean enabled,

        @In("radio") @Title("Volume to start with (%)")
        @Describe("How loud the radio is for somebody who has never turned it up or down.")
        @Range(min = 5, max = 100)
        @Key("default-volume") int defaultVolume,

        @In("radio") @Title("Show what is playing")
        @Describe("Each new track's title and station on the action bar for a few seconds.")
        @Key("now-playing") boolean nowPlaying) {

    public static final RadioSettings DEFAULTS = new RadioSettings(true, 60, true);
}
