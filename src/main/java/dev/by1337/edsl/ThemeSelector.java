package dev.by1337.edsl;

import dev.by1337.yaml.YamlMap;
import dev.by1337.yaml.decoder.YamlDecoder;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class ThemeSelector<T> {
    private final Map<String, T> map = new HashMap<>();
    private T def;

    public ThemeSelector(YamlMap yaml, YamlDecoder<T> decoder) {
        if (Themes.themes().isEmpty()) {
            def = decoder.decode(yaml.get()).getOrThrow();
            map.put("default", def);
        } else {
            for (String theme : Themes.themes().keySet()) {
                var copy = yaml.deepCopy();
                copy.deepReplace(Themes.themes().get(theme)::setPlaceholders);
                T t = decoder.decode(copy.get()).getOrThrow();
                map.put(theme, t);
            }
        }
        def = map.get("default");
        if (def == null) {
            def = map.values().iterator().next();
        }
    }
    public T select(@Nullable Player player){
        if (player == null) return def;
        var v = Themes.getTheme(player);
        if (v == null) return def;
        return map.getOrDefault(v, def);
    }
}
