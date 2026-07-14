package dev.by1337.edsl;

import dev.by1337.plc.PlaceholderApplier;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public class Themes {
    public static Locale defaultLocale = new Locale("ru", "RU");
    private static final Map<String, PlaceholderApplier> themes = new HashMap<>();
    private static Function<Player, String> selector;


    public static @Nullable String getTheme(Player player) {
        if (selector == null) return null;
        return selector.apply(player);
    }


    public static Map<String, PlaceholderApplier> themes() {
        return themes;
    }

    public static void setSelector(Function<Player, String> selector) {
        Themes.selector = selector;
    }
}
