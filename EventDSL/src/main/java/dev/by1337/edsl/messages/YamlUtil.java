package dev.by1337.edsl.messages;


import dev.by1337.yaml.YamlMap;
import org.jetbrains.annotations.ApiStatus;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@ApiStatus.Internal
public class YamlUtil {

    public static List<YamlMap> loadAll(File file) {
        List<YamlMap> out = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equals("---")) {
                    out.add(YamlMap.load(new StringReader(builder.toString())));
                    builder.setLength(0);
                    continue;
                }
                builder.append(line).append("\n");
            }
            if (!builder.isEmpty()) {
                out.add(YamlMap.load(new StringReader(builder.toString())));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return out;
    }

    public static YamlMap replaceAndLoad(File file, String from, String to) {
        try {
            String s = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            return YamlMap.loadFromString(s.replace(from, to));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
