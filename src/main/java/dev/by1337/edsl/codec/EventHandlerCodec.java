package dev.by1337.edsl.codec;

import dev.by1337.edsl.EventContext;
import dev.by1337.edsl.EventHandler;
import dev.by1337.edsl.handle.CommandHandler;
import dev.by1337.edsl.handle.ConditionalHandler;
import dev.by1337.edsl.handle.ListHandler;
import dev.by1337.edsl.handle.OneOfHandler;
import dev.by1337.yaml.YamlValue;
import dev.by1337.yaml.codec.DataResult;
import dev.by1337.yaml.codec.YamlCodec;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

public class EventHandlerCodec<C extends EventContext<C>> {
    private final YamlCodec<Map<String, String>> STRING_2_STRING = YamlCodec.mapOf(YamlCodec.STRING, YamlCodec.MULTI_LINE_STRING);
    private final YamlCodec<Map<String, YamlValue>> S2OBJET_MAP = YamlCodec.mapOf(YamlCodec.STRING, YamlCodec.YAML_VALUE);

    private final List<HandlerCodecHolder<C>> codecHolders = new ArrayList<>();

    public EventHandlerCodec() {
        registerHandler(OneOfHandler::codec, "oneOf");
        registerHandler(ConditionalHandler::codec, "if", "if-all", "if-one", "do", "else");
    }

    public YamlCodec<EventHandler<C>> make() {
        return new YamlCodec<>() {
            @Override
            public DataResult<EventHandler<C>> decode(YamlValue yaml) {
                if (yaml.isPrimitive()) return STRING.decode(yaml)
                        .mapValue(s -> new CommandHandler<>(fixCommand(s)));
                if (yaml.isList()) return yaml.asList(this)
                        .mapValue(ListHandler::new);
                return S2OBJET_MAP.decode(yaml).flatMap(map -> {
                    List<EventHandler<C>> handlers = new ArrayList<>();
                    StringBuilder err = new StringBuilder();
                    for (String s : map.keySet()) {
                        var codec = getCodec(s);
                        if (codec != null){
                            var r = codec.codec(this).decode(yaml);
                            if (r.hasError()) err.append(r.error()).append("\n");
                            if (r.hasResult()) handlers.add(r.result());
                            break;
                        }
                    }
                    StringBuilder buffer = new StringBuilder();
                    for (String cmd : map.keySet()) {
                        if (isCodecKey(cmd)) continue;
                        buffer.append("[").append(cmd).append("]");
                        var value = map.get(cmd);
                        var s = value.decode(MULTI_LINE_STRING, "").result();
                        if (!s.isBlank()) {
                            buffer.append(" ").append(s.replace("\n", "<br><reset>"));
                        }
                        handlers.add(new CommandHandler<>(buffer.toString()));
                        buffer.setLength(0);
                    }
                    if (handlers.isEmpty()) return DataResult.success(EventHandler.nop());
                    EventHandler<C> result = handlers.size() > 1 ? new ListHandler<>(handlers) : handlers.get(0);
                    if (err.isEmpty()) return DataResult.success(result);
                    err.setLength(err.length() - 1);
                    return DataResult.error(err.toString()).partial(result);
                });
            }

            @Override
            public YamlValue encode(EventHandler<C> handler) {
                return handler.doEncode();
            }
        };
    }
    private String fixCommand(String input) {
        if (input.startsWith("[")) return input;
        return "[message] " + input.replace("\n", "<br><reset>");
    }
    private boolean isCodecKey(String key){
        return getCodec(key) != null;
    }
    private @Nullable HandlerCodecHolder<C> getCodec(String key){
        for (HandlerCodecHolder<C> handler : codecHolders) {
            if (handler.keyTester.test(key)) return handler;
        }
        return null;
    }

    public void registerHandler(CodecFactory<C> factory, String... keys) {
        Set<String> set = new HashSet<>(Arrays.asList(keys));
        registerHandler(factory, set::contains);
    }

    public void registerHandler(CodecFactory<C> factory, Predicate<String> keyTester) {
        codecHolders.add(new HandlerCodecHolder<>(factory, keyTester));
    }


    public static class HandlerCodecHolder<C extends EventContext<C>> {
        private final CodecFactory<C> factory;
        private final Predicate<String> keyTester;
        private YamlCodec<? extends EventHandler<C>> codec;

        public HandlerCodecHolder(CodecFactory<C> factory, Predicate<String> keyTester) {
            this.factory = factory;
            this.keyTester = keyTester;
        }

        public YamlCodec<? extends EventHandler<C>> codec(YamlCodec<EventHandler<C>> base) {
            if (codec != null) return codec;
            return codec = factory.create(base);
        }
    }

    public interface CodecFactory<C extends EventContext<C>> {
        YamlCodec<? extends EventHandler<C>> create(YamlCodec<EventHandler<C>> base);
    }
}
// if (value.isPrimitive() || value.isList()) {
//                            var s = value.decode(MULTI_LINE_STRING, "").result();
//                            if (!s.isBlank()) {
//                                buffer.append(" ").append(s.replace("\n", "<br><reset>"));
//                            }
//                        } else {
//                            var m = value.decode(STRING_2_STRING).getOrThrow();
//                            if (!m.isEmpty()) {
//                                m.forEach((k, v) -> buffer.append(" --\"")
//                                        .append(escape(k))
//                                        .append("\" \"")
//                                        .append(escape(v.replace("\n", "<br><reset>")))
//                                        .append("\""));
//                            }
//                        }