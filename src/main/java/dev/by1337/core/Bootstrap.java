package dev.by1337.core;

import dev.by1337.core.util.RepositoryUtil;
import dev.by1337.core.util.reflect.ClasspathUtil;
import dev.by1337.core.util.text.minimessage.MiniMessage;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;

@ApiStatus.Internal
class Bootstrap {

    public static void bootstrap(Plugin plugin) {
        Path libraries = plugin.getDataFolder().toPath().resolve(".libraries");
        libraries.toFile().mkdirs();
        if (!hasClass("org.objectweb.asm.tree.ClassNode")) {
            ClasspathUtil.addUrl(plugin, RepositoryUtil.download("org.ow2.asm:asm:9.9.1", libraries));
            ClasspathUtil.addUrl(plugin, RepositoryUtil.download("org.ow2.asm:asm-tree:9.9.1", libraries));
            ClasspathUtil.addUrl(plugin, RepositoryUtil.download("org.ow2.asm:asm-commons:9.9.1", libraries));
        }
        if (!hasClass("org.joml.Quaternionf")){
            ClasspathUtil.addUrl(plugin, RepositoryUtil.download("org.joml:joml:1.10.8", libraries));
        }
        try {
            MethodHandles.lookup().ensureInitialized(MiniMessage.class);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        //  ClasspathUtil.addUrl(plugin, RepositoryUtil.download(RepositoryUtil.BDEV_REPO,"dev.by1337.fparticle:bukkit:1.5", libraries));
        //  ClasspathUtil.addUrl(plugin, RepositoryUtil.download(RepositoryUtil.BDEV_REPO,"dev.by1337.yaml:byaml-bukkit:1.1", libraries));
        loadNMSBridges(plugin);
        try {
            Class<?> boot = Class.forName("dev.by1337.core.BridgeBootstrapper");
            boot.getMethod("bootstrap").invoke(null);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load nms bridge", ex);
        }
    }

    private static void loadNMSBridges(Plugin plugin) {
        File outFolder = new File(plugin.getDataFolder(), ".bridges");
        outFolder.mkdirs();
        String bridgeName = "bridge-" + plugin.getDescription().getVersion() + "+" + ServerVersion.CURRENT_ID + ".jar";
        File file = new File(outFolder, bridgeName);
        if (!file.exists()) {
            try (var in = getInputStream("bridges/" + bridgeName)) {
                if (in == null) {
                    throw new FileNotFoundException("Unable to find bridges/" + bridgeName);
                }
                try (var out = new FileOutputStream(file)) {
                    in.transferTo(out);
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to load nms bridge " + bridgeName, e);
            }
        }
        ClasspathUtil.addUrl(plugin, file.toPath());
    }


    private static boolean hasClass(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Nullable
    private static InputStream getInputStream(String s) {
        ClassLoader loader = RepositoryUtil.class.getClassLoader();
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
