package dev.by1337.core.command.bcmd.argument;

import dev.by1337.cmd.*;

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
        var list = exx.get();
        if (list.contains(str)) {
            out.put(name, str);
        }
    }

    @Override
    public void suggest(C ctx, CommandReader reader, SuggestionsList suggestions, ArgumentMap args) throws CommandMsgError {
        reader.readString();
        var list = exx.get();
        if (list.size() < 30) {
            for (String string : list) {
                suggestions.suggest(string);
            }
        } else {
            int i = 0;
            for (String string : list) {
                suggestions.suggest(string);
                if (i++ >= 30) {
                    return;
                }
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
