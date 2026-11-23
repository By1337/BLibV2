package dev.by1337.core;

import dev.by1337.bpatch.PatchArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

class NmsBridgeArchiveTest {
    @TempDir
    Path directory;

    @Test
    void unpacksEveryBundledVersionWithoutStartingBukkit() throws Exception {
        try (var input = getClass().getResourceAsStream("/bridges/nms.zip")) {
            assertNotNull(input);
            try (PatchArchive archive = PatchArchive.open(input)) {
                assertFalse(archive.versions().isEmpty());
                assertTrue(archive.versions().stream().anyMatch(version -> version.id().equals(ServerVersion.LastKnown.ID)));
                for (PatchArchive.Version version : archive.versions()) {
                    Path file = Bootstrap.unpackNMSBridge(directory, version.id());
                    try (ZipFile jar = new ZipFile(file.toFile())) {
                        assertNotNull(jar.getEntry("dev/by1337/core/BridgeBootstrapper.class"));
                        assertFalse(jar.stream().anyMatch(entry -> entry.getName().startsWith("META-INF/")
                                || entry.getName().endsWith(".removed")));
                    }
                }
                assertEquals(archive.versions().size(), countFiles());
            }
        }
    }

    @Test
    void reusesTheExtractedBridge() throws Exception {
        Path first = Bootstrap.unpackNMSBridge(directory, "1.16.5");
        byte[] expected = Files.readAllBytes(first);
        FileTime marker = FileTime.fromMillis(1000000000000L);
        Files.setLastModifiedTime(first, marker);
        assertEquals(first, Bootstrap.unpackNMSBridge(directory, "1.16.5"));
        assertEquals(marker, Files.getLastModifiedTime(first));
        assertArrayEquals(expected, Files.readAllBytes(first));
    }

    @Test
    void missingVersionDoesNotLeaveAPartialJar() throws Exception {
        assertThrows(IOException.class, () -> Bootstrap.unpackNMSBridge(directory, "missing"));
        assertEquals(0, countFiles());
    }

    private long countFiles() throws IOException {
        try (var files = Files.list(directory)) {
            return files.count();
        }
    }
}
