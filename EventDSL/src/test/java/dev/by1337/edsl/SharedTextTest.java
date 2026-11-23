package dev.by1337.edsl;

import dev.by1337.edsl.text.LocalizedText;
import dev.by1337.yaml.YamlValue;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.translation.GlobalTranslator;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SharedTextTest {
    @Test
    void deferredEffectReceivesLocaleAndImmutableFrameArguments() {
        var text = decode(Map.of("en_US", "Hello", "ru_RU", "Привет"));
        dev.by1337.edsl.text.TranslatedText prefix = (reference, locale) -> {
            Component source = (Component) reference.arguments().get(0).value();
            Component rendered = GlobalTranslator.render(source, locale);
            String value = ((net.kyori.adventure.text.TextComponent) rendered).content();
            int count = Integer.parseInt(((net.kyori.adventure.text.TextComponent) reference.arguments().get(1).value()).content());
            return Component.text(value.substring(0, Math.min(count, value.length())));
        };
        try (var registry = new TextRegistry()) {
            var source = registry.component(text);
            var first = registry.component(prefix, source, 2);
            var second = registry.component(prefix, source, 4);
            assertEquals(first.key(), second.key());
            assertEquals(Component.text("He"), GlobalTranslator.render(first, Locale.US));
            assertEquals(Component.text("Пр"), GlobalTranslator.render(first, Locale.forLanguageTag("ru-RU")));
            assertEquals(Component.text("Прив"), GlobalTranslator.render(second, Locale.forLanguageTag("ru-RU")));
            assertEquals(Component.text("Пр"), GlobalTranslator.render(first, Locale.forLanguageTag("ru-RU")));
        }
    }

    @Test
    void oneReferenceRendersInTwoLanguagesWithoutResolvingItsSource() {
        var text = decode(Map.of("en_US", "Hit {0}/{1}", "ru_RU", "Бей {0}/{1}"));
        try (var registry = new TextRegistry()) {
            var reference = registry.component(text, 2, 8);
            assertEquals(Component.text("Hit 2/8"), GlobalTranslator.render(reference, Locale.US));
            assertEquals(Component.text("Бей 2/8"), GlobalTranslator.render(reference, Locale.forLanguageTag("ru-RU")));
            assertEquals(Component.text("Hit 2/8"), GlobalTranslator.render(reference, Locale.GERMANY));
            var next = registry.component(text, 3, 8);
            assertEquals(reference.key(), next.key());
            assertEquals(Component.text("Hit 3/8"), GlobalTranslator.render(next, Locale.US));
        }
    }

    @Test
    void acceptsStringAndValidatesNestedLocales() {
        try (var registry = new TextRegistry()) {
            assertEquals(Component.text("English"), GlobalTranslator.render(registry.component(decode("English")), Locale.US));
        }
        for (var invalid : java.util.List.of(Map.of(), Map.of("ru_!", "bad"),
                Map.of("en_US", "one", "en-US", "two"))) {
            assertTrue(LocalizedText.DECODER.decode(YamlValue.wrap(invalid)).hasError(), invalid.toString());
        }
    }

    @Test
    void closeUnregistersKeysAndCannotRemoveAnotherOpening() {
        var text = decode("Original");
        var first = new TextRegistry();
        try (var second = new TextRegistry()) {
            var a = first.component(text);
            var b = second.component(text);
            assertNotEquals(a.key(), b.key());
            first.close();
            first.close();
            assertEquals(a, GlobalTranslator.render(a, Locale.US));
            assertEquals(Component.text("Original"), GlobalTranslator.render(b, Locale.US));
            assertThrows(IllegalStateException.class, () -> first.component(text));
        } finally {
            first.close();
        }
    }

    private LocalizedText decode(Object yaml) {
        return LocalizedText.DECODER.decode(YamlValue.wrap(yaml)).getOrThrow();
    }
}
