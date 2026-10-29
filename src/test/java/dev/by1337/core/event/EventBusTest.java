package dev.by1337.core.event;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.event.EventHandler;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Timeout;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class EventBusTest {
    private final Logger logger = Logger.getAnonymousLogger();
    private final EventBus bus = new EventBus(logger);
    private final List<LogRecord> unexpectedErrors = new ArrayList<>();
    private static boolean installedServer;

    EventBusTest() {
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            @Override
            public synchronized void publish(LogRecord record) {
                unexpectedErrors.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
    }

    @BeforeAll
    static void installServerStub() {
        if (Bukkit.getServer() != null) return;
        Thread mainThread = Thread.currentThread();
        Server server = (Server) Proxy.newProxyInstance(Server.class.getClassLoader(), new Class<?>[]{ Server.class }, (proxy, method, args) -> {
            switch (method.getName()) {
                case "isPrimaryThread":
                    return Thread.currentThread() == mainThread;
                case "getLogger":
                    return Logger.getAnonymousLogger();
                case "getName":
                    return "EventBusTest";
                case "getVersion":
                case "getBukkitVersion":
                    return "test";
                default:
                    throw new UnsupportedOperationException(method.getName());
            }
        });
        Bukkit.setServer(server);
        installedServer = true;
    }

    @AfterAll
    static void restoreServer() throws Exception {
        if (installedServer) {
            var field = Bukkit.class.getDeclaredField("server");
            field.setAccessible(true);
            field.set(null, null);
        }
    }

    @AfterEach
    void noSwallowedAssertionFailures() {
        // The bus intentionally catches Throwable, including assertions in test callbacks.
        // Verify its logs so failed callback assertions cannot silently pass the test.
        bus.close();
        assertTrue(unexpectedErrors.isEmpty(), () -> unexpectedErrors.toString());
    }

    @Test
    void deliversToMultipleSubscribersInOrderAndOnlyForMatchingIdentifier() {
        List<Integer> calls = new ArrayList<>();
        bus.on("balance", id -> calls.add(1));
        bus.on("balance", id -> calls.add(2));
        bus.on("other", id -> fail("Wrong event"));
        bus.emit(new String("balance"), UUID.randomUUID());
        assertEquals(List.of(1, 2), calls);
    }

    @Test
    void noSubscribersAndNoHistory() {
        bus.emit("unknown", null);
        bus.on("unknown", id -> fail("No replay"));
    }

    @Test
    void passesUuidAndGlobalNullExactlyOnce() {
        List<UUID> calls = new ArrayList<>();
        UUID player = UUID.randomUUID();
        bus.on("balance", calls::add);
        bus.emit("balance", player);
        bus.emit("balance", null);
        assertEquals(2, calls.size());
        assertSame(player, calls.get(0));
        assertNull(calls.get(1));
    }

    @Test
    void closeIsIdempotentAndRemovesOnlyItsOwnRegistration() {
        AtomicInteger calls = new AtomicInteger();
        var first = bus.on("balance", id -> calls.incrementAndGet());
        var second = bus.on("balance", id -> calls.incrementAndGet());
        first.close();
        first.close();
        bus.emit("balance", null);
        assertEquals(1, calls.get());
        second.close();
        bus.emit("balance", null);
        assertEquals(1, calls.get());
    }

    @Test
    void handlerCanCloseItselfAndRecursivelyEmit() {
        AtomicInteger calls = new AtomicInteger();
        Subscription[] self = new Subscription[1];
        self[0] = bus.on("balance", id -> {
            calls.incrementAndGet();
            self[0].close();
            self[0].close();
            bus.emit("balance", id);
        });
        bus.emit("balance", null);
        bus.emit("balance", null);
        assertEquals(1, calls.get());
    }

    @Test
    void handlerCanCloseNextListenerBeforeItIsCalled() {
        Subscription[] next = new Subscription[1];
        bus.on("balance", id -> next[0].close());
        next[0] = bus.on("balance", id -> fail("Already closed"));
        AtomicInteger calls = new AtomicInteger();
        bus.on("balance", id -> calls.incrementAndGet());
        bus.emit("balance", null);
        assertEquals(1, calls.get());
    }

    @Test
    void subscriptionDuringDeliveryOnlyAffectsNextSnapshot() {
        AtomicInteger calls = new AtomicInteger();
        bus.on("balance", id -> bus.on("balance", later -> calls.incrementAndGet()));
        bus.emit("balance", null);
        assertEquals(0, calls.get());
        bus.emit("balance", null);
        assertEquals(1, calls.get());
    }

    @Test
    void handlerFailureIsLoggedWithEventAndOwnerAndDeliveryContinues() {
        List<LogRecord> errors = new ArrayList<>();
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                errors.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
        Plugin owner = plugin("Economy", new AtomicBoolean(true));
        var failure = new IllegalStateException("broken");
        bus.on(owner, "balance", id -> {
            throw failure;
        });
        bus.on("balance", id -> {
            throw new LinkageError("unloaded dependency");
        });
        AtomicInteger calls = new AtomicInteger();
        bus.on("balance", id -> calls.incrementAndGet());
        bus.emit("balance", null);
        assertEquals(1, calls.get());
        assertEquals(2, errors.size());
        assertSame(failure, errors.get(0).getThrown());
        assertTrue(errors.get(0).getMessage().contains("balance"));
        assertTrue(errors.get(0).getMessage().contains("Economy"));
        unexpectedErrors.clear();
    }

    @Test
    @Timeout(20)
    void concurrentEmitSubscribeAndUnsubscribe() throws Exception {
        var executor = Executors.newFixedThreadPool(6);
        var start = new CountDownLatch(1);
        AtomicInteger stableCalls = new AtomicInteger();
        AtomicInteger transientCalls = new AtomicInteger();
        bus.on("balance", id -> stableCalls.incrementAndGet());
        List<java.util.concurrent.Future<?>> tasks = new ArrayList<>();
        try {
            for (int t = 0; t < 4; t++) {
                tasks.add(executor.submit(() -> {
                    start.await();
                    for (int i = 0; i < 50000; i++) bus.emit("balance", null);
                    return null;
                }));
            }
            for (int t = 0; t < 2; t++) {
                tasks.add(executor.submit(() -> {
                    start.await();
                    for (int i = 0; i < 10000; i++) {
                        try (var subscription = bus.on("balance", id -> transientCalls.incrementAndGet())) {
                            bus.emit("balance", null);
                        }
                    }
                    return null;
                }));
            }
            start.countDown();
            for (var task : tasks) task.get(15, TimeUnit.SECONDS);
            assertEquals(220000, stableCalls.get());
            int before = transientCalls.get();
            bus.emit("balance", null);
            assertEquals(before, transientCalls.get());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @Timeout(10)
    void deliveryRunsOnEmitterThreadAndCloseDoesNotWaitForRunningCallback() throws Exception {
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        AtomicReference<Thread> actual = new AtomicReference<>();
        var subscription = bus.on("balance", id -> {
            actual.set(Thread.currentThread());
            entered.countDown();
            try {
                assertTrue(release.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                throw new AssertionError(e);
            }
        });
        var executor = Executors.newSingleThreadExecutor();
        try {
            var task = executor.submit(() -> {
                bus.emit("balance", null);
                return Thread.currentThread();
            });
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            subscription.close();
            bus.emit("balance", null);
            release.countDown();
            assertSame(task.get(5, TimeUnit.SECONDS), actual.get());
            assertNotSame(Thread.currentThread(), actual.get());
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void pluginCanRegisterFreshSubscriptionsInOnEnableWithoutPluginEnableEvent() throws Exception {
        Plugin host = plugin("BLib", new AtomicBoolean(true));
        AtomicBoolean enabled = new AtomicBoolean(true);
        Plugin owner = plugin("Economy", enabled);
        var lifecycle = new EventBusLifecycle(host, bus);
        assertNotNull(EventBusLifecycle.class.getMethod("onDisable", PluginDisableEvent.class).getAnnotation(EventHandler.class));
        var old = bus.on(owner, "balance", id -> fail("Disabled subscription"));
        bus.on(owner, "reload", id -> fail("Disabled subscription"));
        AtomicInteger other = new AtomicInteger();
        bus.on(host, "balance", id -> other.incrementAndGet());
        // Bukkit may still report enabled while dispatching PluginDisableEvent.
        lifecycle.onDisable(new PluginDisableEvent(owner));
        enabled.set(false);
        assertThrows(IllegalStateException.class, () -> bus.on(owner, "balance", id -> {
        }));
        old.close();
        lifecycle.onDisable(new PluginDisableEvent(owner));
        bus.emit("balance", null);
        bus.emit("reload", null);
        assertEquals(1, other.get());
        enabled.set(true);
        // onEnable registers fresh listeners before Bukkit dispatches PluginEnableEvent.
        AtomicInteger fresh = new AtomicInteger();
        bus.on(owner, "balance", id -> fresh.incrementAndGet());
        bus.emit("balance", null);
        assertEquals(1, fresh.get());
        old.close();
        bus.emit("balance", null);
        assertEquals(2, fresh.get());
        enabled.set(false);
        lifecycle.onDisable(new PluginDisableEvent(owner));
        bus.emit("balance", null);
        assertEquals(2, fresh.get());
    }

    @Test
    @Timeout(15)
    void pluginDisableRacingWithRegistrationLeavesNoHandlers() throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        try {
            for (int round = 0; round < 100; round++) {
                AtomicBoolean enabled = new AtomicBoolean(true);
                Plugin owner = plugin("Economy", enabled);
                var lifecycle = new EventBusLifecycle(plugin("Host", new AtomicBoolean(true)), bus);
                var start = new CountDownLatch(1);
                var registration = executor.submit(() -> {
                    start.await();
                    for (int i = 0; i < 100; i++) {
                        try {
                            bus.on(owner, "balance", id -> fail("Leaked registration"));
                        } catch (IllegalStateException expected) {
                            break;
                        }
                    }
                    return null;
                });
                var disable = executor.submit(() -> {
                    start.await();
                    enabled.set(false);
                    lifecycle.onDisable(new PluginDisableEvent(owner));
                    return null;
                });
                start.countDown();
                registration.get(5, TimeUnit.SECONDS);
                disable.get(5, TimeUnit.SECONDS);
                bus.emit("balance", null);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void hostDisableClosesBusAndOldHandlesCanStillClose() {
        Plugin host = plugin("BLib", new AtomicBoolean(true));
        var lifecycle = new EventBusLifecycle(host, bus);
        var handle = bus.on("balance", id -> fail("Closed bus"));
        lifecycle.onDisable(new PluginDisableEvent(host));
        lifecycle.close();
        handle.close();
        bus.emit("balance", null);
        assertThrows(IllegalStateException.class, () -> bus.on("balance", id -> {
        }));
    }

    @Test
    void rejectsNullContractsAndDisabledOwners() {
        assertThrows(NullPointerException.class, () -> bus.on(null, id -> {
        }));
        assertThrows(NullPointerException.class, () -> bus.on("balance", null));
        assertThrows(NullPointerException.class, () -> bus.on(null, "balance", id -> {
        }));
        assertThrows(NullPointerException.class, () -> bus.emit(null, null));
        assertThrows(IllegalStateException.class, () -> bus.on(plugin("Disabled", new AtomicBoolean(false)), "balance", id -> {
        }));
    }

    private static Plugin plugin(String name, AtomicBoolean enabled) {
        return (Plugin) Proxy.newProxyInstance(Plugin.class.getClassLoader(), new Class<?>[]{ Plugin.class }, (proxy, method, args) -> {
            switch (method.getName()) {
                case "getName":
                    return name;
                case "isEnabled":
                    return enabled.get();
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "equals":
                    return proxy == args[0];
                case "toString":
                    return name;
                default:
                    throw new UnsupportedOperationException(method.getName());
            }
        });
    }
}
