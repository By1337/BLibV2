package dev.by1337.edsl;

import dev.by1337.cmd.Command;
import dev.by1337.edsl.trace.EventTracer;
import dev.by1337.plc.PlaceholderApplier;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public abstract class EventContext<T extends EventContext<T>> implements PlaceholderApplier, AutoCloseable {
    public final EventTracer tracer;
    private final @Nullable StringBuilder tracerLog;

    public EventContext(boolean debug) {
        if (debug) {
            tracerLog = new StringBuilder();
            tracer = EventTracer.performanceTracer(tracerLog);
        } else {
            tracer = EventTracer.NOP;
            tracerLog = null;
        }
    }

    @Override
    public void close() {
        if (tracerLog != null){
            getLogger().info("TRACER:\n{}", tracerLog);
        }
    }

    public abstract Command<T> command();
    public abstract Logger getLogger();


    public EventTracer tracer(){
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
}
