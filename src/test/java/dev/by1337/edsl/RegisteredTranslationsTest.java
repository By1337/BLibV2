package dev.by1337.edsl;

import dev.by1337.edsl.text.RawTextComponentLike;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.translation.GlobalTranslator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class RegisteredTranslationsTest {
    @AfterEach
    void resetThemes() {
        Themes.setSelector(null);
        Themes.setThemeReplacer(null);
    }

    @Test
    void staticRegistryNeedsNoPluginAndResolvesCurrentTheme() {
        AtomicReference<String> theme = new AtomicReference<>("orange");
        Themes.setSelector(player -> theme.get());
        Themes.setThemeReplacer(id -> text -> text.replace("{label}", id));
        LangTranslator.registerTranslations("registry_theme", Map.of(
                "registry_theme.sword.name", new RawTextComponentLike("{label}")));
        Component reference = Component.translatable("registry_theme.sword.name");
        assertEquals(Component.text("orange"), GlobalTranslator.render(reference, Locale.US));
        theme.set("blue");
        assertEquals(Component.text("blue"), GlobalTranslator.render(reference, Locale.US));
    }

    @Test
    void replacementRemovesOldLinesAndItemsButPreservesOtherRegistries() {
        LangTranslator.registerTranslations("registry_reload", Map.of(
                "registry_reload.sword.lore.0", Component.text("old"),
                "registry_reload.sword.lore.1", Component.text("removed line"),
                "registry_reload.removed.name", Component.text("removed item")));
        LangTranslator.registerTranslations("registry_other", Map.of(
                "registry_other.sword.name", Component.text("other")));
        LangTranslator.registerTranslations("registry_reload", Map.of(
                "registry_reload.sword.lore.0", Component.text("new")));
        assertEquals(Component.text("new"), render("registry_reload.sword.lore.0"));
        assertEquals(Component.empty(), render("registry_reload.sword.lore.1"));
        assertEquals(Component.empty(), render("registry_reload.removed.name"));
        assertEquals(Component.empty(), render("registry_reload.sword.lore.500"));
        assertEquals(Component.text("other"), render("registry_other.sword.name"));
        assertEquals(Component.translatable("unowned.missing"), render("unowned.missing"));
        LangTranslator.registerTranslations("registry_reload", Map.of());
        assertEquals(Component.empty(), render("registry_reload.sword.lore.0"));
    }

    @Test
    void rejectsForeignKeysWithoutReplacingPreviousValues() {
        LangTranslator.registerTranslations("registry_validation", Map.of(
                "registry_validation.name", Component.text("kept")));
        assertThrows(IllegalArgumentException.class, () -> LangTranslator.registerTranslations(
                "registry_validation", Map.of("foreign.name", Component.text("bad"))));
        assertEquals(Component.text("kept"), render("registry_validation.name"));
    }

    private static Component render(String key) {
        return GlobalTranslator.render(Component.translatable(key), Locale.US);
    }
}
