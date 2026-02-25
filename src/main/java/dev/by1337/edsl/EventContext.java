package dev.by1337.edsl;

import dev.by1337.edsl.trace.EventTracer;

public interface EventContext {
    EventTracer tracer();

    default EventTracer.TracerDepth enter(String name, String exit) {
        return tracer().enter(name, exit);
    }

    default void log(String s, Object... args) {
        tracer().log(s, args);
    }

    default void log(String s) {
        tracer().log(s);
    }

}
