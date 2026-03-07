package dev.by1337.edsl.handle;

import dev.by1337.edsl.EventHandler;
import dev.by1337.edsl.context.EventContext;
import dev.by1337.yaml.YamlValue;
import dev.by1337.yaml.codec.YamlCodec;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class AllPlayersHandler<C extends EventContext<C>> implements EventHandler<C> {
    private final EventHandler<C> handler;

    public AllPlayersHandler(EventHandler<C> handler) {
        this.handler = handler;
    }

    @Override
    public boolean handle(C ctx) {
        boolean state = true;
        try (var enter = ctx.tracer().enter("all_players [", "] -> %s")) {
            enter.result(true);
            var players = Bukkit.getOnlinePlayers().iterator();
            while (players.hasNext()) {
                Player player = players.next();
                try (var ignored = ctx.target(player)){
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

    @Override
    public @Nullable YamlValue encode() {
        return YamlValue.wrap(Map.of("all_players", handler.doEncode().getRaw()));
    }

    public static <C extends EventContext<C>> YamlCodec<AllPlayersHandler<C>> codec(YamlCodec<EventHandler<C>> codec) {
        return codec.map(AllPlayersHandler::new, v -> v.handler);
    }
}
