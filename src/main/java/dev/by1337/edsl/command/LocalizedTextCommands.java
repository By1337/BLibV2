package dev.by1337.edsl.command;

import dev.by1337.cmd.Command;
import dev.by1337.core.util.text.AdventureAdapter;
import dev.by1337.cmd.CommandMsgError;
import dev.by1337.cmd.argument.ArgumentString;
import dev.by1337.edsl.command.args.ArgumentLocale;
import dev.by1337.edsl.command.args.ArgumentRawText;
import dev.by1337.edsl.command.args.ArgumentRawTitle;
import dev.by1337.edsl.util.Locales;
import dev.by1337.plc.PlaceholderApplier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.Ticks;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Shared preprocessing and locale-gated output for EventDSL and BMenu.
 */
public final class LocalizedTextCommands<C> {
    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("message", "message"), Map.entry("messages", "message"),
            Map.entry("broadcast", "broadcast"), Map.entry("bc", "broadcast"), Map.entry("message_all", "broadcast"),
            Map.entry("action_bar", "action_bar"), Map.entry("action_bar_all", "action_bar_all"),
            Map.entry("title", "title"), Map.entry("title_all", "title_all"));
    private final Function<C, Player> target;
    private final Function<C, PlaceholderApplier> placeholders;
    private final BiConsumer<C, Consumer<Player>> broadcast;

    private LocalizedTextCommands(Function<C, Player> target, Function<C, PlaceholderApplier> placeholders,
                                  BiConsumer<C, Consumer<Player>> broadcast) {
        this.target = target;
        this.placeholders = placeholders;
        this.broadcast = broadcast;
    }

    /**
     * Rewrites [messages-ru_RU] text to [message_if_lang] ru_RU text before compilation.
     */
    public static String preprocess(String input) {
        if (!input.startsWith("[")) return input;
        int end = input.indexOf(']');
        if (end < 0) return input;
        String name = input.substring(1, end).toLowerCase(Locale.ROOT);
        if (name.endsWith("_if_lang")) {
            String canonical = ALIASES.get(name.substring(0, name.length() - "_if_lang".length()));
            if (canonical != null) return "[" + canonical + "_if_lang]" + input.substring(end + 1);
        }
        for (var alias : ALIASES.entrySet()) {
            if (name.startsWith(alias.getKey() + "-")) {
                Locale locale = Locales.parse(name.substring(alias.getKey().length() + 1));
                return "[" + alias.getValue() + "_if_lang] " + locale.toLanguageTag() + input.substring(end + 1);
            }
        }
        return input;
    }

    public static boolean defersPlaceholders(String source) {
        int end = source.indexOf(']');
        if (!source.startsWith("[") || end < 0) return false;
        String name = source.substring(1, end).toLowerCase(Locale.ROOT);
        return name.endsWith("_if_lang") && ALIASES.containsKey(name.substring(0, name.length() - "_if_lang".length()));
    }

    public static <C> Command<C> create(Function<C, Player> target, Function<C, PlaceholderApplier> placeholders,
                                        BiConsumer<C, Consumer<Player>> broadcast) {
        var commands = new LocalizedTextCommands<>(target, placeholders, broadcast);
        return new Command<C>("root")
                .sub(commands.text("message", false, false).aliases("[messages_if_lang]"))
                .sub(commands.text("broadcast", true, false).aliases("[bc_if_lang]", "[message_all_if_lang]"))
                .sub(commands.text("action_bar", false, true))
                .sub(commands.text("action_bar_all", true, true))
                .sub(commands.title(false))
                .sub(commands.title(true));
    }

    private Command<C> text(String name, boolean all, boolean actionBar) {
        String command = "[" + name + "_if_lang]";
        return new Command<C>(command).alias(command.toUpperCase(Locale.ROOT)).executor(
                new ArgumentLocale<>("locale"),
                new ArgumentRawText<>("text"),
                (ctx, locale, text) -> {
                    visit(ctx, locale, all, player -> {
                        Component rendered = text == null ? Component.empty() : text.asComponent(player, placeholders.apply(ctx));
                        if (actionBar) player.sendActionBar(rendered);
                        else player.sendMessage(rendered);
                    });
                });
    }

    private Command<C> title(boolean all) {
        String command = all ? "[title_all_if_lang]" : "[title_if_lang]";
        return new Command<C>(command).alias(command.toUpperCase(Locale.ROOT)).executor(
                new ArgumentLocale<>("locale"),
                new ArgumentRawTitle<>("text"),
                new ArgumentString<>("fadeIn"),
                new ArgumentString<>("stay"),
                new ArgumentString<>("fadeOut"),
                (ctx, locale, text, fadeIn, stay, fadeOut) -> {
                    visit(ctx, locale, all, player -> {
                        if (text == null) throw new CommandMsgError("Missing localized title");
                        var applier = placeholders.apply(ctx);
                        var times = AdventureAdapter.titleTimes(
                                Ticks.duration(duration(fadeIn, 10, applier)),
                                Ticks.duration(duration(stay, 70, applier)),
                                Ticks.duration(duration(fadeOut, 20, applier)));
                        player.showTitle(text.render(player, applier, times));
                    });
                });
    }

    private void visit(C ctx, Locale locale, boolean all, Consumer<Player> output) {
        Consumer<Player> matched = player -> {
            if (player != null && locale.equals(player.locale())) output.accept(player);
        };
        if (all) broadcast.accept(ctx, matched);
        else matched.accept(target.apply(ctx));
    }

    private static int duration(String value, int fallback, PlaceholderApplier placeholders) {
        if (value == null) return fallback;
        String resolved = placeholders.setPlaceholders(value);
        try {
            return Integer.parseInt(resolved);
        } catch (NumberFormatException e) {
            throw new CommandMsgError("Expected integer duration: " + resolved);
        }
    }
}
