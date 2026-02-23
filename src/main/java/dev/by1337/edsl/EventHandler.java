package dev.by1337.edsl;

import dev.by1337.cmd.Command;
import dev.by1337.edsl.handle.CommandHandler;
import dev.by1337.edsl.handle.ConditionalHandler;
import dev.by1337.edsl.handle.ListHandler;
import dev.by1337.edsl.handle.OneOfHandler;
import dev.by1337.plc.PlaceholderApplier;
import dev.by1337.yaml.YamlValue;
import dev.by1337.yaml.codec.DataResult;
import dev.by1337.yaml.codec.YamlCodec;
import dev.by1337.yaml.codec.schema.SchemaType;
import dev.by1337.yaml.codec.schema.SchemaTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface EventHandler<C> {
    static <T> YamlCodec<EventHandler<T>> codec() {
        return new YamlCodec<>() {
            private final YamlCodec<Map<String, YamlValue>> S2OBJET_MAP = YamlCodec.mapOf(YamlCodec.STRING, YamlCodec.YAML_VALUE);
            private final YamlCodec<OneOfHandler<T>> oneOf = OneOfHandler.codec(this);
            private final YamlCodec<ConditionalHandler<T>> conditional = ConditionalHandler.codec(this);

            @Override
            public DataResult<EventHandler<T>> decode(YamlValue yaml) {
                if (yaml.isPrimitive()) return STRING.decode(yaml)
                        .mapValue(CommandHandler::new);
                if (yaml.isList()) return yaml.asList(this)
                        .mapValue(ListHandler::new);
                return S2OBJET_MAP.decode(yaml).flatMap(map -> {
                    List<EventHandler<T>> handlers = new ArrayList<>();
                    StringBuilder err = new StringBuilder();
                    if (map.containsKey("oneOf")) {
                        var r = oneOf.decode(yaml);
                        if (r.hasError()) err.append(r.error()).append("\n");
                        if (r.hasResult()) handlers.add(r.result());
                    } else if (map.containsKey("if")
                            || map.containsKey("if-all")
                            || map.containsKey("if-one")
                            || map.containsKey("do")
                            || map.containsKey("else")
                    ) {
                        var r = conditional.decode(yaml);
                        if (r.hasError()) err.append(r.error()).append("\n");
                        if (r.hasResult()) handlers.add(r.result());
                    }
                    StringBuilder buffer = new StringBuilder();
                    for (String cmd : map.keySet()) {
                        if (
                                cmd.equals("oneOf") ||
                                        cmd.equals("if") ||
                                        cmd.equals("if-all") ||
                                        cmd.equals("if-one") ||
                                        cmd.equals("do") ||
                                        cmd.equals("else")
                        ) continue;
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
                    err.setLength(err.length() - 1);
                    return DataResult.error(err.toString()).partial(result);
                });
            }

            @Override
            public YamlValue encode(EventHandler<T> handler) {
                return handler.doEncode();
            }

            @Override
            public @NotNull SchemaType schema() {
                return SchemaTypes.ANY;
            }

            private String fixCommand(String input) {
                if (input.startsWith("[")) return input;
                return "[message] " + input.replace("\n", "<br><reset>");
            }
        };
    }

    boolean handle(C ctx, PlaceholderApplier placeholders, Command<C> executor);

    @Nullable YamlValue encode();

    default @NotNull YamlValue doEncode() {
        YamlValue res = encode();
        return res != null ? res : YamlValue.wrap("unsupported " + this.getClass().getSimpleName());
    }

    static <T> EventHandler<T> nop() {
        return new EventHandler<T>() {
            @Override
            public boolean handle(T ctx, PlaceholderApplier placeholders, Command<T> executor) {
                return true;
            }

            @Override
            public @Nullable YamlValue encode() {
                return null;
            }
        };
    }
}
