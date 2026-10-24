package dev.by1337.edsl.text;

import dev.by1337.edsl.PlayerLocal;
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
