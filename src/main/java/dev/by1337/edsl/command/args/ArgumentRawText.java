package dev.by1337.edsl.command.args;

import dev.by1337.cmd.*;
import dev.by1337.edsl.text.RawTextComponentLike;

/** Keeps text unresolved when a command is compiled. */
public final class ArgumentRawText<C> extends Argument<C, RawTextComponentLike> {
    public ArgumentRawText(String name) {
        super(name);
    }

    @Override
    public void parse(C ctx, CommandReader reader, ArgumentMap out) {
        if (reader.ridx() >= reader.length()) return;
        out.put(name, new RawTextComponentLike(reader.src().substring(reader.ridx())));
        reader.ridx(reader.length());
    }

    @Override
    public void suggest(C ctx, CommandReader reader, SuggestionsList suggestions, ArgumentMap args) {
        reader.ridx(reader.length());
    }

    @Override
    public boolean compilable() { return true; }

    @Override
    public boolean allowAsync() { return true; }
}
