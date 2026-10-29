package dev.by1337.core.event;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Synchronous, thread-safe state notifications with no history or replay.
 * A non-null UUID identifies a player; null means one global notification per subscriber,
 * without enumerating players. Identifiers are case-sensitive and compared by String equality.
 * <p>
 * Handlers run in the emitter's thread and must obey Bukkit's threading requirements themselves.
 * Concurrent emitters may call the same handler concurrently; recursive emit is allowed.
 * Each emit reads one subscription snapshot, in registration order. New subscriptions only
 * affect subsequent snapshots. Closing a subscription skips it if its callback has not yet
 * been read; a callback already read by another thread may still run after close returns.
 * A handler may close itself or another subscription during delivery.
 * <p>
 * Publication performs no mandatory allocations or locking. Subscription changes are serialized
 * and replace immutable arrays. Bukkit is used only by the host for plugin lifecycle cleanup.
 */
public final class EventBus implements Closeable {
    private final ConcurrentHashMap<String, Registration[]> listeners = new ConcurrentHashMap<>();
    private final Map<Plugin, Set<Registration>> owners = new IdentityHashMap<>();
    private final Logger logger;
    private boolean closed;

    public EventBus(Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    /**
     * Registers an unowned handler. The caller must close the subscription (for example through
     * BasePlugin.addCloseHook). Prefer the owner overload for automatic cleanup on plugin disable.
     */
    public Subscription on(String eventId, Consumer<@Nullable UUID> handler) {
        return register(null, eventId, handler);
    }

    /**
     * Registers a handler owned by an enabled plugin. BCore's shared bus automatically removes
     * it on PluginDisableEvent. Disabled plugins and closed buses reject new subscriptions.
     * Plugins register fresh subscriptions in onEnable, including when reusing a Plugin instance.
     */
    public Subscription on(Plugin owner, String eventId, Consumer<@Nullable UUID> handler) {
        return register(Objects.requireNonNull(owner, "owner"), eventId, handler);
    }

    private synchronized Subscription register(@Nullable Plugin owner, String eventId, Consumer<@Nullable UUID> handler) {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(handler, "handler");
        if (closed) throw new IllegalStateException("Event bus is closed");
        if (owner != null && !owner.isEnabled())
            throw new IllegalStateException("Plugin is disabled: " + owner.getName());
        Registration registration = new Registration(eventId, owner, handler);
        if (owner != null) owners.computeIfAbsent(owner, ignored -> new HashSet<>()).add(registration);
        Registration[] current = listeners.get(eventId);
        Registration[] next = current == null ? new Registration[1] : Arrays.copyOf(current, current.length + 1);
        next[next.length - 1] = registration;
        listeners.put(eventId, next);
        // Bukkit can change enabled state while registration is in progress.
        // Roll back if the plugin became disabled before this subscription was returned.
        if (owner != null && !owner.isEnabled()) registration.close();
        return registration;
    }

    /**
     * Delivers once to each active handler in the current snapshot, in the calling thread.
     */
    public void emit(String eventId, @Nullable UUID playerId) {
        Registration[] snapshot = listeners.get(eventId);
        if (snapshot == null) return;
        for (Registration registration : snapshot) {
            Consumer<@Nullable UUID> handler = registration.handler;
            if (handler == null) continue;
            try {
                handler.accept(playerId);
            } catch (Throwable error) {
                logger.log(Level.SEVERE, "Event handler failed for '" + eventId + "' (owner: " + registration.ownerName + ")", error);
            }
        }
    }

    /**
     * Removes a plugin's current subscriptions without preventing later registration.
     */
    public synchronized void unsubscribe(Plugin owner) {
        Set<Registration> registrations = owners.remove(Objects.requireNonNull(owner, "owner"));
        if (registrations == null) return;
        for (Registration registration : registrations) remove(registration);
    }

    /**
     * Permanently closes the bus and releases all callbacks and owners. Further emits do nothing.
     */
    @Override
    public synchronized void close() {
        closed = true;
        for (Registration[] registrations : listeners.values()) {
            for (Registration registration : registrations) {
                registration.handler = null;
                registration.owner = null;
            }
        }
        listeners.clear();
        owners.clear();
    }

    // All writes are protected by the bus monitor; emit only reads published arrays and handlers.
    private void remove(Registration registration) {
        if (registration.handler == null) return;
        registration.handler = null;
        registration.owner = null;
        Registration[] current = listeners.get(registration.eventId);
        if (current.length == 1) {
            listeners.remove(registration.eventId);
            return;
        }
        for (int i = 0; i < current.length; i++) {
            if (current[i] == registration) {
                Registration[] next = new Registration[current.length - 1];
                System.arraycopy(current, 0, next, 0, i);
                System.arraycopy(current, i + 1, next, i, current.length - i - 1);
                listeners.put(registration.eventId, next);
                return;
            }
        }
    }

    private final class Registration implements Subscription {
        private final String eventId;
        private final String ownerName;
        private @Nullable Plugin owner;
        private volatile @Nullable Consumer<@Nullable UUID> handler;

        private Registration(String eventId, @Nullable Plugin owner, Consumer<@Nullable UUID> handler) {
            this.eventId = eventId;
            this.owner = owner;
            this.ownerName = owner == null ? "unowned" : owner.getName();
            this.handler = handler;
        }

        @Override
        public void close() {
            synchronized (EventBus.this) {
                if (handler == null) return;
                if (owner != null) {
                    Set<Registration> registrations = owners.get(owner);
                    registrations.remove(this);
                    if (registrations.isEmpty()) owners.remove(owner);
                }
                remove(this);
            }
        }
    }
}
