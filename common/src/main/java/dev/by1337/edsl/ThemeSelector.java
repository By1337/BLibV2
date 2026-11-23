package dev.by1337.edsl;

import dev.by1337.plc.PlaceholderApplier;
import dev.by1337.yaml.YamlMap;
import dev.by1337.yaml.decoder.YamlDecoder;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class ThemeSelector<T> {
    private final Map<String, Entry<T>> map = new HashMap<>();
    private final @Nullable YamlMap yaml;
    private final @Nullable YamlDecoder<T> decoder;
    private final T def;
    private Object token = Themes.cacheToken();

    public ThemeSelector(T value) {
        yaml = null;
        decoder = null;
        def = value;
    }

    public ThemeSelector(YamlMap yaml, YamlDecoder<T> decoder) {
        this.yaml = yaml.deepCopy();
        this.decoder = decoder;
        def = decoder.decode(this.yaml.get()).getOrThrow();
    }

    public synchronized T select(@Nullable Player player) {
        if (yaml == null) return def;
        String id = Themes.getThemeId(player == null ? PlayerLocal.get() : player);
        var replacer = Themes.getTheme(id);
        if (replacer == null) return def;
        Object currentToken = Themes.cacheToken();
        if (token != currentToken) {
            map.clear();
            token = currentToken;
        }
        var entry = map.get(id);
        if (entry != null && entry.replacer == replacer) return entry.value;
        var copy = yaml.deepCopy();
        copy.deepReplace(replacer::setPlaceholders);
        T value = decoder.decode(copy.get()).getOrThrow();
        map.put(id, new Entry<>(replacer, value));
        return value;
    }

    private record Entry<T>(PlaceholderApplier replacer, T value) {
    }
}
