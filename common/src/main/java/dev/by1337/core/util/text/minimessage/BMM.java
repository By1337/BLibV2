package dev.by1337.core.util.text.minimessage;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class BMM {
    public static Component deserialize(String text) {
        return deserialize(text, null);
    }

    public static Component deserialize(String text, @Nullable Locale locale) {
        return dev.by1337.core.util.text.minimessage.MiniMessage.deserialize(text, locale);
    }
}
