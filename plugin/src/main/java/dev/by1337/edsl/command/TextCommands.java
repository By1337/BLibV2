package dev.by1337.edsl.command;

import dev.by1337.cmd.Command;
import dev.by1337.cmd.CommandMsgError;
import dev.by1337.cmd.CommandReader;
import dev.by1337.cmd.argument.ArgumentString;
import dev.by1337.core.util.text.AdventureAdapter;
import dev.by1337.edsl.Themes;
import dev.by1337.edsl.EventHandler;
import dev.by1337.edsl.command.args.ArgumentRawText;
import dev.by1337.edsl.command.args.ArgumentRawTitle;
import dev.by1337.edsl.context.EventContext;
import dev.by1337.edsl.text.RawTextComponentLike;
import dev.by1337.edsl.util.ByLocaleSelector;
import dev.by1337.edsl.util.Locales;
import dev.by1337.plc.PlaceholderApplier;
import dev.by1337.yaml.YamlValue;
import dev.by1337.yaml.codec.DataResult;
import dev.by1337.yaml.decoder.DecodeContext;
import dev.by1337.yaml.decoder.YamlDecoder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.Ticks;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/** Shared text commands; hosts supply access to their own execution context. */
public final class TextCommands<C> {
    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("message", "message"), Map.entry("messages", "message"),
            Map.entry("broadcast", "broadcast"), Map.entry("bc", "broadcast"), Map.entry("message_all", "broadcast"),
            Map.entry("action_bar", "action_bar"), Map.entry("action_bar_all", "action_bar_all"),
            Map.entry("title", "title"), Map.entry("title_all", "title_all"));
    private final Function<C, Player> target;
    private final Function<C, PlaceholderApplier> placeholders;
    private final BiConsumer<C, Consumer<Player>> broadcast;

    private TextCommands(Function<C, Player> target, Function<C, PlaceholderApplier> placeholders,
        BiConsumer<C, Consumer<Player>> broadcast) {
        this.target = target;
        this.placeholders = placeholders;
        this.broadcast = broadcast;
    }

    public static String canonical(String name) {
        return ALIASES.get(name.toLowerCase(Locale.ROOT));
    }

    public static boolean defersPlaceholders(String source) {
        int end = source.indexOf(']');
        return source.startsWith("[") && end > 0 && canonical(source.substring(1, end)) != null;
    }

    public static <C> Command<C> create(Function<C, Player> target, Function<C, PlaceholderApplier> placeholders,
        BiConsumer<C, Consumer<Player>> broadcast) {
        var text = new TextCommands<>(target, placeholders, broadcast);
        var root = new Command<C>("root");
        for (String name : new String[]{ "message", "broadcast", "action_bar", "action_bar_all", "title", "title_all" }) {
            root.sub(text.command(name));
        }
        return root;
    }

    private Command<C> command(String channel) {
        var command = new Command<C>("[" + channel + "]");
        ALIASES.forEach((alias, value) -> {
            if (value.equals(channel)) {
                command.alias("[" + alias + "]");
                command.alias("[" + alias.toUpperCase(Locale.ROOT) + "]");
            }
        });
        if (channel.startsWith("title")) {
            return command.executor(new ArgumentRawTitle<>("text"), new ArgumentString<>("fadeIn"),
                    new ArgumentString<>("stay"), new ArgumentString<>("fadeOut"),
                    (ctx, text, fadeIn, stay, fadeOut) -> visit(ctx, channel,
                            player -> send(channel, new Payload(null, text, fadeIn, stay, fadeOut), player, placeholders.apply(ctx))));
        }
        return command.executor(new ArgumentRawText<>("text"), (ctx, text) -> visit(ctx, channel,
                player -> send(channel, new Payload(text, null, null, null, null), player, placeholders.apply(ctx))));
    }

    private void visit(C ctx, String channel, Consumer<Player> output) {
        if (all(channel)) broadcast.accept(ctx, output);
        else output.accept(target.apply(ctx));
    }

    private static boolean all(String channel) {
        return channel.equals("broadcast") || channel.endsWith("_all");
    }

    public static void send(String channel, Payload payload, Player player, PlaceholderApplier placeholders) {
        if (player == null) throw new CommandMsgError("Text output requires a player");
        if (channel.startsWith("title")) {
            if (payload.title == null) throw new CommandMsgError("Use [title] <\"title\\nsubtitle\"> [fadeIn] [stay] [fadeOut]");
            var times = AdventureAdapter.titleTimes(Ticks.duration(duration(payload.fadeIn, 10, placeholders)),
                    Ticks.duration(duration(payload.stay, 70, placeholders)), Ticks.duration(duration(payload.fadeOut, 20, placeholders)));
            player.showTitle(payload.title.render(player, placeholders, times));
        } else {
            Component text = payload.text == null ? Component.empty() : payload.text.asComponent(player, placeholders);
            if (channel.startsWith("action_bar")) player.sendActionBar(text);
            else player.sendMessage(text);
        }
    }

    public static int duration(String value, int fallback, PlaceholderApplier placeholders) {
        if (value == null) return fallback;
        String resolved = placeholders.setPlaceholders(value);
        try {
            return Integer.parseInt(resolved);
        } catch (NumberFormatException e) {
            throw new CommandMsgError("Expected integer duration: " + resolved);
        }
    }

    /** Locale maps are selected per recipient; an absent player language falls back to Themes.defaultLocale. */
    public static DataResult<EventHandler> decodeLocalized(DecodeContext ctx, String channel, YamlValue yaml) {
        return YamlDecoder.mapOf(YamlDecoder.STRING, YamlDecoder.MULTI_LINE_STRING).decode(ctx, yaml).flatMap(map -> {
            if (map.isEmpty()) return DataResult.error("Empty locale map for " + channel);
            var variants = new ByLocaleSelector<Payload>((Payload) null);
            try {
                for (var entry : map.entrySet()) variants.add(Locales.parse(entry.getKey()), Payload.parse(channel, entry.getValue()));
            } catch (IllegalArgumentException e) {
                return DataResult.error("Invalid localized " + channel + ": " + e.getMessage());
            }
            return DataResult.success(event -> {
                Consumer<Player> output = player -> {
                    if (player == null) return;
                    Payload payload = variants.select(player.locale());
                    if (payload == null) payload = variants.select(Themes.defaultLocale);
                    if (payload != null) {
                        try {
                            send(channel, payload, player, event);
                        } catch (Exception e) {
                            event.getLogger().error("Failed to send localized " + channel, e);
                        }
                    }
                };
                if (all(channel)) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        try (var ignored = event.target(player)) {
                            output.accept(player);
                        }
                    }
                } else output.accept(event.target());
                return true;
            });
        });
    }

    public record Payload(RawTextComponentLike text, ArgumentRawTitle.Text title, String fadeIn, String stay, String fadeOut) {
        public static Payload parse(String channel, String value) {
            if (!channel.startsWith("title")) {
                return new Payload(new RawTextComponentLike(value.replace("\n", "<br><reset>")), null, null, null, null);
            }
            if (!value.startsWith("\"") && !value.startsWith("'")) {
                String[] lines = value.replace("\r\n", "\n").stripTrailing().split("\n", 3);
                String text = lines[0] + (lines.length > 1 ? "\\n" + lines[1] : "");
                if (lines.length < 3) return new Payload(null, ArgumentRawTitle.Text.of(text), null, null, null);
                String[] timings = lines[2].strip().split("\\s+");
                if (timings.length != 3)
                    throw new IllegalArgumentException("Expected fadeIn stay fadeOut after title and subtitle");
                return new Payload(null, ArgumentRawTitle.Text.of(text), timings[0], timings[1], timings[2]);
            }
            var reader = new CommandReader(value);
            String text = reader.readString();
            if (text.isEmpty()) throw new IllegalArgumentException("Missing title text");
            var result = new Payload(null, ArgumentRawTitle.Text.of(text), next(reader), next(reader), next(reader));
            if (next(reader) != null) throw new IllegalArgumentException("Too many title arguments");
            return result;
        }

        private static String next(CommandReader reader) {
            while (reader.hasNext() && Character.isWhitespace(reader.peek())) reader.next();
            return reader.hasNext() ? reader.readString() : null;
        }
    }
}
