package dev.by1337.core.legacy;

import dev.by1337.core.BDev;
import dev.by1337.core.util.reflect.ClasspathUtil;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

public class BLibBridge {
    private static Class<?> bApiClass;
    private static Object bApi;

    public static void bootstrap(Plugin plugin) {
        loadBridge(plugin);
    }

    public static void load(Plugin plugin) throws Exception {
        try {
            bApiClass = Class.forName("org.by1337.blib.core.BApi");
            bApi = bApiClass.getConstructor(Plugin.class).newInstance(plugin);
        } catch (Exception e) {
            bApiClass = null;
            bApi = null;
            throw e;
        }
    }

    public static void onEnable() {
        if (bApiClass == null) return;
        try {
            bApiClass.getMethod("onEnable").invoke(bApi);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void onDisable() {
        if (bApiClass == null) return;
        try {
            bApiClass.getMethod("onDisable").invoke(bApi);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void loadBridge(Plugin plugin) {
        File outFolder = new File(plugin.getDataFolder(), ".bridges");
        outFolder.mkdirs();
        File file = new File(outFolder, "BLib-bridge.jar");
        if (!file.exists() || BDev.IS_SNAPSHOT) {
            try (var in = getInputStream("bridges/BLib-bridge.jar")) {
                if (in == null) {
                    throw new FileNotFoundException("Unable to find bridges/BLib-bridge.jar");
                }
                try (var out = new FileOutputStream(file)) {
                    in.transferTo(out);
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to load bridge BLib-bridge.jar", e);
            }
        }
        ClasspathUtil.addUrl(plugin, file.toPath(), BDev.IS_SNAPSHOT);
    }

    @Nullable
    private static InputStream getInputStream(String s) {
        ClassLoader loader = BLibBridge.class.getClassLoader();
        URL url = loader.getResource(s);
        if (url == null) {
            return null;
        }
        try {
            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);
            return connection.getInputStream();
        } catch (IOException e) {
            return null;
        }
    }
}
