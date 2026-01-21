package dev.by1337.core.util.network;

import io.netty.channel.Channel;
import org.bukkit.entity.Player;

@FunctionalInterface
public interface ChannelGetter {
    ChannelGetter INSTANCE = ChannelGetterCreator.create();

    Channel getChannel(Player pl);

    static Channel get(Player pl) {
        return INSTANCE.getChannel(pl);
    }
}