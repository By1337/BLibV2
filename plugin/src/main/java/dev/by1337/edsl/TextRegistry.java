package dev.by1337.edsl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TranslationArgument;
import net.kyori.adventure.text.TranslatableComponent;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Scoped references for shared entities/items. Locale selection happens during packet encoding,
 * not when the reference is created. Reuse the decoded text; changing arguments allocates no keys.
 * Close only after the objects carrying these references have been removed.
 */
public final class TextRegistry implements AutoCloseable {
    private final String prefix = "eventdsl.runtime." + UUID.randomUUID();
    private final Map<ComponentLike, String> keys = new IdentityHashMap<>();
    private final Map<String, ComponentLike> translations = new HashMap<>();
    private boolean closed;

    /** Arguments use EventDSL's {0}, {1}, ... syntax and remain attached to the lang component. */
    public synchronized TranslatableComponent component(ComponentLike text, Object... arguments) {
        if (closed) throw new IllegalStateException("Text registry is closed");
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(arguments, "arguments");
        TranslationArgument[] args = new TranslationArgument[arguments.length];
        for (int i = 0; i < args.length; i++) {
            Object value = Objects.requireNonNull(arguments[i], "argument");
            args[i] = TranslationArgument.component(value instanceof ComponentLike component
                    ? component.asComponent() : Component.text(String.valueOf(value)));
        }
        String key = keys.get(text);
        if (key == null) {
            key = prefix + "." + keys.size();
            keys.put(text, key);
            translations.put(key, text);
            LangTranslator.registerTranslations(prefix, translations);
        }
        return Component.translatable(key).arguments(args);
    }

    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        LangTranslator.removeRuntimeTranslations(prefix);
        keys.clear();
        translations.clear();
    }
}
