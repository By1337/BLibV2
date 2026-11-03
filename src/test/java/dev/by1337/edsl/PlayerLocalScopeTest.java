package dev.by1337.edsl;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.*;

class PlayerLocalScopeTest {
    private Player player() {
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{ Player.class },
                (proxy, method, args) -> null);
    }

    @Test
    void restoresNestedScopesAfterFailureAndDoesNotLeakAcrossThreads() {
        Player original = PlayerLocal.get();
        Player first = player(), second = player();
        try (var outer = PlayerLocal.withPlayer(first)) {
            assertSame(first, PlayerLocal.get());
            assertNull(CompletableFuture.supplyAsync(PlayerLocal::get).join());
            assertThrows(IllegalStateException.class, () -> {
                try (var inner = PlayerLocal.withPlayer(second)) {
                    assertSame(second, PlayerLocal.get());
                    throw new IllegalStateException("fixture");
                }
            });
            assertSame(first, PlayerLocal.get());
        }
        assertSame(original, PlayerLocal.get());
    }
}
