package dev.by1337.core.command.bcmd.argument;

import dev.by1337.cmd.*;
import dev.by1337.core.command.bcmd.ArgumentParseError;

public class ArgumentBool<C> extends Argument<C, Boolean> {

    public ArgumentBool(String name) {
        super(name);
    }


    @Override
    public void parse(C ctx, CommandReader reader, ArgumentMap out) throws CommandMsgError {
        String str = reader.readString();
        if (str.isEmpty()) return;
        if (!str.equalsIgnoreCase("true") && !str.equalsIgnoreCase("false")) {
            throw new ArgumentParseError("invalid-boolean", name, str, "true, false");
        }
        out.put(name, Boolean.parseBoolean(str));
    }

    @Override
    public void suggest(C ctx, CommandReader reader, SuggestionsList suggestions, ArgumentMap args) throws CommandMsgError {
        String input = reader.readString().toLowerCase(java.util.Locale.ROOT);
        if ("true".startsWith(input)) suggestions.suggest("true");
        if ("false".startsWith(input)) suggestions.suggest("false");
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
