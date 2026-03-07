package dev.by1337.edsl.handle;

import dev.by1337.edsl.context.EventContext;
import dev.by1337.edsl.EventHandler;
import dev.by1337.edsl.check.CheckHandler;
import dev.by1337.yaml.YamlValue;
import dev.by1337.yaml.codec.DataResult;
import dev.by1337.yaml.codec.RecordYamlCodecBuilder;
import dev.by1337.yaml.codec.YamlCodec;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class ConditionalHandler<C extends EventContext<C>> implements EventHandler<C> {
    private final CheckHandlerUnion<C> tester;
    private final EventHandler<C> success;
    private final EventHandler<C> failure;

    private ConditionalHandler(CheckHandlerUnion<C> tester, EventHandler<C> success, EventHandler<C> failure) {
        this.tester = tester;
        this.success = success;
        this.failure = failure;
    }

    @Override
    public boolean handle(C ctx) {
        try (var enter = ctx.enter("if-do {", "} -> %s")){
            if (tester.check.handle(ctx)) {
                if (success != null)
                    success.handle(ctx);
                enter.result(true);
                return true;
            }
            if (failure != null)
                failure.handle(ctx);
            enter.result(false);
            return false;
        }
    }

    @Override
    public YamlValue encode() {
        Map<String, Object> map = new LinkedHashMap<>();
        if (tester.oneOf) applyIfEncoded(tester.check, v -> map.put("if-one", v.getRaw()));
        else applyIfEncoded(tester.check, v -> map.put("if", v.getRaw()));
        applyIfEncoded(success, v -> map.put("do", v.getRaw()));
        applyIfEncoded(failure, v -> map.put("else", v.getRaw()));
        if (map.isEmpty()) return null;
        return YamlValue.wrap(map);
    }

    private static void applyIfEncoded(EventHandler<?> h, Consumer<YamlValue> c) {
        if (h == null) return;
        YamlValue v = h.encode();
        if (v != null) c.accept(v);
    }


    public static <T extends EventContext<T>> YamlCodec<ConditionalHandler<T>> codec(YamlCodec<EventHandler<T>> codec) {
        return RecordYamlCodecBuilder.mapOf(
                ConditionalHandler::new,
                CheckHandlerUnion.<T>codec().fieldOf(null, v -> v.tester),
                codec.fieldOf("do", v -> v.success),
                codec.fieldOf("else", v -> v.success)
        );
    }

    //todo мне не нравится(
    private record CheckHandlerUnion<T extends EventContext<T>>(CheckHandler<T> check, boolean oneOf) {
        private static <T extends EventContext<T>> YamlCodec<CheckHandlerUnion<T>> codec() {
            return new YamlCodec<>() {
                private final YamlCodec<CheckHandler<T>> oneOfCodec = CheckHandler.codec(true);
                private final YamlCodec<CheckHandler<T>> allOfCodec = CheckHandler.codec(false);

                @Override
                public DataResult<CheckHandlerUnion<T>> decode(YamlValue yaml) {
                    if (!yaml.isMap())
                        return DataResult.success(new CheckHandlerUnion<T>(CheckHandler.nopTrue(), false));
                    return yaml.decode(YamlCodec.YAML_MAP).flatMap(map -> {
                        if (map.has("if-one")) {
                            return oneOfCodec.decode(map.get("if-one"))
                                    .mapValue(v -> new CheckHandlerUnion<>(v, true));
                        } else if (map.has("if-all")) {
                            return allOfCodec.decode(map.get("if-all"))
                                    .mapValue(v -> new CheckHandlerUnion<>(v, false));
                        } else if (map.has("if")) {
                            return allOfCodec.decode(map.get("if"))
                                    .mapValue(v -> new CheckHandlerUnion<>(v, false));
                        }
                        return DataResult.success(new CheckHandlerUnion<T>(CheckHandler.nopTrue(), false));
                    });
                }

                @Override
                public YamlValue encode(CheckHandlerUnion<T> handler) {
                    //unsupported по сути
                    return handler.check().encode();
                }
            };
        }
    }
}
