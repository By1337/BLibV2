package dev.by1337.edsl;

import dev.by1337.edsl.codec.EventHandlerCodec;
import dev.by1337.edsl.context.EventContext;
import dev.by1337.yaml.decoder.YamlDecoder;

@FunctionalInterface
public interface EventHandler<C extends EventContext<C>> {
    static <T extends EventContext<T>> YamlDecoder<EventHandler<T>> decoder() {
        return new EventHandlerCodec<>();
    }

    boolean handle(C ctx);

    default boolean handleAndClose(C ctx) {
        try (ctx) {
            return handle(ctx);
        }
    }

    static <T extends EventContext<T>> EventHandler<T> nop() {
        return ctx -> {
            ctx.log("nop -> {}");
            return true;
        };
    }
}
