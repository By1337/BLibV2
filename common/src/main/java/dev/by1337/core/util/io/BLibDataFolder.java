package dev.by1337.core.util.io;

import org.jetbrains.annotations.ApiStatus;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Supplier;

@ApiStatus.Internal
public final class BLibDataFolder {
    private static volatile Supplier<Path> provider;

    private BLibDataFolder() {
    }

    public static Path get() {
        var current = provider;
        if (current == null) {
            throw new IllegalStateException("BLib data folder has not been initialized");
        }
        return current.get();
    }

    public static void setProvider(Supplier<Path> provider) {
        BLibDataFolder.provider = Objects.requireNonNull(provider);
    }
}
