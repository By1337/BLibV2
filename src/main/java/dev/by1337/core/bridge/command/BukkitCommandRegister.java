package dev.by1337.core.bridge.command;

import org.bukkit.command.defaults.BukkitCommand;

public interface BukkitCommandRegister {
    void register(BukkitCommand bukkitCommand);

    void unregister(BukkitCommand bukkitCommand);

}
