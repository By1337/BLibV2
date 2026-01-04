package dev.by1337.core;

import dev.by1337.core.util.text.minimessage.MiniMessage;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.ApiStatus;

import java.nio.file.Path;

@ApiStatus.Internal
public class BDev extends JavaPlugin {
    public static Path HOME_DIR;

    @Override
    public void onLoad() {
        getDataFolder().mkdirs();
        HOME_DIR = getDataFolder().toPath();
        Bootstrap.bootstrap(this);
    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}
