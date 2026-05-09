package dev.by1337.edsl.context;

import dev.by1337.cmd.Command;
import dev.by1337.edsl.trace.EventTracer;
import dev.by1337.plc.PapiResolver;
import dev.by1337.plc.PlaceholderApplier;
import dev.by1337.plc.PlaceholderResolver;
import dev.by1337.plc.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Objects;

public abstract class EventContext<T extends EventContext<T>> implements PlaceholderApplier, AutoCloseable {
    private static final PlaceholderResolver<EventContext<?>> PLACEHOLDERS;
    public final EventTracer tracer;
    private final @Nullable StringBuilder tracerLog;
    private final @Nullable Player player;
    private final PlaceholderResolver<T> placeholders;
    private Player target;
    private Location location;

    public EventContext(boolean debug) {
        this(debug, null);
    }

    public EventContext(boolean debug, @Nullable Player player) {
        this(debug, player, null);
    }

    public EventContext(boolean debug, @Nullable Player player, @Nullable PlaceholderResolver<T> placeholders) {
        if (placeholders == null) {
            // noinspection unchecked
            this.placeholders = (PlaceholderResolver<T>) PLACEHOLDERS;
        } else {
            // noinspection unchecked
            this.placeholders = placeholders.and((PlaceholderResolver<T>) PLACEHOLDERS);
        }
        this.player = target = player;
        if (player != null) {
            setLocation(player.getLocation());
        }
        if (debug) {
            tracerLog = new StringBuilder();
            tracer = EventTracer.performanceTracer(tracerLog);
        } else {
            tracer = EventTracer.NOP;
            tracerLog = null;
        }
    }

    public Location location() {
        return location;
    }

    protected void setLocation(Location location) {
        this.location = location;
    }

    public @Nullable Player source() {
        return player;
    }

    public @Nullable Player target() {
        return target;
    }

    public Scope target(Player player) {
        Objects.requireNonNull(player);
        // noinspection all
        var v = tracer.enter("%s -> {", player.getName(), "} -> %s");
        Player old = target;
        this.target = player;
        return () -> {
            target = old;
            v.close();
        };
    }

    @Override
    public String setPlaceholders(String s) {
        // noinspection unchecked
        return placeholders.setPlaceholders(s, (T) this);
    }

    @Override
    public void close() {
        if (tracerLog != null) {
            getLogger().info("TRACER:\n{}", tracerLog);
        }
    }

    public abstract Command<T> command();

    public abstract Logger getLogger();

    public EventTracer tracer() {
        return tracer;
    }

    public EventTracer.TracerDepth enter(String name, String exit) {
        return tracer().enter(name, exit);
    }

    public void log(String s, Object... args) {
        tracer().log(s, args);
    }

    public void log(String s) {
        tracer().log(s);
    }

    public interface Scope extends AutoCloseable {
        @Override
        void close();
    }

    static {
        // noinspection all
        if (Bukkit.getServer() == null) {
            PLACEHOLDERS = Placeholders.create();
        } else {
            // noinspection all
            PLACEHOLDERS = Placeholders.<EventContext<?>>create()
                    .of("target", (papi, ctx) ->
                            PapiResolver.INSTANCE.setPlaceholders("%" + papi + "%", ctx.target))
                    .and(PapiResolver.INSTANCE.map(EventContext::source));
        }
    }
}
