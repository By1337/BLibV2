package dev.by1337.edsl.command.args;

import dev.by1337.cmd.*;
import dev.by1337.edsl.context.EventContext;
import dev.by1337.plc.PlaceholderApplier;
import dev.by1337.core.util.text.minimessage.BMM;
import dev.by1337.edsl.text.RawTextComponentLike;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

public final class ArgumentRawTitle<C> extends Argument<C, ArgumentRawTitle.Text> {
    public ArgumentRawTitle(String name) { super(name); }

    @Override
    public void parse(C ctx, CommandReader reader, ArgumentMap out) {
        String text = reader.readString();
        if (!text.isEmpty()) out.put(name, new Text(text));
    }

    @Override
    public void suggest(C ctx, CommandReader reader, SuggestionsList suggestions, ArgumentMap args) {
        reader.readString();
    }

    @Override
    public boolean compilable() { return true; }

    @Override
    public boolean allowAsync() { return true; }

    public static final class Text {
        private final RawTextComponentLike text;
        private volatile Parsed cached;

        private Text(String source) {
            text = new RawTextComponentLike(source);
        }

        public Title render(Player player, EventContext context, Title.Times times) {
            return render(player, (PlaceholderApplier) context, times);
        }

        public Title render(Player player, PlaceholderApplier context, Title.Times times) {
            var formatted = text.asFormated(player);
            Parsed parts;
            if (formatted.component() == null) {
                parts = parse(formatted, context.setPlaceholders(formatted.s()));
            } else {
                parts = cached;
                if (parts == null || parts.formatted != formatted) cached = parts = parse(formatted, formatted.s());
            }
            return Title.title(parts.title, parts.subtitle, times);
        }

        private static Parsed parse(RawTextComponentLike.Pair formatted, String source) {
            String[] parts = source.split("\\\\n", 2);
            return new Parsed(formatted, BMM.deserialize(parts[0]),
                    parts.length == 2 ? BMM.deserialize(parts[1]) : Component.empty());
        }

        private record Parsed(RawTextComponentLike.Pair formatted, Component title, Component subtitle) {}
    }
}
