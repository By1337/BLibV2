package dev.by1337.core.util.text.component;


import dev.by1337.core.util.text.minimessage.MiniMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.checkerframework.checker.nullness.qual.NonNull;

public class JsonComponent implements SourcedComponentLike {
    private final String source;
    private final String json;
    private final Component component;

    public JsonComponent(String source) {
        this.source = source;
        component = MiniMessage.deserialize(source).decoration(TextDecoration.ITALIC, false);
        this.json = GsonComponentSerializer.gson().serialize(component);
    }

    public String json() {
        return json;
    }

    public String source() {
        return source;
    }

    @Override
    public @NonNull Component asComponent() {
        return component;
    }
}
