package dev.by1337.edsl.messages;

import dev.by1337.core.util.io.ResourceUtil;
import dev.by1337.edsl.EventHandler;
import dev.by1337.edsl.Themes;
import dev.by1337.edsl.context.EventContext;
import dev.by1337.yaml.YamlMap;
import dev.by1337.yaml.codec.DataResult;
import dev.by1337.yaml.decoder.DataResultBuilder;
import dev.by1337.yaml.decoder.YamlDecoder;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class Messages {
    private final LangData def;
    private final Logger logger;

    private Messages(LangData messages, Logger logger) {
        def = messages;
        this.logger = logger;
    }

    public EventContext handle(String key, EventContext ctx) {
        var target = ctx.target();
        EventHandler v;
        if (target == null) {
            v = def.def.messages.get(key);
        } else {
            v = def.get(target.locale()).messages.get(key);
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
        if (fileName == null) return (c, v) -> DataResult.success((Messages) null);
        return (ctx, ignored) -> {
            var pl = ctx.get(Plugin.class);
            if (!pl.hasResult()) return DataResult.error("Has no plugin in context!");
            var plugin = pl.getOrThrow();
            var file = ResourceUtil.saveOptionalIfNotExist(fileName, plugin);
            if (file == null) return DataResult.success((Messages) null);
            var files = YamlUtil.loadAll(file);

            DataResultBuilder result = new DataResultBuilder();

            YamlDecoder<Data> messagesDecoder = Data.decoder();

            Map<Locale, Data> messages = new LinkedHashMap<>();
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

                var res = messagesDecoder.decode(ctx, map.get());
                if (res.hasError()) result.append(res.error());
                Data data = res.result();
                if (data == null) return res.widen();
                messages.put(locale, data);
            }
            if (messages.isEmpty()) return DataResult.error("No messages found in " + fileName);
            return result.result(new Messages(new LangData(messages, Themes.defaultLocale), plugin.getSLF4JLogger()));
        };
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

}
