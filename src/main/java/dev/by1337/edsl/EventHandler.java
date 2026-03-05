package dev.by1337.edsl;

import dev.by1337.edsl.codec.EventHandlerCodec;
import dev.by1337.yaml.YamlValue;
import dev.by1337.yaml.codec.YamlCodec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface EventHandler<C extends EventContext<C>> {
    static <T extends EventContext<T>> YamlCodec<EventHandler<T>> codec() {
        return new EventHandlerCodec<T>().make();
    }

    boolean handle(C ctx);

    @Nullable YamlValue encode();

    default @NotNull YamlValue doEncode() {
        YamlValue res = encode();
        return res != null ? res : YamlValue.wrap("unsupported " + this.getClass().getSimpleName());
    }

    static <T extends EventContext<T>> EventHandler<T> nop() {
        return new EventHandler<T>() {
            @Override
            public boolean handle(T ctx) {
                ctx.log("nop -> {}");
                return true;
            }

            @Override
            public @Nullable YamlValue encode() {
                return null;
            }
        };
    }
}
