package dev.by1337.edsl.handle;

import dev.by1337.edsl.context.EventContext;
import dev.by1337.edsl.EventHandler;
import dev.by1337.yaml.YamlValue;
import dev.by1337.yaml.codec.RecordYamlCodecBuilder;
import dev.by1337.yaml.codec.YamlCodec;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OneOfHandler<C extends EventContext<C>> implements EventHandler<C> {
    private final List<EventHandler<C>> list;

    public OneOfHandler(List<EventHandler<C>> list) {
        this.list = list;
    }

    @Override
    public boolean handle(C ctx) {
        try (var enter = ctx.enter("oneOf [", "] -> %s")){
            enter.result(false);
            var iterator = list.iterator();
            while (iterator.hasNext()){
                EventHandler<C> handler = iterator.next();
                if (handler.handle(ctx)) {
                    enter.result(true);
                    return true;
                }
                if (iterator.hasNext()){
                    ctx.log("");
                }
            }
            enter.result(false);
            return false;
        }
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

    public static <C extends EventContext<C>> YamlCodec<OneOfHandler<C>> codec(YamlCodec<EventHandler<C>> codec) {
        return RecordYamlCodecBuilder.mapOf(
                OneOfHandler::new,
                codec.listOf().fieldOf("oneOf", OneOfHandler::list)
        );
    }
}
