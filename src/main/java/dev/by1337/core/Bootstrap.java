package dev.by1337.core;

import dev.by1337.bpatch.PatchArchive;
import dev.by1337.core.util.RepositoryUtil;
import dev.by1337.core.util.network.ChannelGetter;
import dev.by1337.core.util.reflect.ClasspathUtil;
import dev.by1337.core.util.text.minimessage.MiniMessage;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;

@ApiStatus.Internal
class Bootstrap {

    private static final Logger log = LoggerFactory.getLogger("BLib");

    public static void bootstrap(Plugin plugin, File jar) {
        Path libraries = plugin.getDataFolder().toPath().resolve(".libraries");
        libraries.toFile().mkdirs();
        if (!hasClass("org.objectweb.asm.tree.ClassNode")) {
            ClasspathUtil.addUrl(plugin, RepositoryUtil.download("org.ow2.asm:asm:9.9.1", libraries));
            ClasspathUtil.addUrl(plugin, RepositoryUtil.download("org.ow2.asm:asm-tree:9.9.1", libraries));
            ClasspathUtil.addUrl(plugin, RepositoryUtil.download("org.ow2.asm:asm-commons:9.9.1", libraries));
        }
        if (!hasClass("org.joml.Quaternionf")) {
            ClasspathUtil.addUrl(plugin, RepositoryUtil.download("org.joml:joml:1.10.8", libraries));
        }
        if (!hasClass("com.github.benmanes.caffeine.cache.Caffeine")) {
            ClasspathUtil.addUrl(plugin, RepositoryUtil.download("com.github.ben-manes.caffeine:caffeine:3.2.4", libraries));
        }
        try {
            var lookup = MethodHandles.lookup();
            lookup.ensureInitialized(MiniMessage.class);
            lookup.ensureInitialized(ChannelGetter.class);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        try {
            File bridges = new File(plugin.getDataFolder(), ".bridges");
            File version = new File(plugin.getDataFolder(), ".version");
            long time = Files.getLastModifiedTime(jar.toPath()).toMillis();
            if (!version.exists() || time != readLong(version)) {
                writeLong(version, time);
                deleteFolder(bridges);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        loadNMSBridge(plugin);
        try {
            Class<?> boot = Class.forName("dev.by1337.core.BridgeBootstrapper");
            boot.getMethod("bootstrap").invoke(null);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load nms bridge", ex);
        }
    }

    private static long readLong(File file) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            return raf.readLong();
        }
    }

    private static void writeLong(File file, long l) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.setLength(8);
            raf.writeLong(l);
        }
    }

    private static void deleteFolder(File file) {
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                deleteFolder(f);
            }
            file.delete();
        } else {
            file.delete();
        }
    }

    private static void loadNMSBridge(Plugin plugin) {
        try {
            loadNMSBridge(plugin, ServerVersion.CURRENT_ID);
        } catch (IOException e) {
            if (ServerVersion.is1_20_6orNewer()) {
                log.warn("Nms bridge for {} not found! Attempting to load for {}", ServerVersion.CURRENT_ID, ServerVersion.LastKnown.ID);
                try {
                    loadNMSBridge(plugin, ServerVersion.LastKnown.ID);
                } catch (IOException ignored) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    private static void loadNMSBridge(Plugin plugin, String versionId) throws IOException {
        Path file = unpackNMSBridge(plugin.getDataFolder().toPath().resolve(".bridges"), versionId);
        ClasspathUtil.addUrl(plugin, file);
    }

    static Path unpackNMSBridge(Path outFolder, String versionId) throws IOException {
        Path file = outFolder.resolve("bridge+" + versionId + ".jar");
        if (!Files.isRegularFile(file)) {
            try (var in = getInputStream("bridges/nms.zip")) {
                if (in == null) {
                    throw new FileNotFoundException("Unable to find bridges/nms.zip");
                }
                try (PatchArchive archive = PatchArchive.open(in)) {
                    archive.unpack(versionId, file);
                }
            } catch (IOException e) {
                throw new IOException("Failed to unpack nms bridge " + versionId, e);
            }
        }
        return file;
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
