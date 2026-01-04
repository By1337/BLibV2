package dev.by1337.core.util.inventory;

import dev.by1337.core.util.NMSBridgeTest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.Objects;

public interface ItemStackSerializer {
    byte[] serialize(ItemStack itemStack);

    ItemStack deserialize(byte[] bytes);

    @ApiStatus.Internal
    class TestImpl implements NMSBridgeTest<ItemStackSerializer> {

        @Override
        public void run(Player player, ItemStackSerializer bridge) {
            ItemStack item = new ItemStack(Material.PAPER);
            item.editMeta(m -> {
                m.displayName(Component.text("test"));
                m.lore(List.of(
                        Component.text("test"),
                        Component.text("test").style(Style.style(NamedTextColor.DARK_AQUA, TextDecoration.BOLD)),
                        Component.text("test")
                ));
            });
            byte[] bytes = bridge.serialize(item);
            ItemStack decoded = bridge.deserialize(bytes);
            if (!Objects.equals(item, decoded)) {
                throw new IllegalStateException("ItemStacks are not equal");
            }
        }
    }
}
