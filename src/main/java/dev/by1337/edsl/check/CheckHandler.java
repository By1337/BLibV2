package dev.by1337.edsl.check;

import dev.by1337.cmd.Command;
import dev.by1337.edsl.EventHandler;
import dev.by1337.plc.PlaceholderApplier;
import dev.by1337.yaml.YamlValue;

public interface CheckHandler<T> extends EventHandler<T> {
    CheckHandler<Void> TRUE = new CheckHandler<Void>() {
        @Override
        public boolean handle(Void ctx, PlaceholderApplier placeholders, Command<Void> executor) {
            return true;
        }

        @Override
        public YamlValue encode() {
            return YamlValue.wrap("true");
        }
    };
    CheckHandler<Void> FALSE = new CheckHandler<Void>() {
        @Override
        public boolean handle(Void ctx, PlaceholderApplier placeholders, Command<Void> executor) {
            return false;
        }

        @Override
        public YamlValue encode() {
            return YamlValue.wrap("false");
        }
    };

    static <T> CheckHandler<T> nopTrue() {
        // noinspection unchecked
        return (CheckHandler<T>) TRUE;
    }

    static <T> CheckHandler<T> nopFalse() {
        // noinspection unchecked
        return (CheckHandler<T>) FALSE;
    }
}
