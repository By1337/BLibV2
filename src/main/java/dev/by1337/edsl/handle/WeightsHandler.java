package dev.by1337.edsl.handle;

import dev.by1337.edsl.EventHandler;
import dev.by1337.edsl.context.EventContext;
import dev.by1337.yaml.YamlMap;
import dev.by1337.yaml.YamlValue;
import dev.by1337.yaml.codec.YamlCodec;
import it.unimi.dsi.fastutil.doubles.Double2ObjectMap;
import it.unimi.dsi.fastutil.doubles.Double2ObjectOpenHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class WeightsHandler<C extends EventContext<C>> implements EventHandler<C> {
    private final Double2ObjectMap<EventHandler<C>> handlers;
    private final double totalWeight;

    public WeightsHandler(Map<Double,EventHandler<C>> handlers) {
        this(new Double2ObjectOpenHashMap<>(handlers));
    }

    public WeightsHandler(Double2ObjectMap<EventHandler<C>> handlers) {
        this.handlers = handlers;
        totalWeight = handlers.keySet().doubleStream().sum();
    }


    @Override
    public boolean handle(C ctx) {
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

    @Override
    public @Nullable YamlValue encode() {
        Map<String, Object> result = new LinkedHashMap<>();
        handlers.forEach((k, v) -> result.put(Double.toString(k), v.encode().getRaw()));
        return YamlValue.wrap(result);
    }

    public static <C extends EventContext<C>> YamlCodec<WeightsHandler<C>> codec(YamlCodec<EventHandler<C>> codec) {
        return YamlCodec.mapOf(YamlCodec.DOUBLE, codec)
                .map(WeightsHandler::new, v -> v.handlers);
    }
}
