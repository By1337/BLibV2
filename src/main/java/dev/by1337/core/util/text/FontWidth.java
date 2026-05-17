package dev.by1337.core.util.text;

import it.unimi.dsi.fastutil.ints.Int2FloatMap;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Objects;

public class FontWidth {
    private static final Int2FloatMap FRONT = new Int2FloatOpenHashMap();

    public static int getPixels(Component c) {
        return ceil(getWidth(c));
    }

    public static float getWidth(Component c) {
        float width = 0;
        if (c instanceof TextComponent text) {
            width += getWidth(text.content());
            if (c.hasDecoration(TextDecoration.BOLD)) {
                width += text.content().length();
            }
        }
        for (Component child : c.children()) {
            width += getWidth(child);
        }
        return width;
    }

    public static int getPixels(char c) {
        return ceil(getWidth(c));
    }

    public static float getWidth(char c) {
        return FRONT.get(c);
    }

    public static int getPixels(String text) {
        return ceil(getWidth(text));
    }

    public static float getWidth(String text) {
        float f = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            float v = FRONT.get(c);
            f += v;
        }
        return f;
    }

    public static int ceil(float f) {
        int i = (int) f;
        return f > i ? i + 1 : i;
    }

    private static @Nullable InputStream getResource(@NotNull String filename) {
        try {
            URL url = FontWidth.class.getClassLoader().getResource(filename);
            if (url == null) {
                return null;
            } else {
                URLConnection connection = url.openConnection();
                connection.setUseCaches(false);
                return connection.getInputStream();
            }
        } catch (IOException var4) {
            return null;
        }
    }

    static {
        FRONT.defaultReturnValue(4);
        try (var dis = new DataInputStream(Objects.requireNonNull(getResource("chars.bin")))) {
            int s = dis.readInt();
            for (int i = 0; i < s; i++) {
                FRONT.put(dis.readInt(), dis.readFloat());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
