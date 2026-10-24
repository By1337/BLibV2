package dev.by1337.edsl.command;

import dev.by1337.cmd.Command;
import dev.by1337.core.util.text.AdventureAdapter;
import dev.by1337.cmd.CommandMsgError;
import dev.by1337.cmd.argument.ArgumentString;
import dev.by1337.cmd.argument.ArgumentStrings;
import dev.by1337.core.bridge.registry.LegacyRegistryBridge;
import dev.by1337.core.command.bcmd.argument.*;
import dev.by1337.edsl.context.EventContext;
import dev.by1337.edsl.command.args.ArgumentRawText;
import dev.by1337.edsl.command.args.ArgumentRawTitle;
import dev.by1337.edsl.text.RawTextComponentLike;

import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.util.Ticks;
import org.bukkit.Bukkit;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DefaultEventCommands {
    private static final Set<String> DEFERRED_TEXT_COMMANDS = Set.of(
            "[message]", "[messages]", "[broadcast]", "[bc]", "[message_all]",
            "[action_bar]", "[action_bar_all]", "[title]", "[title_all]");

    /** Only these built-in commands resolve their own placeholders at execution time. */
    public static boolean defersPlaceholders(String source) {
        int end = source.indexOf(' ');
        return DEFERRED_TEXT_COMMANDS.contains(end < 0 ? source : source.substring(0, end))
                || LocalizedTextCommands.defersPlaceholders(source);
    }


    public static <T extends EventContext> Command<T> createCommands() {
        return new Command<T>("root")
                .and(LocalizedTextCommands.<T>create(
                        EventContext::target,
                        ctx -> ctx,
                        (ctx, output) -> {
                            for (Player player : Bukkit.getOnlinePlayers()) {
                                try (var ignored = ctx.target(player)) {
                                    output.accept(player);
                                }
                            }
                        }))
                .sub(new Command<T>("[console]").executor(
                        new ArgumentStrings<>("cmd"),
                        (ctx, cmd) -> {
                            reqNotNull(cmd, "has no command");
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                        }))
                .sub(new Command<T>("[ref]").executor(
                        new ArgumentStrings<>("key"),
                        (ctx, key) -> {
                            reqNotNull(key, "has no key");
                            ctx.messages().handle(key, ctx);
                        }))

                .sub(new Command<T>("[ref_all]").executor(
                        new ArgumentStrings<>("key"),
                        (ctx, key) -> {
                            reqNotNull(key, "has no key");
                            for (Player player : Bukkit.getOnlinePlayers()) {
                                try (var ignored = ctx.target(player)){
                                    ctx.messages().handle(key, ctx);
                                }
                            }
                        }))
                .sub(new Command<T>("[broadcast]").aliases("[bc]", "[message_all]").executor(
                        new ArgumentRawText<>("msg"),
                        (ctx, msg) -> {
                            for (Player player : Bukkit.getOnlinePlayers()) {
                                try (var ignored = ctx.target(player)) {
                                    player.sendMessage(render(msg, player, ctx));
                                }
                            }
                        }))

                .sub(new Command<T>("[message]").aliases("[messages]").executor(
                        new ArgumentRawText<>("msg"),
                        (ctx, msg) -> {
                            var player = ctx.target();
                            reqNotNull(player, "only with player!");
                            player.sendMessage(render(msg, player, ctx));
                        }))
                .sub(new Command<T>("[chat]").executor(
                        new ArgumentStrings<>("msg"),
                        (ctx, msg) -> {

                            reqNotNull(msg, "Use: [chat] <msg>");
                            var player = ctx.target();
                            reqNotNull(player, "only with player!");
                            player.chat(msg);
                        }))

                .sub(new Command<T>("[player]").executor(
                        new ArgumentStrings<>("msg"),
                        (ctx, msg) -> {
                            reqNotNull(msg, "Use: [player] <msg>");
                            var player = ctx.target();
                            reqNotNull(player, "only with player!");
                            player.performCommand(msg);
                        }))
                .sub(new Command<T>("[sound]").executor(
                        new ArgumentSound<>("sound"),
                        new ArgumentDouble<>("volume"),
                        new ArgumentDouble<>("pitch"),
                        (ctx, sound, volume, pitch) -> {
                            reqNotNull(sound, "Use: [sound] <sound> <?volume> <pitch>");
                            var player = ctx.target();
                            reqNotNull(player, "only with player!");
                            player.playSound(player.getLocation(), sound,
                                    orElseGet(volume, 1D).floatValue(),
                                    orElseGet(pitch, 1D).floatValue()
                            );
                        }))
                .sub(new Command<T>("[sound_all]").executor(
                        new ArgumentSound<>("sound"),
                        new ArgumentDouble<>("volume"),
                        new ArgumentDouble<>("pitch"),
                        (ctx, sound, volume, pitch) -> {
                            reqNotNull(sound, "Use: [sound_all] <sound> <?volume> <pitch>");
                            float v = orElseGet(volume, 1D).floatValue();
                            float p = orElseGet(pitch, 1D).floatValue();
                            Bukkit.getOnlinePlayers().forEach(pl -> pl.playSound(
                                    pl.getLocation(), sound, v, p
                            ));
                        }))
                .sub(new Command<T>("[action_bar]").executor(
                        new ArgumentRawText<>("msg"),
                        (ctx, msg) -> {
                            var player = ctx.target();
                            reqNotNull(player, "only with player!");
                            player.sendActionBar(render(msg, player, ctx));
                        }))
                .sub(new Command<T>("[action_bar_all]").executor(
                        new ArgumentRawText<>("msg"),
                        (ctx, msg) -> {
                            for (Player player : Bukkit.getOnlinePlayers()) {
                                try (var ignored = ctx.target(player)) {
                                    player.sendActionBar(render(msg, player, ctx));
                                }
                            }
                        }))
                .sub(new Command<T>("[give_effect]").executor(
                        new ArgumentRegistry<>("effect", LegacyRegistryBridge.MOB_EFFECT),
                        new ArgumentInt<>("duration"),
                        new ArgumentInt<>("amplifier"),
                        (ctx, effect, duration, amplifier) -> {

                            reqNotNull(effect, "use [give_effect] <effect> <duration> <amplifier>");
                            reqNotNull(duration, "use [give_effect] <effect> <duration> <amplifier>");
                            reqNotNull(amplifier, "use [give_effect] <effect> <duration> <amplifier>");
                            var player = ctx.target();
                            reqNotNull(player, "only with player!");
                            player.addPotionEffect(new PotionEffect(effect.value(), duration, amplifier));
                        }))
                .sub(new Command<T>("[title]").executor(
                        new ArgumentRawTitle<>("msg"),
                        new ArgumentString<>("fadeIn"),
                        new ArgumentString<>("stay"),
                        new ArgumentString<>("fadeOut"),
                        (ctx, msg, fadeIn0, stay0, fadeOut0) -> {

                            reqNotNull(msg, "Use [title] <\"Title\\nSubTitle\"> <?fadeIn> <?stay> <?fadeOut>");
                            int fadeIn = duration(fadeIn0, 10, ctx);
                            int stay = duration(stay0, 70, ctx);
                            int fadeOut = duration(fadeOut0, 20, ctx);
                            var player = ctx.target();
                            reqNotNull(player, "only with player!");
                            player.showTitle(msg.render(player, ctx, AdventureAdapter.titleTimes(
                                    Ticks.duration(fadeIn), Ticks.duration(stay), Ticks.duration(fadeOut))));
                        }
                ))
                .sub(new Command<T>("[title_all]").executor(
                        new ArgumentRawTitle<>("msg"),
                        new ArgumentString<>("fadeIn"),
                        new ArgumentString<>("stay"),
                        new ArgumentString<>("fadeOut"),
                        (ctx, msg, fadeIn0, stay0, fadeOut0) -> {

                            reqNotNull(msg, "Use [title_all] <\"Title\\nSubTitle\"> <?fadeIn> <?stay> <?fadeOut>");
                            int fadeIn = duration(fadeIn0, 10, ctx);
                            int stay = duration(stay0, 70, ctx);
                            int fadeOut = duration(fadeOut0, 20, ctx);
                            Title.Times times = AdventureAdapter.titleTimes(
                                    Ticks.duration(fadeIn), Ticks.duration(stay), Ticks.duration(fadeOut));
                            for (Player player : Bukkit.getOnlinePlayers()) {
                                try (var ignored = ctx.target(player)) {
                                    player.showTitle(msg.render(player, ctx, times));
                                }
                            }
                        }
                ))
                .sub(new Command<T>("[cooldown]").executor(
                        new ArgumentRegistry<>("material", Registry.MATERIAL),
                        new ArgumentInt<>("ticks"),
                        (ctx, material, ticks) -> {

                            reqNotNull(material, "Use: [cooldown] <material> <ticks>");
                            reqNotNull(ticks, "Use: [cooldown] <material> <ticks>");
                            var player = ctx.target();
                            reqNotNull(player, "only with player!");
                            player.setCooldown(material, ticks);
                        })
                )
                ;
    }

    private static Component render(@Nullable RawTextComponentLike text, Player player, EventContext ctx) {
        return text == null ? Component.empty() : text.asComponent(player, ctx);
    }

    private static int duration(@Nullable String value, int fallback, EventContext ctx) {
        if (value == null) return fallback;
        String resolved = ctx.setPlaceholders(value);
        try {
            return Integer.parseInt(resolved);
        } catch (NumberFormatException e) {
            throw new CommandMsgError("Expected integer duration: " + resolved);
        }
    }

    private static <T> T orElseGet(T t, T def) {
        return t != null ? t : def;
    }

    @Contract("null, _ -> fail")
    private static void reqNotNull(Object o, String msg) {
        if (o == null) throw new CommandMsgError(msg);
    }
}
