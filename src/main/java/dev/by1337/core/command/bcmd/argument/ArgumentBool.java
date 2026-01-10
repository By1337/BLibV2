package dev.by1337.core.command.bcmd.argument;

import dev.by1337.cmd.*;

public class ArgumentBool<C> extends Argument<C, Boolean> {

    public ArgumentBool(String name) {
        super(name);
    }


    @Override
    public void parse(C ctx, CommandReader reader, ArgumentMap out) throws CommandMsgError {
        String str = reader.readString();
        out.put(name, Boolean.parseBoolean(str));
    }

    @Override
    public void suggest(C ctx, CommandReader reader, SuggestionsList suggestions, ArgumentMap args) throws CommandMsgError {
        reader.readString();
        suggestions.suggest("true");
        suggestions.suggest("false");
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
