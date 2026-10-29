package dev.by1337.core.event;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;

import java.io.Closeable;

/** Bukkit lifecycle adapter; never involved in notification delivery. */
@ApiStatus.Internal
public final class EventBusLifecycle implements Listener, Closeable {
    private final Plugin host;
    private final EventBus bus;

    public EventBusLifecycle(Plugin host, EventBus bus) {
        this.host = host;
        this.bus = bus;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDisable(PluginDisableEvent event) {
        if (event.getPlugin() == host) close();
        else bus.unsubscribe(event.getPlugin());
    }

    @Override
    public void close() {
        bus.close();
        HandlerList.unregisterAll(this);
    }
}
