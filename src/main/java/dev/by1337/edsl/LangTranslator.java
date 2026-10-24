package dev.by1337.edsl;

import dev.by1337.core.util.io.ResourceUtil;
import dev.by1337.core.util.text.MessageFormatter;
import dev.by1337.core.util.text.minimessage.MiniMessage;
import dev.by1337.edsl.text.RawTextComponentLike;
import dev.by1337.edsl.messages.YamlUtil;
import dev.by1337.edsl.util.ByLocaleSelector;
import dev.by1337.plc.PapiResolver;
import dev.by1337.yaml.YamlMap;
import dev.by1337.yaml.codec.DataResult;
import dev.by1337.yaml.decoder.YamlDecoder;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.*;
import net.kyori.adventure.translation.Translator;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;
import java.util.*;

public class LangTranslator implements Translator {
    private final ThemeSelector<ByLocaleSelector<Map<String, Object>>> lang;
    private final Plugin plugin;
    private final Key key;

    public LangTranslator(ThemeSelector<ByLocaleSelector<Map<String, Object>>> lang, Plugin plugin) {
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
        if (fileName == null) return (c, v) -> DataResult.success((LangTranslator)null);
        return (ctx, ignored) -> {
            var pl = ctx.get(Plugin.class);
            if (!pl.hasResult()) return DataResult.error("Has no plugin in context!");
            var plugin = pl.getOrThrow();
            var file = ResourceUtil.saveOptionalIfNotExist(fileName, plugin);
            if (file == null) return DataResult.success((LangTranslator)null);
            //bukkit разворачивает ключи с '.'
            YamlMap yaml = YamlUtil.replaceAndLoad(file, ".", MAGIC_KEY).deepCopy();
            yaml.deepReplace(s -> s.replace(MAGIC_KEY, "."));

            return RAW_LANG.decode(ctx, yaml.get()).map(raw -> {
                Map<Locale, Map<String, Object>> lang = new HashMap<>();
                for (var entry : raw.entrySet()) {
                    Map<String, Object> values = new HashMap<>();
                    entry.getValue().forEach((name, value) -> {
                        String key = (plugin.getName() + "." + name).toLowerCase(Locale.ROOT);
                        values.put(key, new RawTextComponentLike(value));
                    });
                    lang.put(Translator.parseLocale(entry.getKey()), values);
                }
                return new LangTranslator(new ThemeSelector<>(new ByLocaleSelector<>(lang, Themes.defaultLocale)), plugin);
            });
        };
    }

    @Override
    public @NotNull Key name() {
        return key;
    }

    @Override
    public @Nullable MessageFormat translate(@NotNull String key, @NotNull Locale locale) {
        return null;
    }

    public @Nullable Component translate(final @NotNull TranslatableComponent component, final @NotNull Locale locale) {
        Player player = PlayerLocal.get();
        var map = lang.select(player).select(locale);
        if (map == null) return null;
        Object result = map.get(component.key());
        if (result == null) return null;
        if (result instanceof RawTextComponentLike raw) {
            return likeA(component, raw.asComponent(player, text -> PapiResolver.INSTANCE.setPlaceholders(
                    MessageFormatter.apply(text, toArr(component.arguments())), player)));
        }
        if (result instanceof Component c) return likeA(component, c);
        Component c = MiniMessage.deserialize(
                PapiResolver.INSTANCE.setPlaceholders(MessageFormatter.apply(String.valueOf(result), toArr(component.arguments())), player)
        );
        return likeA(component, c);
    }

    private static Component likeA(TranslatableComponent v, Component v2) {
        return append(v2.style(b -> b.merge(v.style())), v.children());
    }

    @Contract(pure = true)
    private static Component append(Component base, final List<? extends ComponentLike> components) {
        if (components.isEmpty()) return base;
        if (base.children().isEmpty()) return base.children(components);

        final List<ComponentLike> newChildren = new ArrayList<>(components.size() + base.children().size());
        newChildren.addAll(base.children());
        newChildren.addAll(components);
        return base.children(newChildren);
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
