package dev.by1337.edsl.util;

import java.util.Locale;
import java.util.IllformedLocaleException;

public final class Locales {
    private Locales() {
    }

    public static Locale parse(String input) {
        if (input == null || input.isBlank()) throw new IllegalArgumentException("Missing locale");
        try {
            Locale locale = new Locale.Builder().setLanguageTag(input.replace('_', '-')).build();
            if (locale.getLanguage().isEmpty()) throw new IllegalArgumentException("Invalid locale: " + input);
            return locale;
        } catch (IllformedLocaleException e) {
            throw new IllegalArgumentException("Invalid locale: " + input, e);
        }
    }
}
