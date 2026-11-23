package dev.by1337.edsl.text;

import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TranslatableComponent;

/** A deferred text effect. Arguments must capture immutable state before packet encoding. */
@FunctionalInterface
public interface TranslatedText extends ComponentLike {
    Component render(TranslatableComponent reference, Locale locale);

    @Override
    default Component asComponent() {
        throw new IllegalStateException("Register deferred text through TextRegistry.component");
    }
}
