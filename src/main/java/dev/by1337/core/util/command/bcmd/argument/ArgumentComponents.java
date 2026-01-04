package dev.by1337.core.util.command.bcmd.argument;

import dev.by1337.cmd.*;
import dev.by1337.core.util.text.minimessage.MiniMessage;
import net.kyori.adventure.text.Component;

public class ArgumentComponents<C> extends Argument<C, Component> {

    public ArgumentComponents(String name) {
        super(name);
    }

    @Override
    public void parse(C ctx, CommandReader reader, ArgumentMap out) throws CommandMsgError {
        String src = reader.src();
        int idx = reader.ridx();
        if (idx >= src.length()) {
            return;
        }
        out.put(name, MiniMessage.deserialize(src.substring(idx)));
        reader.ridx(reader.length());
    }

    @Override
    public void suggest(C ctx, CommandReader reader, SuggestionsList suggestions, ArgumentMap args) throws CommandMsgError {
        reader.ridx(reader.length());
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