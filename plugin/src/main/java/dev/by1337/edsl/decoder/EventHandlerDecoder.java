package dev.by1337.edsl.decoder;

import dev.by1337.edsl.EventHandler;
import dev.by1337.edsl.check.CheckHandler;
import dev.by1337.edsl.command.TextCommands;
import dev.by1337.edsl.command.args.ArgumentParamsMap;
import dev.by1337.edsl.handle.*;
import dev.by1337.yaml.YamlValue;
import dev.by1337.yaml.codec.DataResult;
import dev.by1337.yaml.decoder.DataResultBuilder;
import dev.by1337.yaml.decoder.DecodeContext;
import dev.by1337.yaml.decoder.YamlDecoder;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class EventHandlerDecoder implements YamlDecoder<EventHandler> {

    private final YamlDecoder<Map<String, YamlValue>> S2OBJET_MAP = YamlDecoder.mapOf(YamlDecoder.STRING, YamlDecoder.YAML_VALUE);
    private final YamlDecoder<AllPlayersHandler> allPlayersCodec = AllPlayersHandler.decoder(this);
    private final YamlDecoder<NearbyHandler> nearbyCodec = NearbyHandler.decoder(this);
    private final YamlDecoder<WeightsHandler> weightsCodec = WeightsHandler.decoder(this);
    private final YamlDecoder<OneOfHandler> oneOfCodec = OneOfHandler.decoder(this);

    private final YamlDecoder<ConditionalHandler> ifDecoder;
    private final YamlDecoder<List<EventHandler>> selfList = this.listOf();
    private @Nullable Function<String, String> commandRewriter;

    public EventHandlerDecoder() {
        this(CheckHandler.decoder());
    }

    public EventHandlerDecoder(YamlDecoder<CheckHandler> checks) {
        ifDecoder = ConditionalHandler.decoder(this, checks);
    }


    public DataResult<? extends EventHandler> decodeCommand(DecodeContext ctx, String cmd, YamlValue value) {
        try (var ignored = ctx.fromKey(cmd)) {
            if (cmd.equals("all_players")) {
                return allPlayersCodec.decode(ctx, value);
            } else if (cmd.startsWith("nearby-")) {
                return nearbyCodec.decode(ctx, value);
            } else if (cmd.equals("weights")) {
                return weightsCodec.decode(ctx, value);
            } else if (cmd.equals("oneOf")) {
                return oneOfCodec.decode(ctx, value);
            }
            if (value.isMap() && TextCommands.canonical(cmd) != null) {
                return TextCommands.decodeLocalized(ctx, TextCommands.canonical(cmd), value);
            }
            if (value.isMap()) {
                return YamlDecoder.STRING_TO_STRING.decode(ctx, value).flatMap(params -> {
                    StringBuilder buffer = new StringBuilder();
                    buffer.append("[").append(cmd).append("] ");
                    params.forEach((k, v) -> {
                        buffer.append('"').append(ArgumentParamsMap.escape(k)).append("\"=\"")
                                .append(ArgumentParamsMap.escape(v.replace("\\n", "\n"))).append("\",");
                    });
                    if (!params.isEmpty()) {
                        buffer.setLength(buffer.length() - 1);
                    }
                    return decodePrimitiveCommand(buffer.toString());
                });
            }
            return MULTI_LINE_STRING.decode(ctx, value).mapValue(s -> {
                if (s.isBlank()) return "[" + cmd + "]";
                return "[" + cmd + "] " + s.replace("\n", "<br><reset>");
            }).flatMap(this::decodePrimitiveCommand);
        }
    }

    @Override
    public DataResult<EventHandler> decode(DecodeContext ctx, YamlValue yaml) {
        if (yaml.isPrimitive()) return STRING.decode(yaml)
                .flatMap(this::decodePrimitiveCommand);
        if (yaml.isList()) return selfList.decode(ctx, yaml).mapValue(ListHandler::new);
        return S2OBJET_MAP.decode(yaml).flatMap(map -> {
            List<EventHandler> handlers = new ArrayList<>();
            var result = new DataResultBuilder();
            if (map.containsKey("if") ||
                    map.containsKey("if-all") ||
                    map.containsKey("if-one") ||
                    map.containsKey("do") ||
                    map.containsKey("else")) {
                ifDecoder.decode(ctx, yaml).ifPresent(handlers::add, result::append);
            }
            for (String cmd : map.keySet()) {
                if (cmd.equals("if") || cmd.equals("if-all") || cmd.equals("if-one") || cmd.equals("do") || cmd.equals("else"))
                    continue;
                YamlValue value = map.get(cmd);
                decodeCommand(ctx, cmd, value).ifPresent(handlers::add, result::append);
            }
            return result.result(new ListHandler(handlers));
        });
    }

    public DataResult<? extends EventHandler> decodePrimitiveCommand(String input) {
        if (!input.startsWith("[")) return decodePrimitiveCommand("[message] " + input.replace("\n", "<br><reset>"));
        if (commandRewriter != null) {
            var ar = input.split(" ", 2);
            if (ar.length == 2) {
                var cmd = ar[0].substring(1, ar[0].length() - 1);
                return DataResult.success(new CommandHandler("[" + commandRewriter.apply(cmd) + "] " + ar[1]));
            }
        }
        return DataResult.success(new CommandHandler(input));
    }

    public EventHandlerDecoder commandRewriter(Function<String, String> commandRewriter) {
        this.commandRewriter = commandRewriter;
        return this;
    }
}
