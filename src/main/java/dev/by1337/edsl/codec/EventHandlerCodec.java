package dev.by1337.edsl.codec;

import dev.by1337.edsl.EventHandler;
import dev.by1337.edsl.context.EventContext;
import dev.by1337.edsl.handle.*;
import dev.by1337.yaml.YamlValue;
import dev.by1337.yaml.codec.DataResult;
import dev.by1337.yaml.decoder.DataResultBuilder;
import dev.by1337.yaml.decoder.DecodeContext;
import dev.by1337.yaml.decoder.YamlDecoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EventHandlerCodec<C extends EventContext<C>> implements YamlDecoder<EventHandler<C>> {

    private final YamlDecoder<Map<String, YamlValue>> S2OBJET_MAP = YamlDecoder.mapOf(YamlDecoder.STRING, YamlDecoder.YAML_VALUE);
    private final YamlDecoder<AllPlayersHandler<C>> allPlayersCodec = AllPlayersHandler.decoder(this);
    private final YamlDecoder<NearbyHandler<C>> nearbyCodec = NearbyHandler.decoder(this);
    private final YamlDecoder<WeightsHandler<C>> weightsCodec = WeightsHandler.decoder(this);
    private final YamlDecoder<OneOfHandler<C>> oneOfCodec = OneOfHandler.decoder(this);

    private final YamlDecoder<ConditionalHandler<C>> ifDecoder = ConditionalHandler.decoder(this);
    private final YamlDecoder<List<EventHandler<C>>> selfList = this.listOf();


    public DataResult<? extends EventHandler<C>> decodeCommand(DecodeContext ctx, String cmd, YamlValue value) {
        try (var ignored = ctx.fromKey(cmd)) {
            if (cmd.equals("all_players")) {
                return allPlayersCodec.decode(value);
            } else if (cmd.startsWith("nearby-")) {
                return nearbyCodec.decode(value);
            } else if (cmd.equals("weights")) {
                return weightsCodec.decode(value);
            } else if (cmd.equals("oneOf")) {
                return oneOfCodec.decode(value);
            }
            return MULTI_LINE_STRING.decode(value).mapValue(s -> {
                if (s.isBlank()) return "[" + cmd + "]";
                return "[" + cmd + "] " + s.replace("\n", "<br><reset>");
            }).flatMap(this::decodePrimitiveCommand);
        }
    }

    @Override
    public DataResult<EventHandler<C>> decode(DecodeContext ctx, YamlValue yaml) {
        if (yaml.isPrimitive()) return STRING.decode(yaml)
                .flatMap(this::decodePrimitiveCommand);
        if (yaml.isList()) return selfList.decode(ctx, yaml).mapValue(ListHandler::new);
        return S2OBJET_MAP.decode(yaml).flatMap(map -> {
            List<EventHandler<C>> handlers = new ArrayList<>();
            var result = new DataResultBuilder();
            if (
                    map.containsKey("if") ||
                            map.containsKey("if-all") ||
                            map.containsKey("if-one") ||
                            map.containsKey("do") ||
                            map.containsKey("else")
            ) {
                ifDecoder.decode(ctx, yaml).ifPresent(handlers::add, result::append);
            }
            for (String cmd : map.keySet()) {
                if (cmd.equals("if") || cmd.equals("if-all") || cmd.equals("if-one") || cmd.equals("do") || cmd.equals("else"))
                    continue;
                var value = map.get(cmd);
                decodeCommand(ctx, cmd, value).ifPresent(handlers::add, result::append);
            }
            return result.result(new ListHandler<>(handlers));
        });
    }

    public DataResult<? extends EventHandler<C>> decodePrimitiveCommand(String input) {
        if (!input.startsWith("[")) return decodePrimitiveCommand("[message] " + input.replace("\n", "<br><reset>"));
        return DataResult.success(new CommandHandler<>(input));
    }
}