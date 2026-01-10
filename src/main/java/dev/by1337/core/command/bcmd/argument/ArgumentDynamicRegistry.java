package dev.by1337.core.command.bcmd.argument;

import dev.by1337.cmd.ArgumentMap;
import dev.by1337.cmd.CommandMsgError;
import dev.by1337.cmd.CommandReader;
import dev.by1337.cmd.SuggestionsList;
import org.bukkit.Keyed;

import java.util.function.Supplier;

public class ArgumentDynamicRegistry<C, E extends Keyed> extends ArgumentRegistry<C, E> {
    private final Supplier<Iterable<E>> registry;
    private final boolean noNamespace;
    private Iterable<E> last;

    public ArgumentDynamicRegistry(String name, Supplier<Iterable<E>> registry) {
        this(name, registry, false);
    }

    public ArgumentDynamicRegistry(String name, Supplier<Iterable<E>> registry, boolean noNamespace) {
        super(name);
        this.registry = registry;
        this.noNamespace = noNamespace;
    }

    protected void rebuildIfNeeded() {
        var actual = registry.get();
        if (last != actual) {
            last = actual;
            lookup.clear();
            build(last, noNamespace);
        }
    }

    @Override
    public void parse(C ctx, CommandReader reader, ArgumentMap out) throws CommandMsgError {
        rebuildIfNeeded();
        super.parse(ctx, reader, out);
    }

    @Override
    public void suggest(C ctx, CommandReader reader, SuggestionsList suggestions, ArgumentMap args) throws CommandMsgError {
        rebuildIfNeeded();
        super.suggest(ctx, reader, suggestions, args);
    }

    @Override
    public boolean compilable() {
        return false;
    }

}
