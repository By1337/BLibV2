package dev.by1337.core.legacy;

import dev.by1337.core.util.reflect.ClasspathUtil;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@ApiStatus.Internal
public final class BVaultBridge {
    private static final String JAR_NAME = "BVault-api-1.1.jar";

    private BVaultBridge() {
    }

    public static void bootstrap(Plugin plugin) {
        try {
            // Reuse the provider's API without initializing it before BVault.onLoad().
            Class.forName("dev.by1337.vault.BVaultApi", false, BVaultBridge.class.getClassLoader());
            return;
        } catch (ClassNotFoundException ignored) {
            // BVault is optional; expose the bundled API to plugins depending on BLib.
        }

        Path directory = plugin.getDataFolder().toPath().resolve(".bridges");
        Path file = directory.resolve(JAR_NAME);
        try {
            if (!Files.isRegularFile(file)) {
                Files.createDirectories(directory);
                try (var input = plugin.getResource("bridges/" + JAR_NAME)) {
                    if (input == null) {
                        throw new IOException("Unable to find bridges/" + JAR_NAME);
                    }
                    Path temporary = Files.createTempFile(directory, "BVault-api-", ".tmp");
                    try {
                        Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
                        Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
                    } finally {
                        Files.deleteIfExists(temporary);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to unpack " + JAR_NAME, e);
        }
        ClasspathUtil.addUrl(plugin, file);
    }
}
