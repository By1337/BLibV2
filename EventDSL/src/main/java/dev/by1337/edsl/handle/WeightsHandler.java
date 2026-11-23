package dev.by1337.edsl.handle;

import dev.by1337.edsl.EventHandler;
import dev.by1337.edsl.context.EventContext;
import dev.by1337.yaml.YamlMap;
import dev.by1337.yaml.YamlValue;
import dev.by1337.yaml.codec.YamlCodec;
import dev.by1337.yaml.decoder.YamlDecoder;
import it.unimi.dsi.fastutil.doubles.Double2ObjectMap;
import it.unimi.dsi.fastutil.doubles.Double2ObjectOpenHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class WeightsHandler implements EventHandler {
    private final Double2ObjectMap<EventHandler> handlers;
    private final double totalWeight;

    public WeightsHandler(Map<Double, EventHandler> handlers) {
        this(new Double2ObjectOpenHashMap<>(handlers));
    }

    public WeightsHandler(Double2ObjectMap<EventHandler> handlers) {
        this.handlers = handlers;
        totalWeight = handlers.keySet().doubleStream().sum();
    }


    @Override
    public boolean handle(EventContext ctx) {
        var tracer = ctx.tracer();
        try (var enter = tracer.enter("weights {", "}")) {
            double randomValue = ThreadLocalRandom.current().nextDouble() * totalWeight;
            double cumulativeWeight = 0;

            for (var item : handlers.double2ObjectEntrySet()) {
                double key = item.getDoubleKey();
                cumulativeWeight += key;
                if (randomValue < cumulativeWeight) {
                    try (var ignored2 = tracer.enter("%.2f {", key, "}")) {
                        boolean state = item.getValue().handle(ctx);
                        enter.result(state);
                        return state;
                    }
                }
            }
        }
        throw new IllegalStateException("No items to select from.");
    }

    public static YamlDecoder<WeightsHandler> decoder(YamlDecoder<EventHandler> codec) {
        return YamlDecoder.mapOf(YamlDecoder.STRING
                .flatMap((ctx, s) -> YamlDecoder.DOUBLE.decode(ctx, YamlValue.wrap(s.replace(",", ".")))),
                codec)
                .map(WeightsHandler::new);
    }
}
