package dev.by1337.core.util.network;

import io.netty.channel.Channel;
import org.bukkit.entity.Player;

@FunctionalInterface
public interface ChannelGetter {
    Channel getChannel(Player pl);
}