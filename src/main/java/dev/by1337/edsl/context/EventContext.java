package dev.by1337.edsl.context;

import dev.by1337.cmd.Command;
import dev.by1337.edsl.messages.Messages;
import dev.by1337.edsl.trace.EventTracer;
import dev.by1337.plc.PapiResolver;
import dev.by1337.plc.PlaceholderApplier;
import dev.by1337.plc.PlaceholderResolver;
import dev.by1337.plc.Placeholders;
import dev.by1337.yaml.decoder.DecodeContext;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EventContext implements PlaceholderApplier, AutoCloseable {
    private static final PlaceholderResolver<EventContext> PLACEHOLDERS;
    private static final Logger log = LoggerFactory.getLogger(EventContext.class);
    public final EventTracer tracer;
    private final @Nullable StringBuilder tracerLog;
    private PlaceholderResolver<EventContext> placeholders;

    private final @Nullable Player source;
    private @Nullable Player target;
    private Messages messages;
    private Command<EventContext> command;
    private List<Object> context = new ArrayList<>();

    public EventContext(boolean debug) {
        this(debug, null);
    }

    public EventContext(boolean debug, @Nullable Player source) {
        this(debug, source, null);
    }

    public EventContext(boolean debug, @Nullable Player source, @Nullable PlaceholderResolver<EventContext> placeholders) {
        if (placeholders == null) {
            this.placeholders = PLACEHOLDERS;
        } else {
            this.placeholders = placeholders.and(PLACEHOLDERS);
        }
        this.source = target = source;
        if (source != null) {
            context.add(source.getLocation());
        }
        if (debug) {
            tracerLog = new StringBuilder();
            tracer = EventTracer.performanceTracer(tracerLog);
        } else {
            tracer = EventTracer.NOP;
            tracerLog = null;
        }
    }

    public <T> Scope with(T o) {
        List<Object> old = this.context;
        this.context = new ArrayList<>(this.context.size() + 1);
        this.context.add(o);
        this.context.addAll(old);
        return () -> this.context = old;
    }

    public <T> @Nullable T get(Class<T> type){
        for (int i = this.context.size() - 1; i >= 0; i--) {
            var o = this.context.get(i);
            if (type.isInstance(o)) {
                return (T) o;
            }
        }
        return null;
    }

    public @Nullable Player source() {
        return source;
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
        return placeholders.setPlaceholders(s, this);
    }

    @Override
    public void close() {
        if (tracerLog != null && !tracerLog.isEmpty()) {
            getLogger().info("TRACER:\n{}", tracerLog);
        }
    }

    public Command<EventContext> command() {
        return command;
    }

    public EventContext setCommand(Command<EventContext> command) {
        this.command = command;
        return this;
    }

    public Logger getLogger() {
        return log;
    }

    public EventTracer tracer() {
        return tracer;
    }

    public Messages messages() {
        return messages;
    }

    public EventContext messages(Messages v) {
        messages = v;
        return this;
    }

    public void setMessages(Messages messages) {
        this.messages = messages;
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
            PLACEHOLDERS = Placeholders.<EventContext>create()
                    .of("target", (papi, ctx) ->
                            PapiResolver.INSTANCE.setPlaceholders("%" + papi + "%", ctx.target))
                    .and(PapiResolver.INSTANCE.map(EventContext::source));
        }
    }
}
