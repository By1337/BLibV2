package dev.by1337.core.util;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

public class RepositoryUtil {
    public static final String MAVEN_REPO = "https://repo1.maven.org/maven2";
    public static final String BDEV_REPO = "https://repo.by1337.space/releases";
    private static final Logger log = LoggerFactory.getLogger("BDevCore");

    //groupId:artifactId:version
    public static Path download(String path, Path folder) {
        return download(MAVEN_REPO, path, folder);
    }

    //groupId:artifactId:version
    public static Path download(String repo, String path, Path folder) {
        String[] args = path.split(":", 3);
        if (args.length != 3) {
            throw new IllegalArgumentException("Expected groupId:artifactId:version, but got " + path);
        }
        return download(repo, args[0], args[1], args[2], folder);
    }

    public static Path download(
            String repo,
            String groupId,
            String artifactId,
            String version,
            Path folder
    ) {
        // url/{groupId}/{artifactId}/{version}/{artifactId}-{version}.jar
        String path = groupId.replace('.', '/') + '/'
                + artifactId + '/'
                + version + '/'
                + artifactId + '-' + version + ".jar";

        String fullUrl = repo.endsWith("/")
                ? repo + path
                : repo + '/' + path;

        Path resultFolder = folder
                .resolve(groupId.replace('.', '/'))
                .resolve(artifactId)
                .resolve(version);
        try {
            Files.createDirectories(resultFolder);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create directory " + resultFolder, e);
        }

        Path out = resultFolder.resolve(artifactId + "-" + version + ".jar");
        if (Files.exists(out)) return out;

        try (InputStream shaded = getInputStream("libraries/" + path)) {
            if (shaded != null) {
                try (OutputStream fos = Files.newOutputStream(out)) {
                    shaded.transferTo(fos);
                    return out;
                }
            }
        } catch (IOException e) {
            log.error("Failed to load shaded dependency {}", path, e);
        }

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        Path tmp = out.resolveSibling(out.getFileName() + ".part");

        try {
            log.info("Downloading {}", fullUrl);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<Path> response =
                    client.send(request, HttpResponse.BodyHandlers.ofFile(tmp));

            if (response.statusCode() != 200) {
                throw new RuntimeException("HTTP " + response.statusCode() + " for " + fullUrl);
            }

//            if (Files.size(tmp) < 1024) {
//                throw new RuntimeException("Downloaded file is too small: " + tmp);
//            }

            Files.move(tmp, out,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);

            return out;

        } catch (Exception e) {
            throw new RuntimeException("Failed to download " + fullUrl, e);
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
