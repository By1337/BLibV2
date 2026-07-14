package dev.by1337.edsl;

import dev.by1337.core.util.io.ResourceUtil;
import dev.by1337.core.util.text.MessageFormatter;
import dev.by1337.core.util.text.minimessage.MiniMessage;
import dev.by1337.edsl.messages.Util;
import dev.by1337.edsl.util.ByLocaleSelector;
import dev.by1337.yaml.YamlMap;
import dev.by1337.yaml.codec.DataResult;
import dev.by1337.yaml.decoder.YamlDecoder;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import net.kyori.adventure.translation.Translator;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LangTranslator implements Translator {
    private final ByLocaleSelector<Map<String, Object>> lang;
    private final Plugin plugin;
    private final Key key;

    public LangTranslator(ByLocaleSelector<Map<String, Object>> lang, Plugin plugin) {
        this.lang = lang;
        this.plugin = plugin;
        key = new NamespacedKey(plugin, "lang");
    }

    private static final YamlDecoder<Map<String, Map<String, String>>> RAW_LANG = YamlDecoder.mapOf(
            YamlDecoder.STRING,
            YamlDecoder.mapOf(YamlDecoder.STRING, YamlDecoder.STRING)
    );
    private static final String MAGIC_KEY = "тут_была_точка_uwu";

    public static YamlDecoder<LangTranslator> decoder(String fileName) {
        if (fileName == null) return (c, v) -> DataResult.success(null);
        return (ctx, ignored) -> {
            var pl = ctx.get(Plugin.class);
            if (!pl.hasResult()) return DataResult.error("Has no plugin in context!");
            var plugin = pl.getOrThrow();
            var file = ResourceUtil.saveIfNotExist(fileName, plugin);
            //bukkit разворачивает ключи с '.'
            YamlMap yaml = Util.replaceAndLoad(file, ".", MAGIC_KEY).deepCopy();
            yaml.deepReplace(s -> s.replace(MAGIC_KEY, "."));
            return RAW_LANG.decode(ctx, yaml.get()).flatMap(raw -> {
                Map<Locale, Map<String, Object>> lang = new HashMap<>();
                for (String langKey : raw.keySet()) {
                    Map<String, Object> map = new HashMap<>();
                    var rawMap = raw.get(langKey);
                    for (Map.Entry<String, String> e : rawMap.entrySet()) {
                        String key = (plugin.getName() + "." + e.getKey().replace(MAGIC_KEY, ".")).toLowerCase(Locale.ROOT);
                        String value = e.getValue();
                        if (value.contains("{")) {
                            map.put(key, value);
                        } else {
                            map.put(key, MiniMessage.deserialize(value));
                        }
                    }
                    lang.put(Translator.parseLocale(langKey), map);
                }
                return DataResult.success(new LangTranslator(new ByLocaleSelector<>(lang, Themes.defaultLocale), plugin));

            });
        };
    }

    @Override
    public @NonNull Key name() {
        return key;
    }

    @Override
    public @Nullable MessageFormat translate(@NonNull String key, @NonNull Locale locale) {
        return null;
    }

    public @Nullable Component translate(final TranslatableComponent component, final Locale locale) {
        var map = lang.select(locale);
        if (map == null) return null;
        Object result = map.get(component.key());
        if (result == null) return null;
        if (result instanceof Component c) return c;
        Component c = MiniMessage.deserialize(MessageFormatter.apply(String.valueOf(result), toArr(component.arguments())));
        return likeA(component, c);
    }

    private static Component likeA(TranslatableComponent v, Component v2){
        return v2
                .style(b -> b.merge(v.style()))
                .children(v.children());
    }

    private static Object[] toArr(List<TranslationArgument> arguments) {
        if (arguments.isEmpty()) return new Object[0];
        Object[] res = new Object[arguments.size()];
        for (int i = 0; i < res.length; i++) {
            var v = arguments.get(i).value();
            if (v instanceof TextComponent t) {
                res[i] = t.content();
            } else {
                //todo?
                res[i] = v;
            }
        }
        return res;
    }
}
