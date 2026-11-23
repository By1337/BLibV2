package dev.by1337.edsl;

import dev.by1337.edsl.text.RawTextComponentLike;
import dev.by1337.plc.PlaceholderApplier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class RawTextComponentLikeTest extends EventDslTest {
    @AfterEach
    void reset() {
        Themes.setSelector(null);
        Themes.setThemeReplacer(null);
        Themes.invalidateCache();
    }

    @Test
    void explicitPlayerOverridesPlayerLocal() throws Exception {
        org.bukkit.entity.Player localPlayer = (org.bukkit.entity.Player) java.lang.reflect.Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{ org.bukkit.entity.Player.class },
                (proxy, method, args) -> null);
        org.bukkit.entity.Player explicitPlayer = (org.bukkit.entity.Player) java.lang.reflect.Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{ org.bukkit.entity.Player.class },
                (proxy, method, args) -> null);
        var field = PlayerLocal.class.getDeclaredField("LOCAL1");
        field.setAccessible(true);
        Object previous = field.get(null);
        ThreadLocal<org.bukkit.entity.Player> local = new ThreadLocal<>();
        local.set(localPlayer);
        field.set(null, local);
        try {
            Themes.setSelector(player -> player == localPlayer ? "local" : "explicit");
            Themes.setThemeReplacer(id -> s -> id);
            var raw = new RawTextComponentLike("{value}");
            assertEquals(Component.text("local"), raw.asComponent());
            assertEquals(Component.text("explicit"), raw.asComponent(explicitPlayer, s -> s));
        } finally {
            field.set(null, previous);
            local.remove();
        }
    }

    @Test
    void absentThemeAndDynamicFallbackNeverReturnNull() {
        var plain = new RawTextComponentLike("hello");
        assertSame(plain.asComponent(), plain.asComponent());
        var dynamic = new RawTextComponentLike("hello {name}");
        assertNull(dynamic.asFormated(null).component());
        assertEquals(Component.text("hello Alex"), dynamic.asComponent(s -> s.replace("{name}", "Alex")));
        assertNotNull(dynamic.asComponent());
    }

    @Test
    void themeIsAppliedBeforeCallerAndOnlyThemeResultIsCached() {
        AtomicInteger themeCalls = new AtomicInteger();
        PlaceholderApplier theme = s -> {
            themeCalls.incrementAndGet();
            return s.replace("{greeting}", "hello {name}");
        };
        Themes.setThemeReplacer(id -> theme);
        var raw = new RawTextComponentLike("{greeting}");
        assertEquals(Component.text("hello Alex"), raw.asComponent(s -> s.replace("{name}", "Alex")));
        assertEquals(Component.text("hello Bob"), raw.asComponent(s -> s.replace("{name}", "Bob")));
        assertEquals(1, themeCalls.get());
        assertEquals("hello {name}", raw.asFormated(null).s());
    }

    @Test
    void resolvedThemeComponentSkipsCallerAndLangRemainsTranslatable() {
        PlaceholderApplier theme = s -> s.replace("{label}", "<lang:example.key>");
        Themes.setThemeReplacer(id -> theme);
        var raw = new RawTextComponentLike("{label}");
        Component component = raw.asComponent(s -> {
            fail("No dynamic placeholders remain");
            return s;
        });
        assertTrue(component instanceof TranslatableComponent);
        assertSame(component, raw.asComponent());
    }

    @Test
    void switchingManyThemesAndReplacingResolverDoesNotReuseStaleResult() {
        AtomicReference<String> selected = new AtomicReference<>();
        Map<String, PlaceholderApplier> resolvers = new ConcurrentHashMap<>();
        Themes.setSelector(player -> selected.get());
        Themes.setThemeReplacer(resolvers::get);
        var raw = new RawTextComponentLike("{value}");
        for (int i = 0; i < 100; i++) {
            String value = "theme-" + i;
            resolvers.put(value, s -> value);
            selected.set(value);
            assertEquals(Component.text(value), raw.asComponent());
        }
        selected.set("theme-0");
        Component original = raw.asComponent();
        assertEquals(Component.text("theme-0"), original);
        resolvers.put("theme-0", s -> "updated");
        Themes.invalidateCache();
        assertEquals(Component.text("updated"), raw.asComponent());
        Themes.invalidateCache();
        assertEquals(Component.text("updated"), raw.asComponent());
    }

    @Test
    void invalidationRecomputesMutableResolver() {
        AtomicReference<String> value = new AtomicReference<>("old");
        PlaceholderApplier resolver = s -> value.get();
        Themes.setThemeReplacer(id -> resolver);
        var raw = new RawTextComponentLike("{value}");
        assertEquals(Component.text("old"), raw.asComponent());
        value.set("new");
        Themes.invalidateCache();
        assertEquals(Component.text("new"), raw.asComponent());
    }

    @Test
    void concurrentThemeMissesSafelyGrowCache() throws Exception {
        ThreadLocal<String> selected = new ThreadLocal<>();
        Themes.setSelector(player -> selected.get());
        Map<String, PlaceholderApplier> resolvers = new ConcurrentHashMap<>();
        for (int i = 0; i < 64; i++) {
            String value = "parallel-" + i;
            resolvers.put(value, s -> value);
        }
        Themes.setThemeReplacer(resolvers::get);
        var raw = new RawTextComponentLike("{value}");
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Void>> tasks = new ArrayList<>();
            for (String value : resolvers.keySet()) tasks.add(() -> {
                selected.set(value);
                try {
                    for (int i = 0; i < 50; i++) assertEquals(Component.text(value), raw.asComponent());
                } finally {
                    selected.remove();
                }
                return null;
            });
            for (Future<Void> task : executor.invokeAll(tasks)) task.get();
        } finally {
            executor.shutdownNow();
        }
    }
}
