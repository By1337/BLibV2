package dev.by1337.edsl.command.args;

import dev.by1337.cmd.*;
import dev.by1337.edsl.util.Locales;

import java.util.Locale;

public final class ArgumentLocale<C> extends Argument<C, Locale> {
    public ArgumentLocale(String name) { super(name); }

    @Override
    public void parse(C ctx, CommandReader reader, ArgumentMap out) {
        try {
            out.put(name, Locales.parse(reader.readString()));
        } catch (IllegalArgumentException e) {
            throw new CommandMsgError(e.getMessage());
        }
    }

    @Override
    public void suggest(C ctx, CommandReader reader, SuggestionsList suggestions, ArgumentMap args) {}

    @Override public boolean compilable() { return true; }
    @Override public boolean allowAsync() { return true; }
}
