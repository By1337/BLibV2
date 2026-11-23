package dev.by1337.edsl;

import dev.by1337.plc.PlaceholderApplier;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.function.Function;

public class Themes {
    public static Locale defaultLocale = new Locale("ru", "RU");

    private static volatile Object cacheToken = new Object();
    private static volatile Function<@Nullable String, @Nullable PlaceholderApplier> themeReplacer;
    private static volatile Function<@Nullable Player, @Nullable String> selector;


    public static @Nullable String getThemeId(@Nullable Player player) {
        var current = selector;
        return current == null ? null : current.apply(player);
    }

    public static @Nullable PlaceholderApplier getTheme(@Nullable String s) {
        var current = themeReplacer;
        return current == null ? null : current.apply(s);
    }

    /** Call after changing the contents of an existing theme resolver. */
    public static void invalidateCache() {
        cacheToken = new Object();
    }

    public static Object cacheToken() {
        return cacheToken;
    }

    public static void setThemeReplacer(@Nullable Function<@Nullable String, @Nullable PlaceholderApplier> themeReplacer) {
        Themes.themeReplacer = themeReplacer;
        invalidateCache();
    }

    public static void setSelector(@Nullable Function<@Nullable Player, @Nullable String> selector) {
        Themes.selector = selector;
        invalidateCache();
    }
}
