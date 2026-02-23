package dev.by1337.edsl.handle;

import dev.by1337.cmd.Command;
import dev.by1337.edsl.EventHandler;
import dev.by1337.plc.PlaceholderApplier;
import dev.by1337.yaml.YamlValue;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ListHandler<C> implements EventHandler<C> {
    private final List<EventHandler<C>> list;

    public ListHandler(List<EventHandler<C>> list) {
        this.list = list;
    }

    @Override
    public boolean handle(C ctx, PlaceholderApplier placeholders, Command<C> executor) {
        boolean state = true;
        for (EventHandler<C> handler : list) {
            if (!handler.handle(ctx, placeholders, executor)) {
                state = false;
            }
        }
        return state;
    }

    @Override
    public @Nullable YamlValue encode() {
        List<Object> result = new ArrayList<>();
        for (EventHandler<C> handler : list) {
            var v = handler.encode();
            if (v != null) result.add(v.getRaw());
        }
        return !result.isEmpty() ? YamlValue.wrap(result) : null;
    }

    public List<EventHandler<C>> list() {
        return list;
    }
}
