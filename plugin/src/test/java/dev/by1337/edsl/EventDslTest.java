package dev.by1337.edsl;

import dev.by1337.core.ServerVersion;
import org.bukkit.Bukkit;
import org.bukkit.Server;

import java.lang.reflect.Proxy;

abstract class EventDslTest {
    static {
        initializeServerVersion();
    }

    private static void initializeServerVersion() {
        try {
            var field = Bukkit.class.getDeclaredField("server");
            field.setAccessible(true);
            Object previous = field.get(null);
            if (previous != null) {
                return;
            }
            Object server = Proxy.newProxyInstance(EventDslTest.class.getClassLoader(),
                    new Class<?>[]{ Server.class, VersionedServer.class }, (proxy, method, args) -> null);
            field.set(null, server);
            try {
                Class.forName(ServerVersion.class.getName());
            } finally {
                field.set(null, previous);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not load the test server version", e);
        }
    }

    private interface VersionedServer {
    }
}
