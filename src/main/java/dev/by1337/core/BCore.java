package dev.by1337.core;

import dev.by1337.core.util.command.BukkitCommandRegister;
import dev.by1337.core.util.inventory.InventoryUtil;
import dev.by1337.core.util.inventory.ItemStackSerializer;
import dev.by1337.core.util.world.BlockEntityUtil;

public class BCore {
    static BlockEntityUtil blockEntityUtil;
    static ItemStackSerializer itemStackSerializer;
    static InventoryUtil inventoryUtil;
    static BukkitCommandRegister bukkitCommandRegister;

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
}
