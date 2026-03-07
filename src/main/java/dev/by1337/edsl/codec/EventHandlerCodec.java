package dev.by1337.edsl.codec;

import dev.by1337.edsl.EventHandler;
import dev.by1337.edsl.context.EventContext;
import dev.by1337.edsl.handle.*;
import dev.by1337.yaml.YamlValue;
import dev.by1337.yaml.codec.DataResult;
import dev.by1337.yaml.codec.YamlCodec;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

public class EventHandlerCodec<C extends EventContext<C>> implements YamlCodec<EventHandler<C>> {
    private final YamlCodec<Map<String, String>> STRING_2_STRING = YamlCodec.mapOf(YamlCodec.STRING, YamlCodec.MULTI_LINE_STRING);
    private final YamlCodec<Map<String, YamlValue>> S2OBJET_MAP = YamlCodec.mapOf(YamlCodec.STRING, YamlCodec.YAML_VALUE);
    private final YamlCodec<AllPlayersHandler<C>> allPlayersCodec = AllPlayersHandler.codec(this);
    private final YamlCodec<NearbyHandler<C>> nearbyCodec = NearbyHandler.codec(this);

    private final List<HandlerCodecHolder<C>> codecHolders = new ArrayList<>();

    public EventHandlerCodec() {
        registerHandler(OneOfHandler::codec, "oneOf");
        registerHandler(new CodecFactory<C>() {
            @Override
            public YamlCodec<? extends EventHandler<C>> create(YamlCodec<EventHandler<C>> base) {
                return dev.by1337.edsl.handle.ConditionalHandler.codec(base);
            }
        }, "if", "if-all", "if-one", "do", "else");
    }

    @Override
    public DataResult<EventHandler<C>> decode(YamlValue yaml) {
        if (yaml.isPrimitive()) return STRING.decode(yaml)
                .flatMap(this::decodePrimitiveCommand);
        if (yaml.isList()) return yaml.asList(this)
                .mapValue(ListHandler::new);
        return S2OBJET_MAP.decode(yaml).flatMap(map -> {
            List<EventHandler<C>> handlers = new ArrayList<>();
            StringBuilder err = new StringBuilder();
            for (String s : map.keySet()) {
                var codec = getCodec(s);
                if (codec != null) {
                    var r = codec.codec(this).decode(yaml);
                    if (r.hasError()) err.append(r.error()).append("\n");
                    if (r.hasResult()) handlers.add(r.result());
                    break;
                }
            }
            for (String cmd : map.keySet()) {
                if (isCodecKey(cmd)) continue;
                var value = map.get(cmd);
                var r = decodeCommand(cmd, value);
                if (r.hasError()) err.append(r.error()).append("\n");
                if (r.hasResult()) handlers.add(r.result());
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


    public DataResult<? extends EventHandler<C>> decodeCommand(String cmd, YamlValue value) {
        if (cmd.equals("all_players")) {
            return allPlayersCodec.decode(value);
        } else if (cmd.startsWith("nearby-")) {
            return nearbyCodec.decode(value)
                    .and(DOUBLE.decode(cmd.substring("nearby-".length())))
                    .map(NearbyHandler::setRadius);
        }
        return MULTI_LINE_STRING.decode(value).mapValue(s -> {
            if (s.isBlank()) return "[" + cmd + "]";
            return "[" + cmd + "] " + s.replace("\n", "<br><reset>");
        }).flatMap(this::decodePrimitiveCommand);
    }

    public DataResult<? extends EventHandler<C>> decodePrimitiveCommand(String input) {
        if (!input.startsWith("[")) return decodePrimitiveCommand("[message] " + input.replace("\n", "<br><reset>"));
        //if (input.startsWith("[message_all]")){
            //    String source = input.substring("[message_all]".length());
            //    return decodeCommand("all_players", YamlValue.wrap("[message] " + source));
            //}
        return DataResult.success(new CommandHandler<>(input));
    }

    private boolean isCodecKey(String key) {
        return getCodec(key) != null;
    }

    private @Nullable HandlerCodecHolder<C> getCodec(String key) {
        for (HandlerCodecHolder<C> handler : codecHolders) {
            if (handler.keyTester.test(key)) return handler;
        }
        return null;
    }

    public void registerHandler(CodecFactory<C> factory, String key) {
        registerHandler(factory, s -> s.equals(key));
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