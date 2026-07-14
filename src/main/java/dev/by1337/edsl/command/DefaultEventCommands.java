package dev.by1337.edsl.command;

import dev.by1337.cmd.Command;
import dev.by1337.cmd.CommandMsgError;
import dev.by1337.cmd.argument.ArgumentString;
import dev.by1337.cmd.argument.ArgumentStrings;
import dev.by1337.core.bridge.registry.LegacyRegistryBridge;
import dev.by1337.core.command.bcmd.argument.*;
import dev.by1337.core.util.text.minimessage.MiniMessage;
import dev.by1337.edsl.context.EventContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.util.Ticks;
import org.bukkit.Bukkit;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.Contract;

public class DefaultEventCommands {

    public static <T extends EventContext> Command<T> createCommands() {
        return new Command<T>("root")
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
                .sub(new Command<T>("[broadcast]").alias("[bc]").executor(
                        new ArgumentComponents<>("msg"),
                        (ctx, msg) -> {
                            var c = orElseGet(msg, Component.empty());
                            for (Player player : Bukkit.getOnlinePlayers()) {
                                player.sendMessage(c);
                            }
                        }))

                .sub(new Command<T>("[message]").executor(
                        new ArgumentComponents<>("msg"),
                        (ctx, msg) -> {
                            var player = ctx.target();
                            reqNotNull(player, "only with player!");
                            player.sendMessage(orElseGet(msg, Component.empty()));
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
                .sub(new Command<T>("[action_bar]").executor(
                        new ArgumentComponents<>("msg"),
                        (ctx, msg) -> {

                            var player = ctx.target();
                            reqNotNull(player, "only with player!");
                            player.sendActionBar(orElseGet(msg, Component.empty()));
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
                        new ArgumentString<>("msg"),
                        new ArgumentInt<>("fadeIn"),
                        new ArgumentInt<>("stay"),
                        new ArgumentInt<>("fadeOut"),
                        (ctx, msg, fadeIn0, stay0, fadeOut0) -> {

                            reqNotNull(msg, "Use [title] <\"Title\\nSubTitle\"> <?fadeIn> <?stay> <?fadeOut>");
                            int fadeIn = orElseGet(fadeIn0, 10);
                            int stay = orElseGet(stay0, 70);
                            int fadeOut = orElseGet(fadeOut0, 20);
                            String[] arr = msg.split("\\\\n", 2);
                            var player = ctx.target();
                            reqNotNull(player, "only with player!");
                            player.showTitle(Title.title(
                                    MiniMessage.deserialize(arr[0]),
                                    arr.length == 2 ? MiniMessage.deserialize(arr[1]) : Component.empty(),
                                    Title.Times.times(
                                            Ticks.duration(fadeIn),
                                            Ticks.duration(stay),
                                            Ticks.duration(fadeOut)
                                    )
                            ));
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

    private static <T> T orElseGet(T t, T def) {
        return t != null ? t : def;
    }

    @Contract("null, _ -> fail")
    private static void reqNotNull(Object o, String msg) {
        if (o == null) throw new CommandMsgError(msg);
    }
}
