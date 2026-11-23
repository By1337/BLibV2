package dev.by1337.edsl;

import org.bukkit.entity.Player;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;

public class PlayerLocal {

    private static final ThreadLocal<Player> SCOPED = new ThreadLocal<>();

    /** Explicit rendering context for hosts outside BMenu/Leaf. Nested scopes restore their caller. */
    public static Scope withPlayer(Player player) {
        Player previous = SCOPED.get();
        SCOPED.set(java.util.Objects.requireNonNull(player));
        return () -> {
            if (previous == null) SCOPED.remove();
            else SCOPED.set(previous);
        };
    }

    @FunctionalInterface
    public interface Scope extends AutoCloseable {
        @Override
        void close();
    }

    private static ThreadLocal<Player> LOCAL1;
    private static ThreadLocal<Player> LOCAL2;

    public static Player get() {
        Player scoped = SCOPED.get();
        if (scoped != null) return scoped;
        if (LOCAL1 != null) {
            var v = LOCAL1.get();
            if (v != null) return v;
        }
        if (LOCAL2 != null) {
            return LOCAL2.get();
        }
        return null;
    }

    public static void init() {
        ThreadLocal<Player> l1 = null;
        ThreadLocal<Player> l2 = null;

        try {
            Class<?> cl = Class.forName("com.by1337.leaf.item.ItemRenderer");
            MethodHandles.lookup().ensureInitialized(cl);
            l1 = getStatic(cl, "BUKKIT_PLAYER_THREAD_LOCAL");
        } catch (Exception ignored) {
        }
        try {
            Class<?> cl = Class.forName("dev.by1337.bmenu.slot.render.BukkitItemRenderer");
            MethodHandles.lookup().ensureInitialized(cl);
            l2 = getStatic(cl, "PLAYER_THREAD_LOCAL");
        } catch (Exception ignored) {
        }
        LOCAL1 = l1;
        LOCAL2 = l2;
    }

    static {
        init();
    }

    private static ThreadLocal<Player> getStatic(Class<?> cl, String fieldName) throws Exception {
        var f = cl.getDeclaredField(fieldName);
        f.setAccessible(true);
        return (ThreadLocal<Player>) f.get(null);
    }

}
