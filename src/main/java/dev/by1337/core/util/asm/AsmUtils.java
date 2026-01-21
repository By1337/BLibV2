package dev.by1337.core.util.asm;

import dev.by1337.core.BDev;

import java.io.File;
import java.nio.file.Files;

public class AsmUtils {
    public static void dumpGeneratedClass(byte[] clazz, String name) {
        try {
            File generated = new File(BDev.HOME_DIR.toFile(), ".generated");
            generated.mkdirs();
            Files.write(generated.toPath().resolve(name + ".class"), clazz);
        } catch (Exception ignored) {
        }
    }
}
