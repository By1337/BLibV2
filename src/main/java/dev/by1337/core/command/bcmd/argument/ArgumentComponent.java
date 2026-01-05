package dev.by1337.core.command.bcmd.argument;

import dev.by1337.cmd.*;
import dev.by1337.core.util.text.minimessage.MiniMessage;
import net.kyori.adventure.text.Component;

public class ArgumentComponent<C> extends Argument<C, Component> {

    public ArgumentComponent(String name) {
        super(name);
    }

    @Override
    public void parse(C ctx, CommandReader reader, ArgumentMap out) throws CommandMsgError {
        String src = reader.readString();
        out.put(name, MiniMessage.deserialize(src));
        reader.ridx(reader.length());
    }

    @Override
    public void suggest(C ctx, CommandReader reader, SuggestionsList suggestions, ArgumentMap args) throws CommandMsgError {
        String s = reader.readString();
        suggestions.suggest(s);
    }

    @Override
    public boolean compilable() {
        return true;
    }

    @Override
    public boolean allowAsync() {
        return true;
    }
}