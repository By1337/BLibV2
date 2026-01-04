package dev.by1337.core.util.reflect;

import dev.by1337.core.ServerVersion;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Unsafe;

import java.io.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

public class ClasspathUtil {
    private static final MethodHandle ADD_URL;
    private static final Logger log = LoggerFactory.getLogger("BDevCore");

    public static void addUrl(Plugin plugin, Path path) {
        try {
            File file;
            try {
                file = fixJar(path.toFile(), plugin);
            } catch (Exception e) {
                log.error("Failed to fix jar {} for {}", path, plugin.getName(), e);
                file = path.toFile();
            }
            log.info("Loading library {}", file.toPath());
            addUrl(plugin, file.toPath().toUri().toURL());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private static void addUrl(Plugin plugin, URL url) {
        try {
            ADD_URL.invoke(((URLClassLoader) plugin.getClass().getClassLoader()), url);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static File fixJar(File file, Plugin plugin) throws Exception {
        if (!file.getName().endsWith(".jar")) return file;

        File out = new File(
                file.getParentFile(),
                ".fixed/" + ServerVersion.CURRENT_ID + "/" + file.getName()
        );
        if (out.exists()) return out;
        out.getParentFile().mkdirs();

        try (
                JarFile jar = new JarFile(file);
                JarOutputStream jos = new JarOutputStream(new FileOutputStream(out))
        ) {
            PluginDescriptionFile descriptionFile;
            var pluginYml = jar.getEntry("plugin.yml");
            if (pluginYml != null) {
                try (var in = jar.getInputStream(pluginYml)) {
                    descriptionFile = new PluginDescriptionFile(in);
                }
            } else {
                descriptionFile = plugin.getDescription();
            }
            var iterator =  jar.entries();
            while (iterator.hasMoreElements()){
                JarEntry entry =  iterator.nextElement();
                String name = entry.getName();

                if (name.startsWith("META-INF/")
                        && (name.endsWith(".SF")
                        || name.endsWith(".RSA")
                        || name.endsWith(".DSA"))) {
                    continue;
                }
                if (entry.isDirectory()) {
                    jos.putNextEntry(new JarEntry(name));
                    jos.closeEntry();
                    continue;
                }
                JarEntry newEntry = new JarEntry(name);
                newEntry.setTime(entry.getTime());
                newEntry.setMethod(JarEntry.DEFLATED);

                jos.putNextEntry(newEntry);
                try (var entryStream = jar.getInputStream(entry)){
                    if (name.endsWith(".class") && !name.endsWith("module-info.class")) {
                        byte[] clazz = entryStream.readAllBytes();
                        @SuppressWarnings("deprecation")
                        byte[] patched = Bukkit.getUnsafe()
                                .processClass(descriptionFile, name, clazz);
                        jos.write(patched);
                    } else {
                        entryStream.transferTo(jos);
                    }
                }
                jos.closeEntry();
            }
        }

        return out;
    }

    static {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Unsafe unsafe = (Unsafe) theUnsafe.get(null);

            Field implLookup = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");

            MethodHandles.Lookup lookup = (MethodHandles.Lookup) unsafe.getObject(unsafe.staticFieldBase(implLookup), unsafe.staticFieldOffset(implLookup));
            Class<?> urlClassType = Class.forName("java.net.URLClassLoader");

            ADD_URL = lookup.findVirtual(urlClassType, "addURL", MethodType.methodType(Void.TYPE, URL.class));

        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
