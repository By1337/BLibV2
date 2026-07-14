package dev.by1337.edsl.util;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ByLocaleSelector<T> {
    private final Map<String, T> byLanguageTag = new HashMap<>();
    private final Map<String, T> byLC = new HashMap<>();
    private final Map<String, T> byLanguage = new HashMap<>();
    private final T def;

    public ByLocaleSelector(T def) {
        this.def = def;
    }
    public ByLocaleSelector(Map<Locale, T> map, Locale def){
        map.forEach(this::add);
        this.def = select(def);
    }

    public T select(Locale locale) {
        T t = byLanguageTag.get(locale.toLanguageTag());
        if (t != null) return t;
        var lang = locale.getLanguage();
        t = byLC.get(lang + "-" + locale.getCountry());
        if (t != null) return t;
        return byLanguage.getOrDefault(lang, def);
    }

    public void add(Locale locale, T value) {
        byLanguageTag.put(locale.toLanguageTag(), value);
        byLC.put(locale.getLanguage() + "-" + locale.getCountry(), value);
        byLanguage.put(locale.getLanguage(), value);
    }
}
