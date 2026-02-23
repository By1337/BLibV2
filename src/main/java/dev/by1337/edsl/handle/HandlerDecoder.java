package dev.by1337.edsl.handle;

import dev.by1337.edsl.EventHandler;
import dev.by1337.edsl.HandlerCodecBuilder;
import dev.by1337.yaml.YamlValue;
import dev.by1337.yaml.codec.DataResult;
import dev.by1337.yaml.codec.YamlCodec;
import dev.by1337.yaml.codec.schema.SchemaType;
import dev.by1337.yaml.codec.schema.SchemaTypes;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class HandlerDecoder<T> {
    private final YamlCodec<Map<String, YamlValue>> S2OBJET_MAP = YamlCodec.mapOf(YamlCodec.STRING, YamlCodec.YAML_VALUE);

    protected final Map<String, Decoder<T>> decoders = new LinkedHashMap<>();
    protected final HandlerCodecBuilder<T> builder;

    public HandlerDecoder(HandlerCodecBuilder<T> builder) {
        this.builder = builder;
        decoders.put("oneOf", Decoder.fromCodec(OneOfHandler.createCodec(builder)));
        decoders.put("if", Decoder.fromCodec(ConditionalHandler.createCodec(builder)));
        decoders.put("if-all", Decoder.fromCodec(ConditionalHandler.createCodec(builder)));
        decoders.put("if-one", Decoder.fromCodec(ConditionalHandler.createCodec(builder)));
        decoders.put("do", Decoder.fromCodec(ConditionalHandler.createCodec(builder)));
        decoders.put("else", Decoder.fromCodec(ConditionalHandler.createCodec(builder)));
    }

    public DataResult<EventHandler<T>> decode(YamlValue yaml) {
        if (yaml.isPrimitive())
          return YamlCodec.STRING.decode(yaml).mapValue(s -> new CommandHandler<>(fixCommand(s)));

        return S2OBJET_MAP.decode(yaml).flatMap(map -> {
            Decoder<T> decoder = null;
            for (String s : map.keySet()) {
                if ((decoder = decoders.get(s)) != null) break;
            }
            List<EventHandler<T>> handlers = new ArrayList<>();
            StringBuilder err = new StringBuilder();
            if (decoder != null){
                tryDecode(yaml, decoder, err, handlers::add);
            }
            StringBuilder buffer = new StringBuilder();
            for (String cmd : map.keySet()) {
                if (decoders.containsKey(cmd)) continue;
                buffer.append("[").append(cmd).append("]");
                var value = map.get(cmd).decode(YamlCodec.MULTI_LINE_STRING).getOrThrow();
                if (!value.isBlank()) {
                    buffer.append(" ").append(value.replace("\n", "<br><reset>"));
                }
                var s = buffer.toString();
                handlers.add(new CommandHandler<>(fixCommand(s)));
                buffer.setLength(0);
            }
            if (handlers.isEmpty()) return DataResult.success(EventHandler.nop());
            EventHandler<T> result = handlers.size() > 1 ? new ListHandler<>(handlers) : handlers.get(0);
            if (err.isEmpty()) return DataResult.success(result);
            err.setLength(err.length()-1);
            return DataResult.error(err.toString()).partial(result);
        });
    }
    private static <T> void tryDecode(YamlValue v, Decoder<T> codec, StringBuilder err, Consumer<EventHandler<T>> c) {
        var res = codec.decode(v);
        if (res.hasError()) {
            err.append(res.error()).append("\n");
        }
        if (res.hasResult()) {
            c.accept(res.result());
        }
    }

    protected String fixCommand(String input) {
        if (input.startsWith("[")) return input;
        return "[message] " + input.replace("\n", "<br><reset>");
    }

    public YamlCodec<EventHandler<T>> createCodec() {
        return new YamlCodec<>() {
            //final YamlCodec<ListHandler<T>> fromList = ListHandler.createCodec(b, this);

            @Override
            public DataResult<EventHandler<T>> decode(YamlValue yaml) {
                if (yaml.isList()) return yaml.asList(this).mapValue(ListHandler::new);
                return HandlerDecoder.this.decode(yaml);
            }

            @Override
            public YamlValue encode(EventHandler<T> handler) {
                return handler.doEncode();
            }

            @Override
            public @NotNull SchemaType schema() {
                return SchemaTypes.ANY;
            }
        };
    }

    public interface Decoder<T> {
        DataResult<EventHandler<T>> decode(YamlValue in);

        static <T> Decoder<T> fromCodec(YamlCodec<? extends EventHandler<T>> codec){
            return v -> codec.decode(v).mapValue(i -> i);
        }
    }
}
