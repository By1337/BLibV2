package dev.by1337.core.lang;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.Reader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Parsed translation table. Mirrors BLib v1's {@code Translation} string-resolution model:
 * resolves a message key to the text for the {@link #getDefaultLocale() default locale},
 * returning {@code null} when the key is unknown so callers fall back to the key itself.
 *
 * <p>Only the plain-string resolution used by the bcmd command framework is implemented here;
 * adventure {@code Component} translation lives in BLib v1's {@code Translation} and is handled
 * by MiniMessage elsewhere in BLibV2.
 */
public class Translation {
    private final Locale defaultLocale;
    private final Map<String, Map<Locale, String>> messages;

    public Translation(Locale defaultLocale, Map<String, Map<Locale, String>> messages) {
        this.defaultLocale = defaultLocale;
        this.messages = messages;
    }

    public static Translation fromJson(Reader reader) {
        JsonObject root = new Gson().fromJson(reader, JsonObject.class);
        Locale defaultLocale = root.has("default-locale")
                ? parseLocale(root.get("default-locale").getAsString())
                : Locale.ENGLISH;
        Map<String, Map<Locale, String>> messages = new HashMap<>();
        JsonObject msgObj = root.getAsJsonObject("messages");
        for (var e : msgObj.entrySet()) {
            String key = e.getKey();
            JsonObject langs = e.getValue().getAsJsonObject();
            Map<Locale, String> byLocale = new HashMap<>();
            for (var le : langs.entrySet()) {
                byLocale.put(parseLocale(le.getKey()), le.getValue().getAsString());
            }
            messages.put(key, byLocale);
        }
        return new Translation(defaultLocale, messages);
    }

    public String translate(String key) {
        return translateByLocale(key, defaultLocale);
    }

    public String translateByLocale(String key, Locale locale) {
        Map<Locale, String> byLocale = messages.get(key);
        if (byLocale == null) return null;
        String v = locale != null ? byLocale.get(locale) : null;
        return v != null ? v : byLocale.get(defaultLocale);
    }

    public Locale getDefaultLocale() {
        return defaultLocale;
    }

    private static Locale parseLocale(String s) {
        String[] parts = s.split("_", 3);
        if (parts.length == 1) return new Locale(parts[0]);
        if (parts.length == 2) return new Locale(parts[0], parts[1]);
        return new Locale(parts[0], parts[1], parts[2]);
    }
}
