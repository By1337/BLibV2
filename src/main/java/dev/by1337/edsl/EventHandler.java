package dev.by1337.edsl;

import dev.by1337.cmd.Command;
import dev.by1337.plc.PlaceholderApplier;
import dev.by1337.yaml.YamlValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface EventHandler<C> {

    boolean handle(C ctx, PlaceholderApplier placeholders, Command<C> executor);

    @Nullable YamlValue encode();

    default @NotNull YamlValue doEncode() {
        YamlValue res = encode();
        return res != null ? res : YamlValue.wrap("unsupported " + this.getClass().getSimpleName());
    }

    static <T> EventHandler<T> nop() {
        return new EventHandler<T>() {
            @Override
            public boolean handle(T ctx, PlaceholderApplier placeholders, Command<T> executor) {
                return true;
            }

            @Override
            public @Nullable YamlValue encode() {
                return null;
            }
        };
    }
}
