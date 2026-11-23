package dev.by1337.core.util.text.component;

import dev.by1337.core.util.text.minimessage.MiniMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;

@Deprecated
public class SourcedComponent implements SourcedComponentLike {
    private final Component component;
    private final String source;

    public SourcedComponent(String source) {
        this.source = source;
        component = MiniMessage.deserialize(source).decoration(TextDecoration.ITALIC, false);
    }

    @Override
    public String source() {
        return source;
    }

    @Override
    public @NotNull Component asComponent() {
        return component;
    }
}
