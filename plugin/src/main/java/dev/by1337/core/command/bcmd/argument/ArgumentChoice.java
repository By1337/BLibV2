package dev.by1337.core.command.bcmd.argument;

import dev.by1337.cmd.*;
import dev.by1337.core.command.bcmd.ArgumentParseError;

import java.util.Collection;
import java.util.function.Supplier;

public class ArgumentChoice<C> extends Argument<C, String> {

    private final Supplier<Collection<String>> exx;

    public ArgumentChoice(String name, Supplier<Collection<String>> exx) {
        super(name);
        this.exx = exx;
    }

    public ArgumentChoice(String name, Collection<String> exx) {
        super(name);
        this.exx = () -> exx;
    }

    @Override
    public void parse(C ctx, CommandReader reader, ArgumentMap out) throws CommandMsgError {
        String str = reader.readString();
        if (str.isEmpty()) return;
        var list = exx.get();
        if (list.contains(str)) {
            out.put(name, str);
        } else {
            throw new ArgumentParseError("invalid-choice", name, str, String.join(", ", list));
        }
    }

    @Override
    public void suggest(C ctx, CommandReader reader, SuggestionsList suggestions, ArgumentMap args) throws CommandMsgError {
        String input = reader.readString();
        for (String value : exx.get()) {
            if (value.startsWith(input)) {
                suggestions.suggest(value);
                if (!suggestions.hasFree()) return;
            }
        }
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
