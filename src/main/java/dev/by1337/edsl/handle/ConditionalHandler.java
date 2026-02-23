package dev.by1337.edsl.handle;

import dev.by1337.cmd.Command;
import dev.by1337.edsl.EventHandler;
import dev.by1337.edsl.HandlerCodecBuilder;
import dev.by1337.edsl.check.CheckHandler;
import dev.by1337.plc.PlaceholderApplier;
import dev.by1337.yaml.YamlValue;
import dev.by1337.yaml.codec.DataResult;
import dev.by1337.yaml.codec.RecordYamlCodecBuilder;
import dev.by1337.yaml.codec.YamlCodec;
import dev.by1337.yaml.codec.schema.SchemaType;
import dev.by1337.yaml.codec.schema.SchemaTypes;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class ConditionalHandler<C> implements EventHandler<C> {
    private final CheckHandlerUnion<C> tester;
    private final EventHandler<C> success;
    private final EventHandler<C> failure;

    public ConditionalHandler(CheckHandlerUnion<C> tester, EventHandler<C> success, EventHandler<C> failure) {
        this.tester = tester;
        this.success = success;
        this.failure = failure;
    }

    @Override
    public boolean handle(C ctx, PlaceholderApplier placeholders, Command<C> executor) {
        if (tester.check.handle(ctx, placeholders, executor)) {
            if (success !=null)
                success.handle(ctx, placeholders, executor);
            return true;
        }
        if (failure != null)
            failure.handle(ctx, placeholders, executor);
        return false;
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

    public static <T> YamlCodec<ConditionalHandler<T>> createCodec(HandlerCodecBuilder<T> b) {
        return YamlCodec.lazyLoad(() -> RecordYamlCodecBuilder.mapOf(
                ConditionalHandler::new,
                CheckHandlerUnion.createCodec(b).fieldOf(null, v -> v.tester),
                b.handlerCodec().fieldOf("do", v -> v.success),
                b.handlerCodec().fieldOf("else", v -> v.success)
        ));
    }

    public record CheckHandlerUnion<T>(CheckHandler<T> check, boolean oneOf) {

        private static <T> YamlCodec<CheckHandlerUnion<T>> createCodec(HandlerCodecBuilder<T> b) {
            return new YamlCodec<>() {

                @Override
                public DataResult<CheckHandlerUnion<T>> decode(YamlValue yaml) {
                    if (!yaml.isMap()) return DataResult.success(new CheckHandlerUnion<>(CheckHandler.nopTrue(), false));
                    return yaml.decode(YamlCodec.YAML_MAP).flatMap(map -> {
                        if (map.has("if-one")) {
                            return b.checkCodecOneOf().decode(map.get("if-one"))
                                    .mapValue(v -> new CheckHandlerUnion<>(v, true));
                        } else if (map.has("if-all")) {
                            return b.checkCodec().decode(map.get("if-all"))
                                    .mapValue(v -> new CheckHandlerUnion<>(v, false));
                        } else if (map.has("if")) {
                            return b.checkCodec().decode(map.get("if"))
                                    .mapValue(v -> new CheckHandlerUnion<>(v, false));
                        }
                        return DataResult.success(new CheckHandlerUnion<>(CheckHandler.nopTrue(), false));
                    });
                }

                @Override
                public YamlValue encode(CheckHandlerUnion<T> handler) {
                    //unsupported по сути
                    return handler.check().encode();
                }

                @Override
                public @NotNull SchemaType schema() {
                    //unsupported
                    return SchemaTypes.ANY;
                }
            };
        }
    }
}
