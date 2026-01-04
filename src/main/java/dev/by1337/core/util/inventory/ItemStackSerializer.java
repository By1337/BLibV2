package dev.by1337.core.util.inventory;

import org.bukkit.inventory.ItemStack;

public interface ItemStackSerializer {
    byte[] serialize(ItemStack itemStack);
    ItemStack deserialize(byte[] bytes);
}
