package dev.by1337.core.util.io;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import dev.by1337.yaml.YamlMap;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class ResourceUtil {

    @NotNull
    @CanIgnoreReturnValue
    public static File saveIfNotExist(@NotNull String path, @NotNull Plugin plugin) {
        path = path.replace('\\', '/');
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        var f = new File(plugin.getDataFolder(), path);
        if (!f.exists()) {
            plugin.saveResource(path, false);
        }
        return f;
    }

    public static YamlMap load(@NotNull String path, @NotNull Plugin plugin) {
        return YamlMap.load(saveIfNotExist(path, plugin));
    }
}
