package dev.by1337.core.util.text.minimessage;

import net.kyori.adventure.text.Component;

public class BMM {
    public static Component deserialize(String text) {
        return dev.by1337.core.util.text.minimessage.MiniMessage.deserialize(text);
    }
}
