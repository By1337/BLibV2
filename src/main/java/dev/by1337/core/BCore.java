package dev.by1337.core;

import dev.by1337.core.bridge.command.BukkitCommandRegister;
import dev.by1337.core.bridge.inventory.InventoryUtil;
import dev.by1337.core.bridge.inventory.ItemStackSerializer;
import dev.by1337.core.bridge.nbt.NbtBridge;
import dev.by1337.core.bridge.world.BlockEntityUtil;

public class BCore {
    static BlockEntityUtil blockEntityUtil;
    static ItemStackSerializer itemStackSerializer;
    static InventoryUtil inventoryUtil;
    static BukkitCommandRegister bukkitCommandRegister;
    static NbtBridge nbtBridge;

    public static BlockEntityUtil getBlockEntityUtil() {
        return blockEntityUtil;
    }

    public static ItemStackSerializer getItemStackSerializer() {
        return itemStackSerializer;
    }

    public static InventoryUtil getInventoryUtil() {
        return inventoryUtil;
    }

    public static BukkitCommandRegister getBukkitCommandRegister() {
        return bukkitCommandRegister;
    }

    public static NbtBridge getNbtBridge() {
        return nbtBridge;
    }
}
