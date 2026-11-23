package dev.by1337.core.util.io;

import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class ResourceUtilTest {
    private static final String MENU = "resource-util-test/menu/";
    @TempDir
    Path directory;

    @Test
    void mutatorChangesDestinationButKeepsOriginalResourceLookup() throws Exception {
        var plugin = plugin();
        File destination = directory.resolve("BMenu/menu/bauc").toFile();
        File saved = ResourceUtil.saveIfNotExist("/resource-util-test\\menu\\home.yml", plugin, destination, path -> {
            assertEquals(MENU + "home.yml", path);
            return path.substring(MENU.length());
        });
        assertEquals(new File(destination, "home.yml"), saved);
        assertEquals("title: Home\n", Files.readString(saved.toPath()));
        assertEquals(List.of(MENU + "home.yml"), plugin.reads);
        assertFalse(plugin.getDataFolder().exists());
    }

    @Test
    void builderCopiesAllMenuFilesWithoutRepeatingPrefix() throws Exception {
        var plugin = plugin();
        Path destination = directory.resolve("menu");
        ResourceUtil.resourceStream(plugin, MENU + "home.yml", MENU + "nested/confirm.yml")
                .to(destination.toFile())
                .stripPrefix(MENU)
                .storeIfFolderNotExist();
        assertEquals("title: Home\n", Files.readString(destination.resolve("home.yml")));
        assertEquals("title: Nested\n", Files.readString(destination.resolve("nested/confirm.yml")));
        assertFalse(Files.exists(destination.resolve("menu")));
        assertEquals(2, plugin.reads.size());
    }

    @Test
    void wildcardIncludesNestedResourcesAndPreservesStructureUnderExternalDestination() throws Exception {
        var plugin = plugin();
        Path destination = directory.resolve("BMenu/menu/bauc");
        ResourceUtil.resourceStream(plugin, MENU + "*", MENU + "home.yml")
                .to(destination.toFile())
                .stripPrefix(MENU)
                .storeIfFolderNotExist();
        assertTrue(Files.isRegularFile(destination.resolve("home.yml")));
        assertTrue(Files.isRegularFile(destination.resolve("nested/confirm.yml")));
        assertEquals(2, plugin.reads.size());
        try (var files = Files.walk(destination)) {
            assertEquals(2, files.filter(Files::isRegularFile).count());
        }
    }

    @Test
    void existingDestinationSkipsAllResourcesAndMapping() throws Exception {
        var plugin = plugin();
        Path destination = Files.createDirectory(directory.resolve("menu"));
        Files.writeString(destination.resolve("custom.yml"), "custom");
        ResourceUtil.resourceStream(plugin, "missing/*", "missing.yml")
                .to(destination.toFile())
                .pathMutator(path -> {
                    throw new AssertionError("Must not map paths");
                })
                .storeIfFolderNotExist();
        assertTrue(plugin.reads.isEmpty());
        assertEquals("custom", Files.readString(destination.resolve("custom.yml")));
        assertFalse(Files.exists(destination.resolve("missing.yml")));
    }

    @Test
    void existingEmptyDestinationAlsoSkipsMissingDefaults() throws Exception {
        var plugin = plugin();
        Path destination = Files.createDirectory(directory.resolve("menu"));
        ResourceUtil.resourceStream(plugin, MENU + "home.yml").to(destination.toFile()).stripPrefix(MENU).storeIfFolderNotExist();
        assertTrue(plugin.reads.isEmpty());
        assertFalse(Files.exists(destination.resolve("home.yml")));
    }

    @Test
    void perFileModePreservesExistingFilesAndAddsMissingOnes() throws Exception {
        var plugin = plugin();
        Path destination = Files.createDirectory(directory.resolve("menu"));
        Files.writeString(destination.resolve("home.yml"), "custom");
        ResourceUtil.resourceStream(plugin, MENU + "*").to(destination.toFile()).stripPrefix(MENU).storeIfNotExist();
        assertEquals("custom", Files.readString(destination.resolve("home.yml")));
        assertTrue(Files.isRegularFile(destination.resolve("nested/confirm.yml")));
        assertEquals(List.of(MENU + "nested/confirm.yml"), plugin.reads);
    }

    @Test
    void oldResourceStreamCallsRemainCompatible() throws Exception {
        var plugin = plugin();
        Path destination = directory.resolve("legacy");
        ResourceUtil.resourceStream(plugin, MENU + "home.yml", MENU + "nested/confirm.yml")
                .storeToNotExistFolder(destination.toFile(), path -> path.substring(MENU.length()));
        assertTrue(Files.isRegularFile(destination.resolve("home.yml")));
        assertTrue(Files.isRegularFile(destination.resolve("nested/confirm.yml")));
        plugin.reads.clear();
        ResourceUtil.resourceStream(plugin, "missing.yml").storeToNotExistFolder(destination.toFile());
        assertTrue(plugin.reads.isEmpty());
    }

    @Test
    void defaultSaveUsesPluginDataFolderAndOptionalMissingDoesNotCreateIt() throws Exception {
        var plugin = plugin();
        assertNull(ResourceUtil.saveOptionalIfNotExist("missing.yml", plugin));
        assertFalse(plugin.getDataFolder().exists());
        File saved = ResourceUtil.saveIfNotExist(MENU + "home.yml", plugin);
        assertEquals(plugin.getDataFolder().toPath().resolve(MENU + "home.yml"), saved.toPath());
        assertEquals("title: Home\n", Files.readString(saved.toPath()));
    }

    @Test
    void missingResourceFailsBeforeCreatingDestination() {
        var plugin = plugin();
        Path destination = directory.resolve("menu");
        var error = assertThrows(IllegalArgumentException.class, () -> ResourceUtil.saveIfNotExist("missing.yml", plugin, destination.toFile(), Function.identity()));
        assertTrue(error.getMessage().contains("missing.yml"));
        assertFalse(Files.exists(destination));
    }

    @Test
    void buildersCanBeReusedWithIndependentDestinations() {
        var plugin = plugin();
        var selection = ResourceUtil.resourceStream(plugin, MENU + "home.yml").stripPrefix(MENU);
        selection.to(directory.resolve("one").toFile()).storeIfFolderNotExist();
        selection.to(directory.resolve("two").toFile()).storeIfFolderNotExist();
        assertTrue(Files.isRegularFile(directory.resolve("one/home.yml")));
        assertTrue(Files.isRegularFile(directory.resolve("two/home.yml")));
    }

    @Test
    void jarEnumerationDoesNotRequireDirectoryEntries() throws Exception {
        Path archive = directory.resolve("plugin.jar");
        try (var jar = new JarOutputStream(Files.newOutputStream(archive))) {
            for (String name : List.of("menu/home.yml", "menu/nested/confirm.yml", "config.yml")) {
                jar.putNextEntry(new JarEntry(name));
                jar.write(name.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                jar.closeEntry();
            }
        }
        assertEquals(List.of("config.yml", "menu/home.yml", "menu/nested/confirm.yml"), ResourceUtil.resourcePaths(archive));
        Files.delete(archive); // The archive handle must be closed.
    }

    @Test
    void invalidMappingsAndUnsupportedWildcardsFailWithoutWriting() {
        var plugin = plugin();
        File destination = directory.resolve("menu").toFile();
        assertThrows(IllegalArgumentException.class, () -> ResourceUtil.saveIfNotExist(MENU + "home.yml", plugin, destination, path -> "../outside.yml"));
        assertThrows(IllegalArgumentException.class, () -> ResourceUtil.resourceStream(plugin, MENU + "home.yml").to(destination).stripPrefix("other").storeIfNotExist());
        assertThrows(IllegalArgumentException.class, () -> ResourceUtil.resourceStream(plugin, "menu/*.yml").to(destination).storeIfNotExist());
        assertFalse(destination.exists());
        assertTrue(plugin.reads.isEmpty());
    }

    private TestPlugin plugin() {
        return new TestPlugin(directory.resolve("Auction").toFile());
    }

    // A concrete plugin gives wildcard lookup a real code source (target/test-classes).
    private static final class TestPlugin implements Plugin {
        private final File dataFolder;
        private final List<String> reads = new ArrayList<>();

        private TestPlugin(File dataFolder) {
            this.dataFolder = dataFolder;
        }

        @Override
        public File getDataFolder() {
            return dataFolder;
        }

        @Override
        public String getName() {
            return "Auction";
        }

        @Override
        public InputStream getResource(String path) {
            reads.add(path);
            return getClass().getClassLoader().getResourceAsStream(path);
        }

        @Override
        public void saveResource(String path, boolean replace) {
            throw new AssertionError("saveResource ignores custom destinations");
        }

        @Override
        public PluginDescriptionFile getDescription() {
            throw new UnsupportedOperationException();
        }

        @Override
        public FileConfiguration getConfig() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void saveConfig() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void saveDefaultConfig() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void reloadConfig() {
            throw new UnsupportedOperationException();
        }

        @Override
        public PluginLoader getPluginLoader() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Server getServer() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public void onDisable() {
        }

        @Override
        public void onLoad() {
        }

        @Override
        public void onEnable() {
        }

        @Override
        public boolean isNaggable() {
            return false;
        }

        @Override
        public void setNaggable(boolean value) {
        }

        @Override
        public ChunkGenerator getDefaultWorldGenerator(String world, String id) {
            return null;
        }

        @Override
        public Logger getLogger() {
            return Logger.getAnonymousLogger();
        }

        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            return false;
        }

        @Override
        public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
            return List.of();
        }
    }
}
