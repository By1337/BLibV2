package dev.by1337.core;

import dev.by1337.core.util.reflect.ClasspathUtil;
import dev.by1337.yaml.util.Wildcard;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

class ThemesBotter {
    private static final Logger log = LoggerFactory.getLogger(ThemesBotter.class);
    private final Plugin plugin;
    private Class<?> cl;

    ThemesBotter(Plugin plugin) {
        this.plugin = plugin;
        File file = findFile(plugin, "BThemes*.jar");
        if (file == null || !file.exists()) {
            return;
        }
        ClasspathUtil.addUrl(plugin, file.toPath());
        try {
            cl = Class.forName("com.by1337.themes.Booter", false, ThemesBotter.class.getClassLoader());
        } catch (ClassNotFoundException ignored) {
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    public void onLoad() {
        if (cl == null) return;
        try {
            cl.getMethod("onLoad", Plugin.class).invoke(null, plugin);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    public void onEnable() {
        if (cl == null) return;
        try {
            cl.getMethod("onEnable").invoke(null);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    public void onDisable() {
        if (cl == null) return;
        try {
            cl.getMethod("onDisable").invoke(null);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private static File findFile(Plugin plugin, String pattern) {
        for (File file : plugin.getDataFolder().listFiles()) {
            if (Wildcard.matches(file.getName(), pattern)) {
                return file;
            }
        }
        return null;
    }
}
