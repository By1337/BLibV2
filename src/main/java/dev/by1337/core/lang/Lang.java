package dev.by1337.core.lang;

import dev.by1337.core.BDev;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

/**
 * Translation provider for the bcmd command framework.
 *
 * <p>Mirrors BLib v1's {@code Lang}: a thin wrapper that delegates key resolution to a
 * {@link Translation}. Translations are loaded from the external {@code translation.json}
 * in the plugin's data folder (so server admins can override them), falling back to the
 * bundled resource inside the jar. The file is re-read when its last-modified time changes,
 * so editing it applies on the next lookup without a plugin reload.
 */
public final class Lang {
    private static final Object LOCK = new Object();
    private static volatile Translation translation;
    private static long lastModified = -1;

    private Lang() {
    }

    /** Force (re)load from the external file or the bundled resource. */
    public static void loadTranslations() {
        synchronized (LOCK) {
            reloadLocked();
        }
    }

    public static String getMessage(String key) {
        if (translation == null || outdated()) {
            synchronized (LOCK) {
                if (translation == null || outdated()) {
                    reloadLocked();
                }
            }
        }
        return translation == null ? key : getOrDefault(translation.translate(key), key);
    }

    private static boolean outdated() {
        File ext = externalFile();
        return ext != null && ext.exists() && ext.lastModified() != lastModified;
    }

    private static void reloadLocked() {
        File ext = externalFile();
        try (InputStream in = open(ext)) {
            if (in == null) {
                translation = null;
                return;
            }
            translation = Translation.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8));
            lastModified = ext != null && ext.exists() ? ext.lastModified() : -1;
        } catch (Exception ignored) {
            if (ext != null && ext.exists()) lastModified = ext.lastModified();
        }
    }

    private static InputStream open(File ext) throws Exception {
        if (ext != null && ext.exists()) return new FileInputStream(ext);
        return Lang.class.getResourceAsStream("/translation.json");
    }

    private static File externalFile() {
        try {
            if (BDev.HOME_DIR != null) return BDev.HOME_DIR.resolve("translation.json").toFile();
        } catch (Throwable ignored) {
            // BDev not available (e.g. offline test) -> fall back to jar resource
        }
        return null;
    }

    private static <T> T getOrDefault(T value, T def) {
        return value == null ? def : value;
    }
}
