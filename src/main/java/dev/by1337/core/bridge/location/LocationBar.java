package dev.by1337.core.bridge.location;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Sends personal locator bar waypoints through the version-specific NMS bridge.
 * Implementations require a Minecraft version supporting locator bar packets.
 * Calls must run on the server thread responsible for the viewer.
 *
 * <p>Waypoints have no world information in the packet. The caller is responsible
 * for removing or resending them on world changes and reconnects.</p>
 */
public interface LocationBar {

    /** Vanilla dots whose appearance changes with distance; no resource pack required. */
    NamespacedKey DEFAULT_STYLE = NamespacedKey.minecraft("default");

    /** Vanilla bowtie icon; no resource pack required. */
    NamespacedKey BOWTIE_STYLE = NamespacedKey.minecraft("bowtie");

    /**
     * Adds a waypoint or replaces the waypoint with the same id, including its
     * color and style. No entity or resource pack is required for the default style.
     */
    void send(Player viewer, Waypoint waypoint);

    /**
     * Updates only the position of a waypoint previously sent to this viewer.
     * Use {@link #send(Player, Waypoint)} to change its color or style.
     * Never send a position update before adding the waypoint to the client.
     */
    void update(Player viewer, Waypoint waypoint);

    /** Removes a waypoint. Removing an absent waypoint is safe. */
    void remove(Player viewer, UUID uuid);

    default void remove(Player viewer, Waypoint waypoint) {
        remove(viewer, Objects.requireNonNull(waypoint, "waypoint").getUuid());
    }

    /**
     * Mutable waypoint description with fluent setters; setters do not send packets.
     * The bridge must encode the id as {@code new UUID(id, 0L)} for all operations.
     * Ids must be unique among waypoints sent to the same viewer, including those
     * supplied by other users of this bridge.
     */
    final class Waypoint {
        private static final AtomicLong counter = new AtomicLong();
        private int rgb = 0xFFFFFF;
        private @Nullable NamespacedKey style;
        private double x;
        private double y;
        private double z;
        private final UUID uuid;

        public Waypoint(UUID uuid) {
            this.uuid = uuid;
        }

        public Waypoint() {
            uuid = new UUID(counter.getAndIncrement(), 1337);
        }

        /** Coordinates are rounded down to block coordinates by the bridge. */
        public Waypoint setPos(double x, double y, double z) {
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
                throw new IllegalArgumentException("Waypoint coordinates must be finite");
            }
            this.x = x;
            this.y = y;
            this.z = z;
            return this;
        }

        /** Copies coordinates only; the location's world is not retained. */
        public Waypoint setPos(Location location) {
            Objects.requireNonNull(location, "location");
            return setPos(location.getX(), location.getY(), location.getZ());
        }

        public Waypoint setColor(Color color) {
            return setColor(color.asRGB());
        }

        /** Sets an opaque RGB color in the range 0x000000..0xFFFFFF. */
        public Waypoint setColor(int rgb) {
            if ((rgb & 0xFF000000) != 0) {
                throw new IllegalArgumentException("Waypoint color must be a 24-bit RGB value");
            }
            this.rgb = rgb;
            return this;
        }

        /** Accepts exactly six hexadecimal digits, optionally prefixed with '#'. */
        public Waypoint setColor(String hex) {
            Objects.requireNonNull(hex, "hex");
            String value = hex.startsWith("#") ? hex.substring(1) : hex;
            if (!value.matches("[0-9a-fA-F]{6}")) {
                throw new IllegalArgumentException("Waypoint color must be RRGGBB or #RRGGBB");
            }
            return setColor(Integer.parseInt(value, 16));
        }

        /**
         * Selects a client waypoint style, not an item texture. Null uses
         * {@code minecraft:default}. Vanilla 1.21.11 also supplies
         * {@code minecraft:bowtie}; custom styles require client resources.
         * Unknown styles do not automatically fall back to the default.
         */
        public Waypoint setStyle(@Nullable NamespacedKey style) {
            this.style = style;
            return this;
        }

        public UUID getUuid() {
            return uuid;
        }

        public int getRgb() {
            return rgb;
        }

        public @Nullable NamespacedKey getStyle() {
            return style;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getZ() {
            return z;
        }
    }
}
