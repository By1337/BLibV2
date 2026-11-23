package dev.by1337.edsl;

import dev.by1337.yaml.YamlValue;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class LangTranslatorTest {
    @TempDir
    Path directory;

    private Plugin plugin() {
        return (Plugin) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ Plugin.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "getDataFolder" -> directory.toFile();
                    case "getName" -> "TestPlugin";
                    case "namespace" -> "testplugin";
                    default -> null;
                });
    }

    @Test
    void restoresDotsInTranslationKeysAndPreservesDotsInValues() throws Exception {
        Files.writeString(directory.resolve("lang.yml"), "ru_RU:\n  'time.format.years': 'Годы.'\n");
        LangTranslator translator = LangTranslator.decoder("lang.yml").decode(YamlValue.wrap(Map.of()), plugin()).getOrThrow();
        GlobalTranslator.translator().addSource(translator);
        try {
            assertEquals(Component.text("Годы."), GlobalTranslator.render(Component.translatable("testplugin.time.format.years"), new Locale("ru", "RU")));
        } finally {
            GlobalTranslator.translator().removeSource(translator);
        }
    }

    @Test
    void dottedTranslationKeysStillResolveThemeAtRenderTime() throws Exception {
        Files.writeString(directory.resolve("lang.yml"), "ru_RU:\n  'second.color': '{label}'\n");
        AtomicReference<String> selected = new AtomicReference<>("first");
        Themes.setSelector(player -> selected.get());
        Themes.setThemeReplacer(id -> text -> text.replace("{label}", id));
        LangTranslator translator = LangTranslator.decoder("lang.yml").decode(YamlValue.wrap(Map.of()), plugin()).getOrThrow();
        GlobalTranslator.translator().addSource(translator);
        try {
            Component input = Component.translatable("testplugin.second.color");
            assertEquals(Component.text("first"), GlobalTranslator.render(input, new Locale("ru", "RU")));
            selected.set("second");
            assertEquals(Component.text("second"), GlobalTranslator.render(input, new Locale("ru", "RU")));
        } finally {
            GlobalTranslator.translator().removeSource(translator);
            Themes.setSelector(null);
            Themes.setThemeReplacer(null);
        }
    }
}
