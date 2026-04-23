package dev.by1337.core.entity;

import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public interface EntityWrapper {
    void sendSpawnPackets(Player player);

    void sendDirtyData(Player player);

    void sendRemovePacket(Player player);

    boolean hasDirtyData();

    void removeDirtyData();

    Entity asBukkit();

    int getId();

    interface Maker {
        EntityWrapper make(EntityType type, World w, double x, double y, double z);
    }
}