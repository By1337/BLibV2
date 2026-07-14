package dev.by1337.edsl;

import dev.by1337.edsl.codec.EventHandlerCodec;
import dev.by1337.edsl.context.EventContext;

@FunctionalInterface
public interface EventHandler {
    static EventHandlerCodec decoder() {
        return new EventHandlerCodec();
    }

    boolean handle(EventContext ctx);

    default boolean handleAndClose(EventContext ctx) {
        try (ctx) {
            return handle(ctx);
        }
    }

    static EventHandler nop() {
        return ctx -> {
            ctx.tracer().log("nop -> {}");
            return true;
        };
    }
}
