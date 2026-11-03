package dev.by1337.core.util.io;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import dev.by1337.yaml.YamlMap;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class ResourceUtil {

    /**
     * Selects resources by exact path or a trailing {@code /*}, which includes all files
     * below that directory recursively. Selection and copying are deferred until store.
     */
    @NotNull
    public static ResourceStream resourceStream(@NotNull Plugin plugin, String... entries) {
        return new ResourceStream(List.of(entries), plugin, plugin.getDataFolder(), Function.identity());
    }

    @NotNull
    @CanIgnoreReturnValue
    public static File saveIfNotExist(@NotNull String path, @NotNull Plugin plugin) {
        return saveIfNotExist(path, plugin, plugin.getDataFolder(), Function.identity());
    }

    /** Reads the original resource path; the mutator affects only its path under destRoot. */
    @NotNull
    @CanIgnoreReturnValue
    public static File saveIfNotExist(@NotNull String path, @NotNull Plugin plugin, File destRoot, Function<String, String> pathMutator) {
        String source = normalizeResourcePath(path);
        String destination = Objects.requireNonNull(pathMutator.apply(source), "Mutated resource path");
        Path root = destRoot.toPath().toAbsolutePath().normalize();
        Path target = root.resolve(normalizeResourcePath(destination)).normalize();
        if (!target.startsWith(root) || target.equals(root)) {
            throw new IllegalArgumentException("Resource destination must be a file under " + destRoot + ": " + destination);
        }
        if (!Files.exists(target)) {
            // saveResource always writes under plugin.getDataFolder(), so copy the stream directly.
            try (var input = plugin.getResource(source)) {
                if (input == null) throw new IllegalArgumentException("Resource not found in " + plugin.getName() + ": " + source);
                Files.createDirectories(target.getParent());
                try {
                    Files.copy(input, target);
                } catch (FileAlreadyExistsException ignored) {
                    // Another caller has already created this file. Never overwrite it.
                }
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to save resource " + source + " to " + target, e);
            }
        }
        return target.toFile();
    }

    @Nullable
    @CanIgnoreReturnValue
    public static File saveOptionalIfNotExist(@NotNull String path, @NotNull Plugin plugin) {
        try {
            return saveIfNotExist(path, plugin);
        } catch (Exception e) {
            return null;
        }
    }

    public static YamlMap load(@NotNull String path, @NotNull Plugin plugin) {
        return YamlMap.load(saveIfNotExist(path, plugin));
    }

    private static String normalizeResourcePath(String path) {
        String normalized = Objects.requireNonNull(path, "path").replace('\\', '/');
        int start = 0;
        while (start < normalized.length() && normalized.charAt(start) == '/') start++;
        normalized = normalized.substring(start);
        if (normalized.isEmpty()) throw new IllegalArgumentException("Empty resource path");
        return normalized;
    }

    // Enumerate the plugin's own code source, including JARs without directory entries.
    static List<String> resourcePaths(Path location) {
        try {
            if (Files.isDirectory(location)) {
                try (var files = Files.walk(location)) {
                    return files.filter(Files::isRegularFile)
                            .map(path -> location.relativize(path).toString().replace('\\', '/'))
                            .sorted().collect(Collectors.toList());
                }
            }
            try (var jar = new JarFile(location.toFile())) {
                return jar.stream().filter(entry -> !entry.isDirectory()).map(entry -> entry.getName())
                        .sorted().collect(Collectors.toList());
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to enumerate resources in " + location, e);
        }
    }

    /**
     * Immutable copy builder. Paths always refer to the original plugin resources;
     * {@link #pathMutator(Function)} and {@link #stripPrefix(String)} affect only saved names.
     */
    public static class ResourceStream {
        private final List<String> entries;
        private final Plugin plugin;
        private final File destination;
        private final Function<String, String> pathMutator;

        public ResourceStream(Set<String> entries, Plugin plugin) {
            this(entries, plugin, plugin.getDataFolder(), Function.identity());
        }

        private ResourceStream(Collection<String> entries, Plugin plugin, File destination, Function<String, String> pathMutator) {
            this.entries = List.copyOf(entries);
            this.plugin = Objects.requireNonNull(plugin, "plugin");
            this.destination = Objects.requireNonNull(destination, "destination");
            this.pathMutator = Objects.requireNonNull(pathMutator, "pathMutator");
        }

        /** Sets the destination root, including a directory owned by a different plugin. */
        public ResourceStream to(File folder) {
            return new ResourceStream(entries, plugin, folder, pathMutator);
        }

        /** Replaces the destination path mapping. Receives a normalized original resource path. */
        public ResourceStream pathMutator(Function<String, String> mutator) {
            return new ResourceStream(entries, plugin, destination, mutator);
        }

        /** Keeps the relative directory structure below the given source prefix. */
        public ResourceStream stripPrefix(String prefix) {
            String normalized = normalizeResourcePath(prefix);
            String directory = normalized.endsWith("/") ? normalized : normalized + "/";
            return pathMutator(path -> {
                if (!path.startsWith(directory)) {
                    throw new IllegalArgumentException("Resource " + path + " is not under " + directory);
                }
                return path.substring(directory.length());
            });
        }

        /** Copies missing files, preserving every existing destination file. */
        public void storeIfNotExist() {
            for (String entry : resolveEntries()) {
                saveIfNotExist(entry, plugin, destination, pathMutator);
            }
        }

        /**
         * Checks the destination once before any resource lookup or copying.
         * If it already exists, skips the entire selection, including missing destination files.
         */
        public void storeIfFolderNotExist() {
            if (destination.exists()) return;
            storeIfNotExist();
        }

        public void storeToNotExistFolder(File folder) {
            to(folder).storeIfFolderNotExist();
        }

        public void storeToNotExistFolder(File folder, Function<String, String> mutator) {
            to(folder).pathMutator(mutator).storeIfFolderNotExist();
        }

        private Set<String> resolveEntries() {
            Set<String> resolved = new LinkedHashSet<>();
            List<String> available = null;
            for (String entry : entries) {
                String path = normalizeResourcePath(entry);
                int wildcard = path.indexOf('*');
                if (wildcard == -1) {
                    resolved.add(path);
                } else {
                    if (!path.endsWith("/*") || wildcard != path.length() - 1) {
                        throw new IllegalArgumentException("Only a trailing /* is supported: " + entry);
                    }
                    if (available == null) available = pluginResourcePaths();
                    String prefix = path.substring(0, path.length() - 1);
                    for (String resource : available) {
                        if (resource.startsWith(prefix)) resolved.add(resource);
                    }
                }
            }
            return resolved;
        }

        private List<String> pluginResourcePaths() {
            var source = plugin.getClass().getProtectionDomain().getCodeSource();
            if (source == null) throw new IllegalStateException("Cannot locate resource archive for " + plugin.getName());
            try {
                return resourcePaths(Path.of(source.getLocation().toURI()));
            } catch (URISyntaxException e) {
                throw new IllegalStateException("Invalid resource archive location for " + plugin.getName(), e);
            }
        }
    }
}
