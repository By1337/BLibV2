package dev.by1337.core.command.bcmd.argument;

import dev.by1337.cmd.*;
import dev.by1337.core.command.bcmd.argument.util.NamespacedKeyTrie;
import org.bukkit.Registry;
import org.bukkit.Sound;

import java.util.List;
import java.util.Locale;

public class ArgumentSound<C> extends Argument<C, Sound> {
    private static final NamespacedKeyTrie<Sound> LOOKUP = new NamespacedKeyTrie<>();
    private static final NamespacedKeyTrie<Sound> LOOKUP_ALTS = new NamespacedKeyTrie<>();

    public ArgumentSound(String name) {
        super(name);
    }

    @Override
    public void parse(C ctx, CommandReader reader, ArgumentMap out) throws CommandMsgError {
        String str = reader.readString();
        if (str.isEmpty()) {
            return;
        }
        String input = str.toLowerCase(Locale.ENGLISH);
        Sound value = lookup(input);
        if (value == null) {
            List<String> suggestions = LOOKUP.getWordsWithPrefix("", 5);
            throw new CommandMsgError("Unknown " + name + ": " + str + ", did you mean: " + String.join(", ", suggestions));
        }
        out.put(name, value);
    }

    private static Sound lookup(String input) {
        Sound s = LOOKUP.search(input);
        if (s != null) return s;
        s = LOOKUP_ALTS.search(input);
        if (s != null) return s;
        return LOOKUP_ALTS.search(
                input.replace(".", "")
                        .replace("_", "")
        );
    }

    @Override
    public void suggest(C ctx, CommandReader reader, SuggestionsList suggestions, ArgumentMap args) throws CommandMsgError {
        String str = reader.readString();
        LOOKUP.getWordsWithPrefix(str, 15).forEach(suggestions::suggest);
    }

    static {
        Registry.SOUNDS.iterator().forEachRemaining(sound -> {
            var key = sound.getKey();
            LOOKUP.insert(key.getKey(), sound);

            LOOKUP_ALTS.insert(key.toString(), sound);
            LOOKUP_ALTS.insert(key.getKey()
                            .replace(".", "")
                            .replace("_", "")
                    , sound);
        });
    }
}
