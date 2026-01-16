package dev.by1337.core.bridge.nbt;

import dev.by1337.core.bridge.NMSBridgeTest;
import dev.by1337.core.util.nbt.BinaryNbt;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface NbtBridge {
    BinaryNbt.CompoundTag of(PersistentDataContainer pdc);

    BinaryNbt.CompoundTag of(ItemStack item, @Nullable World world);

    @Deprecated
    ItemStack create(BinaryNbt.CompoundTag tag, @Nullable World world);

    @Deprecated
    Object toNMS(BinaryNbt.NbtTag tag);

    @Deprecated
    BinaryNbt.NbtTag ofNMS(Object tag);


    class TestImpl implements NMSBridgeTest<NbtBridge> {

        @Override
        public void run(Player player, NbtBridge walker) {
            ItemStack item = getItemStack();
            World world = player.getWorld();

            var v = walker.create(walker.of(item, world), world);
            if (!item.equals(v)) {
                throw new IllegalStateException("Failed to decode item from NBTWalker");
            }

        }

        private ItemStack getItemStack() {
            ItemStack item = new ItemStack(Material.PAPER);
            item.editMeta(m -> {
                m.displayName(Component.text("test"));
                m.lore(List.of(
                        Component.text("test"),
                        Component.text("test").style(Style.style(NamedTextColor.DARK_AQUA, TextDecoration.BOLD)),
                        Component.text("test")
                ));
            });
            return item;
        }
    }
}
