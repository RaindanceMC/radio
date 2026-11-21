package de.raindancer.modules.radio.listener;

import de.raindancer.modules.radio.service.RadioService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** The radio comes back on when somebody who left it on logs on, and ends when they leave. */
public final class RadioListener implements Listener {

    private final RadioService radio;

    public RadioListener(RadioService radio) {
        this.radio = radio;
    }

    // LOW: before the music on logging on (MONITOR), so that one sees the radio coming and stays quiet.
    @EventHandler(priority = EventPriority.LOW)
    public void onJoin(PlayerJoinEvent event) {
        radio.joined(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        radio.left(event.getPlayer().getUniqueId());
    }
}
