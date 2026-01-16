package dev.by1337.core.bridge.world;

import dev.by1337.core.bridge.NMSBridgeTest;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public interface BlockEntityUtil {

    default void setBlock(Location location, BlockInfo blockInfo, boolean applyPhysics) {
        setBlock(location, blockInfo.blockId, blockInfo.blockEntity, applyPhysics);
    }

    /**
     * Sets a block at the given location using a raw block id and serialized block entity.
     *
     * <p>The {@code entity} parameter represents implementation-specific serialized
     * block entity data (usually NBT). If {@code entity} is {@code null}, the block
     * is placed without any associated block entity.</p>
     *
     * @param location     target world location
     * @param id           internal block id
     * @param entity       serialized block entity data, or {@code null}
     * @param applyPhysics whether to apply block physics during placement
     */
    void setBlock(Location location, int id, byte @Nullable [] entity, boolean applyPhysics);

    /**
     * Captures the current block state at the given location.
     *
     * <p>The returned {@link BlockInfo} contains:
     * <ul>
     *   <li>block id</li>
     *   <li>block data</li>
     *   <li>serialized block entity (if present)</li>
     * </ul>
     *
     * <p>This method is typically used to snapshot a block before modification
     * so it can later be restored via {@link #setBlock(Location, BlockInfo, boolean)}.</p>
     *
     * @param location target world location
     * @return captured block information
     */
    BlockInfo getBlock(Location location);

    /**
     * Attempts to clear removable ("clearable") data associated with the block
     * at the given location.
     *
     * <p>This method is intended to remove mutable runtime state without changing
     * the block type itself. Typical examples include:</p>
     *
     * <ul>
     *   <li>container inventories (chests, barrels, etc.)</li>
     *   <li>stored items or internal buffers</li>
     *   <li>other block-entity data that should not persist</li>
     * </ul>
     *
     * <p>If the block does not support clearing, or no clearable data exists,
     * the method should perform no action.</p>
     *
     * <p>This operation must be safe to call on any block type.</p>
     *
     * @param location target world location
     */
    void tryClear(Location location);


    record BlockInfo(BlockData data, int blockId, byte @Nullable [] blockEntity) {
    }

    @ApiStatus.Internal
    class TestImpl implements NMSBridgeTest<BlockEntityUtil> {
        @Override
        public void run(Player player, BlockEntityUtil bridge) {
            Location location = player.getLocation().clone().add(0, -1, 0);
            Block block = location.getBlock();

            BlockInfo original = bridge.getBlock(location);

            try {
                block.setType(Material.CHEST, false);

                if (!(block.getState(false) instanceof Chest chest)) {
                    throw new IllegalStateException("Chest was not placed");
                }

                ItemStack dirt = new ItemStack(Material.DIRT);
                chest.getInventory().setItem(0, dirt);
                chest.update(true, false);

                if (!Objects.equals(dirt, ((Chest) block.getState(false)).getInventory().getItem(0))) {
                    throw new IllegalStateException("setItem did not set dirt");
                }

                BlockInfo captured = bridge.getBlock(location);
                if (captured == null) {
                    throw new IllegalStateException("getBlock returned null");
                }

                bridge.tryClear(location);

                if (!isAir(((Chest) block.getState(false)).getInventory().getItem(0))) {
                    throw new IllegalStateException("tryClear did not clear inventory");
                }

                bridge.setBlock(location, captured, false);

                if (!Objects.equals(dirt, ((Chest) block.getState(false)).getInventory().getItem(0))) {
                    throw new IllegalStateException("setBlock did not restore inventory");
                }

            } finally {
                bridge.setBlock(location, original, false);
            }
        }

        private boolean isAir(@Nullable ItemStack item) {
            return item == null || item.getType().isAir();
        }
    }
}
