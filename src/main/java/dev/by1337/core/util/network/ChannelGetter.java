package dev.by1337.core.util.network;

import io.netty.channel.Channel;
import org.bukkit.entity.Player;

@FunctionalInterface
public interface ChannelGetter {
    Channel getChannel(Player pl);

    static Channel get(Player pl) {
        return Impl.getChannel(pl);
    }

    class Impl {
        private static ChannelGetter channelGetter;

        public static Channel getChannel(Player pl) {
            if (channelGetter == null)
                channelGetter = ChannelGetterCreator.create(pl);
            return channelGetter.getChannel(pl);

        }
    }
}