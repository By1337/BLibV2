package dev.by1337.edsl.text;

import dev.by1337.core.util.text.minimessage.BMM;
import dev.by1337.edsl.PlayerLocal;
import dev.by1337.edsl.Themes;
import dev.by1337.plc.PlaceholderApplier;
import dev.by1337.yaml.decoder.YamlDecoder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Theme-dependent text. Only the result before caller placeholders is cached. */
public final class RawTextComponentLike implements ComponentLike {
    public static final YamlDecoder<RawTextComponentLike> DECODER = YamlDecoder.STRING.map(RawTextComponentLike::new);
    public static final int SOFT_MAX_ARRAY_LENGTH = Integer.MAX_VALUE - 8;
    private static final AtomicInteger COUNTER = new AtomicInteger(1);
    private static final Map<String, Integer> THEME_TO_ID = new ConcurrentHashMap<>();
    private static final PlaceholderApplier IDENTITY = s -> s;

    // Published arrays are immutable. Concurrent misses may compute the same BMM result.
    private volatile Cache cache = new Cache(Themes.cacheToken(), new Pair[2]);
    private final String source;

    public RawTextComponentLike(String source) {
        this.source = Objects.requireNonNull(source);
    }

    public String source() {
        return source;
    }

    @Override
    public @NotNull Component asComponent() {
        return asComponent(null, IDENTITY);
    }

    public @NotNull Component asComponent(PlaceholderApplier placeholders) {
        return asComponent(null, placeholders);
    }

    public @NotNull Component asComponent(@Nullable Player player, PlaceholderApplier placeholders) {
        var formatted = asFormated(player);
        if (formatted.component != null) return formatted.component;
        return BMM.deserialize(placeholders.setPlaceholders(formatted.s));
    }

    /** A null player uses PlayerLocal; the returned component is never locale-rendered. */
    public @NotNull Pair asFormated(@Nullable Player player) {
        String themeId = Themes.getThemeId(player == null ? PlayerLocal.get() : player);
        int theme = themeId == null ? 0 : THEME_TO_ID.computeIfAbsent(themeId, v -> COUNTER.getAndIncrement());
        Object token = Themes.cacheToken();
        var current = cache;
        if (current.token == token && theme < current.entries.length) {
            var pair = current.entries[theme];
            if (pair != null) return pair;
        }
        PlaceholderApplier replacer = Themes.getTheme(themeId);
        String text = replacer == null ? source : replacer.setPlaceholders(source);
        Pair pair = new Pair(text, canBeCached(text) ? BMM.deserialize(text) : null);
        synchronized (this) {
            current = cache;
            Pair[] entries = current.token == token ? current.entries : new Pair[2];
            int capacity = entries.length;
            if (capacity <= theme) capacity = newLength(capacity, theme + 1 - capacity, capacity >> 1);
            entries = Arrays.copyOf(entries, capacity);
            entries[theme] = pair;
            cache = new Cache(token, entries);
        }
        return pair;
    }

    private static boolean canBeCached(String input) {
        return !input.contains("{") && !input.contains("%");
    }

    private record Cache(Object token, Pair[] entries) {}

    public record Pair(String s, @Nullable Component component) {}

    public static int newLength(int oldLength, int minGrowth, int prefGrowth) {
        int prefLength = oldLength + Math.max(minGrowth, prefGrowth);
        if (0 < prefLength && prefLength <= SOFT_MAX_ARRAY_LENGTH) {
            return prefLength;
        } else {
            return hugeLength(oldLength, minGrowth);
        }
    }

    private static int hugeLength(int oldLength, int minGrowth) {
        int minLength = oldLength + minGrowth;
        if (minLength < 0) {
            throw new OutOfMemoryError("Required array length " + oldLength + " + " + minGrowth + " is too large");
        } else return Math.max(minLength, SOFT_MAX_ARRAY_LENGTH);
    }
}
