package dev.by1337.core;

import dev.by1337.cmd.Command;
import dev.by1337.core.command.bcmd.CommandWrapper;
import dev.by1337.core.command.bcmd.TestCommand;
import dev.by1337.core.command.bcmd.requires.RequiresPermission;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.ApiStatus;

import java.nio.file.Path;

@ApiStatus.Internal
public class BDev extends JavaPlugin {
    public static Path HOME_DIR;
    private CommandWrapper commands;

    @Override
    public void onLoad() {
        getDataFolder().mkdirs();
        HOME_DIR = getDataFolder().toPath();
        Bootstrap.bootstrap(this);
    }

    @Override
    public void onEnable() {
        commands = new CommandWrapper(create(), this);
        commands.setPermission("bdev.use");
        commands.register();
    }

    @Override
    public void onDisable() {
        commands.unregister();
    }

    private Command<CommandSender> create() {
        return new Command<CommandSender>("bdev")
                .requires(new RequiresPermission<>("bdev.use"))
                .sub(TestCommand.createTest("commands"))
                ;
    }
}
