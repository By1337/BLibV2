package dev.by1337.core.command.bcmd;

import dev.by1337.edsl.MessageManager;
import dev.by1337.core.util.io.ResourceUtil;
import dev.by1337.yaml.YamlValue;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/** Shared argument errors owned by BLib, including commands registered by dependent plugins. */
public final class CommandMessages {
    private static MessageManager messages;

    private CommandMessages() {
    }

    public static void load(Plugin plugin) {
        ResourceUtil.saveIfNotExist("messages.yml", plugin);
        var result = MessageManager.decoder("messages.yml").decode(YamlValue.EMPTY, plugin);
        if (result.hasError() || result.result() == null) {
            throw new IllegalStateException("Failed to load command messages: " + result.error());
        }
        messages = result.result();
    }

    public static void send(CommandSender sender, ArgumentParseError error) {
        if (sender instanceof Player player && messages != null) {
            messages.call(error.messageKey(), player, error.placeholders());
        } else {
            sender.sendMessage(error.getMessage());
        }
    }
}
