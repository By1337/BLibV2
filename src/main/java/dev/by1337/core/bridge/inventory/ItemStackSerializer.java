package dev.by1337.core.bridge.inventory;

import dev.by1337.core.bridge.NMSBridgeTest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public interface ItemStackSerializer {
    byte[] serialize(ItemStack itemStack, @Nullable World world);

    ItemStack deserialize(byte[] bytes, @Nullable World world);

    @Deprecated
    String toSNbt(ItemStack itemStack, @Nullable World world);
    @Deprecated
    ItemStack fromSNbt(String snbt);

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
            World world = player.getWorld();
            byte[] bytes = bridge.serialize(item, world);
            ItemStack decoded = bridge.deserialize(bytes, world);
            if (!Objects.equals(item, decoded)) {
                throw new IllegalStateException("ItemStacks are not equal");
            }

        }
    }
}
