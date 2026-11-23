package dev.by1337.core.bridge;

import org.bukkit.entity.Player;

public interface NMSBridgeTest<T> {
    void run(Player player, T bridge);
}
