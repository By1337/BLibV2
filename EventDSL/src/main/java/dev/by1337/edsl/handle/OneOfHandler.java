package dev.by1337.edsl.handle;

import dev.by1337.edsl.context.EventContext;
import dev.by1337.edsl.EventHandler;
import dev.by1337.yaml.YamlValue;
import dev.by1337.yaml.codec.RecordYamlCodecBuilder;
import dev.by1337.yaml.codec.YamlCodec;
import dev.by1337.yaml.decoder.YamlDecoder;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OneOfHandler implements EventHandler {
    private final List<EventHandler> list;

    public OneOfHandler(List<EventHandler> list) {
        this.list = list;
    }

    @Override
    public boolean handle(EventContext ctx) {
        try (var enter = ctx.tracer().enter("oneOf [", "] -> %s")) {
            enter.result(false);
            var iterator = list.iterator();
            while (iterator.hasNext()) {
                EventHandler handler = iterator.next();
                if (handler.handle(ctx)) {
                    enter.result(true);
                    return true;
                }
                if (iterator.hasNext()) {
                    ctx.tracer().log("");
                }
            }
            enter.result(false);
            return false;
        }
    }

    public List<EventHandler> list() {
        return list;
    }

    public static YamlDecoder<OneOfHandler> decoder(YamlDecoder<EventHandler> codec) {
        return codec.listOf().map(OneOfHandler::new);
    }
}
