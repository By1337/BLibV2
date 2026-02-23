package dev.by1337.edsl.handle;

import dev.by1337.cmd.Command;
import dev.by1337.edsl.EventHandler;
import dev.by1337.edsl.HandlerCodecBuilder;
import dev.by1337.plc.PlaceholderApplier;
import dev.by1337.yaml.YamlValue;
import dev.by1337.yaml.codec.RecordYamlCodecBuilder;
import dev.by1337.yaml.codec.YamlCodec;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OneOfHandler<C> implements EventHandler<C> {
    private final List<EventHandler<C>> list;

    public OneOfHandler(List<EventHandler<C>> list) {
        this.list = list;
    }

    @Override
    public boolean handle(C ctx, PlaceholderApplier placeholders, Command<C> executor) {
        for (EventHandler<C> handler : list) {
            if (handler.handle(ctx, placeholders, executor)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public @Nullable YamlValue encode() {
        List<Object> result = new ArrayList<>();
        for (EventHandler<C> handler : list) {
            var v = handler.encode();
            if (v != null) result.add(v.getRaw());
        }
        return !result.isEmpty() ? YamlValue.wrap(Map.of("oneOf", result)) : null;
    }

    public List<EventHandler<C>> list() {
        return list;
    }

    public static <C> YamlCodec<OneOfHandler<C>> createCodec(HandlerCodecBuilder<C> b) {
        return YamlCodec.lazyLoad(() -> RecordYamlCodecBuilder.mapOf(
                OneOfHandler::new,
                b.handlerCodec().listOf().fieldOf("oneOf", OneOfHandler::list)
        ));
    }
}
