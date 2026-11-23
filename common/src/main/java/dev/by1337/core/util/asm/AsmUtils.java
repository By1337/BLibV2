package dev.by1337.core.util.asm;

import dev.by1337.core.util.io.BLibDataFolder;
import org.jetbrains.annotations.ApiStatus;

import java.io.File;
import java.nio.file.Files;

@ApiStatus.Internal
public class AsmUtils {
    public static void dumpGeneratedClass(byte[] clazz, String name) {
        try {
            File generated = new File(BLibDataFolder.get().toFile(), ".generated");
            generated.mkdirs();
            Files.write(generated.toPath().resolve(name + ".class"), clazz);
        } catch (Exception ignored) {
        }
    }
}
