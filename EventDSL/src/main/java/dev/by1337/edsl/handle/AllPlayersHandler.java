package dev.by1337.edsl.handle;

import dev.by1337.edsl.EventHandler;
import dev.by1337.edsl.context.EventContext;
import dev.by1337.yaml.YamlValue;
import dev.by1337.yaml.codec.YamlCodec;
import dev.by1337.yaml.decoder.YamlDecoder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class AllPlayersHandler implements EventHandler {
    private final EventHandler handler;

    public AllPlayersHandler(EventHandler handler) {
        this.handler = handler;
    }

    @Override
    public boolean handle(EventContext ctx) {
        boolean state = true;
        try (var enter = ctx.tracer().enter("all_players [", "] -> %s")) {
            enter.result(true);
            var players = Bukkit.getOnlinePlayers().iterator();
            while (players.hasNext()) {
                Player player = players.next();
                try (var ignored = ctx.target(player)) {
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

    public static YamlDecoder<AllPlayersHandler> decoder(YamlDecoder<EventHandler> codec) {
        return codec.map(AllPlayersHandler::new);
    }
}
