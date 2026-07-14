package dev.by1337.edsl.messages;

import dev.by1337.core.util.io.ResourceUtil;
import dev.by1337.edsl.EventHandler;
import dev.by1337.edsl.Themes;
import dev.by1337.edsl.context.EventContext;
import dev.by1337.yaml.YamlMap;
import dev.by1337.yaml.codec.DataResult;
import dev.by1337.yaml.decoder.DataResultBuilder;
import dev.by1337.yaml.decoder.YamlDecoder;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class Messages {
    private final LangData def;
    private final Map<String, LangData> messages;
    private final Logger logger;

    private Messages(Map<String, LangData> messages, Logger logger) {
        this.messages = messages;
        def = messages.values().iterator().next();
        this.logger = logger;
    }

    public EventContext handle(String key, EventContext ctx) {
        var target = ctx.target();
        EventHandler v;
        if (target == null) {
            v = def.def.messages.get(key);
        } else {
            v = getTheme(target).get(target.locale()).messages.get(key);
        }
        if (v == null) {
            logger.error("Unknown message key {}", key);
            return ctx;
        }
        ctx.setMessages(this);
        v.handle(ctx);
        return ctx;
    }

    public static YamlDecoder<Messages> decoder(String fileName) {
        if (fileName == null) return (c, v) -> DataResult.success(null);
        return (ctx, ignored) -> {
            var pl = ctx.get(Plugin.class);
            if (!pl.hasResult()) return DataResult.error("Has no plugin in context!");
            var plugin = pl.getOrThrow();
            var file = ResourceUtil.saveIfNotExist(fileName, plugin);
            var files = Util.loadAll(file);

            DataResultBuilder result = new DataResultBuilder();

            YamlDecoder<Data> messagesDecoder = Data.decoder();

            Map<Locale, Map<String, Data>> messages = new HashMap<>();
            for (YamlMap map : files) {
                if (map.has("header")) continue;
                Locale locale;
                if (map.has("$lang")) {
                    var langRes = map.get("$lang").asString();
                    if (!langRes.hasResult()) return langRes.widen();
                    var lang = langRes.getOrThrow().split("_", 2);
                    if (lang.length != 2) return DataResult.error("Bad locale! " + langRes.result());
                    locale = new Locale(lang[0], lang[1]);
                    map.set("$lang", null);
                } else {
                    locale = new Locale("ru", "RU");
                }

                if (Themes.themes().isEmpty()) {
                    var res = messagesDecoder.decode(map.get());
                    if (res.hasError()) {
                        result.append(res.error());
                    }
                    Data data = res.result();
                    if (data == null) return res.widen();
                    messages.computeIfAbsent(locale, k -> new LinkedHashMap<>())
                            .put("default", data);
                } else {
                    for (String s : Themes.themes().keySet()) {
                        var copy_map = map.deepCopy();
                        copy_map.deepReplace(Themes.themes().get(s)::setPlaceholders);
                        var res = messagesDecoder.decode(copy_map.get());
                        if (res.hasError()) {
                            result.append(res.error());
                        }
                        Data data = res.result();
                        if (data == null) return res.widen();
                        messages.computeIfAbsent(locale, k -> new LinkedHashMap<>())
                                .put(s, data);
                    }
                }
            }
            Map<String, Map<Locale, Data>> map = new HashMap<>();
            messages.forEach((locale, v) ->
                    v.forEach((theme, data) ->
                            map.computeIfAbsent(theme, k -> new LinkedHashMap<>())
                                    .put(locale, data)));
            Locale defaultLang = Themes.defaultLocale;
            Map<String, LangData> resultMap = new HashMap<>();
            map.forEach((theme, data) ->
                    resultMap.put(theme, new LangData(data, defaultLang))
            );
            return result.result(new Messages(resultMap, plugin.getSLF4JLogger()));
        };
    }

    private LangData getTheme(Player player) {
        var s = Themes.getTheme(player);
        if (s == null) return def;
        return messages.getOrDefault(s, def);
    }

    private static class LangData {
        private final Data def;
        private final boolean single;
        private final Map<String, Data> byLanguage;
        private final Map<String, Data> byLanguageTag;

        public LangData(Map<Locale, Data> map, Locale def) {
            var v = map.get(def);
            if (v == null) {
                this.def = map.values().iterator().next();
            } else {
                this.def = v;
            }
            byLanguage = new HashMap<>();
            byLanguageTag = new HashMap<>();
            map.forEach((s, data) -> {
                byLanguage.put(s.getLanguage(), data);
                byLanguageTag.put(s.toLanguageTag(), data);
            });
            single = byLanguage.size() == 1 && byLanguageTag.size() == 1;
        }

        public Data get(@Nullable Locale locale) {
            if (single || locale == null) return def;
            var v = byLanguageTag.get(locale.toLanguageTag());
            if (v != null) return v;
            return byLanguage.getOrDefault(locale.getLanguage(), def);
        }

    }


    private record Data(Map<String, EventHandler> messages) {
        private static YamlDecoder<Data> decoder() {
            return YamlDecoder.mapOf(YamlDecoder.STRING, EventHandler.decoder())
                    .map(Data::new);
        }
    }

    /*private static class Header {
        public static final YamlDecoder<Header> DECODER = RecordYamlDecoder.mapOf(
                Header::new,
                YamlDecoder.STRING.fieldOf("default_lang", "ru_RU"),
                YamlDecoder.STRING.fieldOf("theme_papi_detector"),
                YamlDecoder.mapOf(YamlDecoder.STRING, YamlDecoder.mapOf(YamlDecoder.STRING, YamlDecoder.STRING))
                        .fieldOf("themes", Map.of())
        );
        public final Locale default_lang;
        public final String theme_papi_detector;
        public final Map<String, PlaceholderApplier> themes;

        public Header(String defaultLang, @Nullable String themePapiDetector, Map<String, Map<String, String>> themes) {
            var lang = defaultLang.split("_", 2);
            if (lang.length != 2) throw new IllegalArgumentException("Invalid default locale!");
            default_lang = new Locale(lang[0], lang[1]);
            theme_papi_detector = themePapiDetector;
            this.themes = new LinkedHashMap<>();
            themes.forEach((k, v) -> {
                Placeholders<Void> placeholders = new Placeholders<>();
                v.forEach(placeholders::of);
                this.themes.put(k, placeholders.bind(null));
            });
        }
    }*/
}
