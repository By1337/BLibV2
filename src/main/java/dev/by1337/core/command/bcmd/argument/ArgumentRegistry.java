package dev.by1337.core.command.bcmd.argument;

import dev.by1337.cmd.*;
import dev.by1337.core.command.bcmd.argument.util.NamespacedKeyTrie;
import org.bukkit.Keyed;

import java.util.List;
import java.util.Locale;

public class ArgumentRegistry<C, E extends Keyed> extends Argument<C, E> {

    protected final NamespacedKeyTrie<E> lookup = new NamespacedKeyTrie<>();

    protected ArgumentRegistry(String name) {
        super(name);
    }

    public ArgumentRegistry(String name, Iterable<E> registry) {
        this(name, registry, false);
    }

    public ArgumentRegistry(String name, Iterable<E> registry, boolean noNamespace) {
        super(name);
        build(registry, noNamespace);
    }

    protected void build(Iterable<E> registry, boolean noNamespace) {
        registry.iterator().forEachRemaining(key -> {
            lookup.insert(key.getKey().getKey(), key);
            if (noNamespace) return;
            lookup.insert(key.getKey().toString(), key);
        });
    }

    @Override
    public void parse(C ctx, CommandReader reader, ArgumentMap out) throws CommandMsgError {
        String str = reader.readString();
        if (str.isEmpty()) {
            return;
        }
        String input = str.toLowerCase(Locale.ENGLISH);
        E value = lookup.search(input);
        if (value == null) {
            List<String> suggestions = lookup.getWordsWithPrefix("", 5);
            throw new CommandMsgError("Unknown " + name + ": " + str + ", did you mean: " + String.join(", ", suggestions));
        }
        out.put(name, value);
    }

    @Override
    public void suggest(C ctx, CommandReader reader, SuggestionsList suggestions, ArgumentMap args) throws CommandMsgError {
        String str = reader.readString();
        //todo триграммы?
        lookup.getWordsWithPrefix(str, 15).forEach(suggestions::suggest);
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
