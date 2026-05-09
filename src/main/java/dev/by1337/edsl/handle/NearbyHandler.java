package dev.by1337.edsl.handle;

import dev.by1337.edsl.EventHandler;
import dev.by1337.edsl.context.EventContext;
import dev.by1337.yaml.YamlValue;
import dev.by1337.yaml.codec.DataResult;
import dev.by1337.yaml.codec.YamlCodec;
import dev.by1337.yaml.decoder.YamlDecoder;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class NearbyHandler<C extends EventContext<C>> implements EventHandler<C> {
    private static final Logger log = LoggerFactory.getLogger(NearbyHandler.class);
    private final EventHandler<C> handler;
    private double radius = 10;

    public NearbyHandler(EventHandler<C> handler) {
        this.handler = handler;
    }

    public NearbyHandler(EventHandler<C> handler, double radius) {
        this.handler = handler;
        this.radius = radius;
    }

    @Override
    public boolean handle(C ctx) {
        boolean state = true;
        try (var enter = ctx.tracer().enter("nearby [", "] -> %s")) {
            enter.result(true);
            var location = ctx.location();
            if (location == null) {
                log.error("nearby by null location!");
                ctx.tracer().log("nearby by null location!");
                return false;
            }
            var players = location.getNearbyPlayers(radius).iterator();
            while (players.hasNext()) {
                Player target = players.next();
                try (var ignored = ctx.target(target)) {
                    if (!handler.handle(ctx)) {
                        enter.result(false);
                        state = false;
                    }
                }
                if (players.hasNext()) {
                    ctx.tracer().log("");
                }
            }
        }
        return state;
    }

    public double radius() {
        return radius;
    }

    public NearbyHandler<C> setRadius(double radius) {
        this.radius = radius;
        return this;
    }

    public static <C extends EventContext<C>> YamlDecoder<NearbyHandler<C>> decoder(YamlDecoder<EventHandler<C>> codec) {
        return codec.flatMap((ctx, v) -> {
            var key = ctx.getFromKey();
            if (key == null) return DataResult.error("Has no $FromKey");
            var arr = key.split("-", 2);
            if (arr.length != 2) return DataResult.error("Bad radius " + key);
            return YamlDecoder.DOUBLE.decode(ctx, YamlValue.wrap(arr[1])).map(r -> new NearbyHandler<>(v, r));
        });
    }
}
