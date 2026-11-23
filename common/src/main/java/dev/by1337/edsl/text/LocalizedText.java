package dev.by1337.edsl.text;

import dev.by1337.edsl.PlayerLocal;
import dev.by1337.edsl.util.Locales;
import dev.by1337.yaml.decoder.YamlDecoder;
import java.util.Comparator;
import dev.by1337.edsl.Themes;
import dev.by1337.edsl.util.ByLocaleSelector;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.function.UnaryOperator;

/** Locale selection happens before theme resolution and placeholder application. */
public final class LocalizedText implements ComponentLike {
    /** A raw EventDSL string or a nested locale -> raw string map. English is the fallback. */
    public static final YamlDecoder<LocalizedText> DECODER = RawTextComponentLike.DECODER
            .map(raw -> new LocalizedText(raw, Map.of()))
            .whenMap(YamlDecoder.mapOf(
                    YamlDecoder.STRING, RawTextComponentLike.DECODER).map(LocalizedText::fromLocales));

    public static LocalizedText of(String text) {
        return new LocalizedText(new RawTextComponentLike(text), Map.of());
    }

    private static LocalizedText fromLocales(Map<String, RawTextComponentLike> input) {
        if (input.isEmpty()) throw new IllegalArgumentException("Text locale map must not be empty");
        Map<Locale, ComponentLike> variants = new LinkedHashMap<>();
        input.forEach((key, value) -> {
            Locale locale = Locales.parse(key);
            if (variants.putIfAbsent(locale, value) != null)
                throw new IllegalArgumentException("Duplicate text locale: " + key);
        });
        ComponentLike fallback = variants.get(Locale.US);
        if (fallback == null) fallback = variants.get(Locale.ENGLISH);
        if (fallback == null) fallback = variants.entrySet().stream()
                .filter(entry -> entry.getKey().getLanguage().equals("en"))
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(Locale::toLanguageTag)))
                .map(Map.Entry::getValue).findFirst().orElseGet(() -> variants.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey(Comparator.comparing(Locale::toLanguageTag)))
                        .findFirst().orElseThrow().getValue());
        return new LocalizedText(fallback, variants);
    }

    private final ComponentLike fallback;
    private final Map<Locale, ComponentLike> variants;
    private final ByLocaleSelector<ComponentLike> selector;

    public LocalizedText(ComponentLike fallback, Map<Locale, ComponentLike> variants) {
        this.fallback = fallback;
        this.variants = Map.copyOf(variants);
        selector = new ByLocaleSelector<>(fallback);
        variants.forEach(selector::add);
    }

    public ComponentLike select(Locale locale) {
        return selector.select(locale);
    }

    public LocalizedText map(UnaryOperator<ComponentLike> mapper) {
        Map<Locale, ComponentLike> mapped = new LinkedHashMap<>();
        variants.forEach((locale, value) -> mapped.put(locale, mapper.apply(value)));
        return new LocalizedText(mapper.apply(fallback), mapped);
    }

    @Override
    public @NotNull Component asComponent() {
        var player = PlayerLocal.get();
        return select(player == null ? Themes.defaultLocale : player.locale()).asComponent();
    }
}
