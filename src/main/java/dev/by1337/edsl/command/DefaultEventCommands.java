package dev.by1337.edsl.command;

import dev.by1337.cmd.Command;
import dev.by1337.cmd.CommandMsgError;
import dev.by1337.cmd.argument.ArgumentString;
import dev.by1337.cmd.argument.ArgumentStrings;
import dev.by1337.core.bridge.registry.LegacyRegistryBridge;
import dev.by1337.core.command.bcmd.argument.*;
import dev.by1337.core.util.text.minimessage.MiniMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.util.Ticks;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

public class DefaultEventCommands {


    public static Command<Player> createPlayerCommands() {
        return new Command<Player>("root")
                .sub(new Command<Player>("[message]").executor(
                        new ArgumentComponents<>("msg"),
                        (player, msg) -> {
                            reqNotNull(player, "only with player!");
                            player.sendMessage(orElseGet(msg, Component.empty()));
                        }))
                .sub(new Command<Player>("[chat]").executor(
                        new ArgumentStrings<>("msg"),
                        (player, msg) -> {
                            reqNotNull(player, "only with player!");
                            reqNotNull(msg, "Use: [chat] <msg>");
                            player.chat(msg);
                        }))

                .sub(new Command<Player>("[player]").executor(
                        new ArgumentStrings<>("msg"),
                        (player, msg) -> {
                            reqNotNull(player, "only with player!");
                            reqNotNull(msg, "Use: [player] <msg>");
                            player.performCommand(msg);
                        }))
                .sub(new Command<Player>("[sound]").executor(
                        new ArgumentSound<>("sound"),
                        new ArgumentDouble<>("volume"),
                        new ArgumentDouble<>("pitch"),
                        (player, sound, volume, pitch) -> {
                            reqNotNull(player, "only with player!");
                            reqNotNull(sound, "Use: [sound] <sound> <?volume> <pitch>");
                            player.playSound(player.getLocation(), sound,
                                    orElseGet(volume, 1D).floatValue(),
                                    orElseGet(pitch, 1D).floatValue()
                            );
                        }))
                .sub(new Command<Player>("[action_bar]").executor(
                        new ArgumentComponents<>("msg"),
                        (player, msg) -> {
                            reqNotNull(player, "only with player!");
                            player.sendActionBar(orElseGet(msg, Component.empty()));
                        }))

                .sub(new Command<Player>("[give_effect]").executor(
                        new ArgumentRegistry<>("effect", LegacyRegistryBridge.MOB_EFFECT),
                        new ArgumentInt<>("duration"),
                        new ArgumentInt<>("amplifier"),
                        (player, effect, duration, amplifier) -> {
                            reqNotNull(player, "only with player!");
                            reqNotNull(effect, "use [give_effect] <effect> <duration> <amplifier>");
                            reqNotNull(duration, "use [give_effect] <effect> <duration> <amplifier>");
                            reqNotNull(amplifier, "use [give_effect] <effect> <duration> <amplifier>");
                            player.addPotionEffect(new PotionEffect(effect.value(), duration, amplifier));
                        }))
                .sub(new Command<Player>("[title]").executor(
                        new ArgumentString<>("msg"),
                        new ArgumentInt<>("fadeIn"),
                        new ArgumentInt<>("stay"),
                        new ArgumentInt<>("fadeOut"),
                        (player, msg, fadeIn0, stay0, fadeOut0) -> {
                            reqNotNull(player, "only with player!");
                            reqNotNull(msg, "Use [title] <\"Title\\nSubTitle\"> <?fadeIn> <?stay> <?fadeOut>");
                            int fadeIn = orElseGet(fadeIn0, 10);
                            int stay = orElseGet(stay0, 70);
                            int fadeOut = orElseGet(fadeOut0, 20);
                            String[] arr = msg.split("\\\\n", 2);
                            player.showTitle(Title.title(
                                    MiniMessage.deserialize(arr[0]),
                                    arr.length == 2 ? MiniMessage.deserialize(arr[1]) : Component.empty(),
                                    Title.Times.of(
                                            Ticks.duration(fadeIn),
                                            Ticks.duration(stay),
                                            Ticks.duration(fadeOut)
                                    )
                            ));
                        }
                ))
                .sub(new Command<Player>("[cooldown]").executor(
                        new ArgumentRegistry<>("material", Registry.MATERIAL),
                        new ArgumentInt<>("ticks"),
                        (player, material, ticks) -> {
                            reqNotNull(player, "only with player!");
                            reqNotNull(material, "Use: [cooldown] <material> <ticks>");
                            reqNotNull(ticks, "Use: [cooldown] <material> <ticks>");
                            player.setCooldown(material, ticks);
                        })
                )
                ;
    }
    //                .sub(new Command<Player>("[sound]")
    //                        .aliases("[SOUND]")
    //                        .argument(new ArgumentSound<>("sound"))
    //                        .argument(new ArgumentDouble<>("volume"))
    //                        .argument(new ArgumentDouble<>("pitch"))
    //                        .executor((v, args) -> {
    //                                    if (v == null) throw new CommandMsgError("only with player");
    //                                    float volume = ((Number) args.getOrDefault("volume", 1F)).floatValue();
    //                                    float pitch = ((Number) args.getOrDefault("pitch", 1F)).floatValue();
    //                                    Sound sound = (Sound) args.getOrThrow("sound", "use [sound] <sound> <?volume> <?pitch>");
    //                                    v.playSound(v.getLocation(), sound, volume, pitch);
    //                                }
    //                        )
    //                )

    private static <T> T orElseGet(T t, T def) {
        return t != null ? t : def;
    }

    private static void reqNotNull(Object o, String msg) {
        if (o == null) throw new CommandMsgError(msg);
    }
}
