package dev.by1337.core.util.text;

import dev.by1337.core.ServerVersion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;

import java.time.Duration;

/**
 * Adventure API compatibility across supported server versions.
 */
public final class AdventureAdapter {
    private AdventureAdapter() {
    }

    /**
     * Uses the modern factory on 1.21.11+, retaining the 1.16.5-compatible factory on older servers.
     */
    @SuppressWarnings("deprecation")
    public static Title.Times titleTimes(Duration fadeIn, Duration stay, Duration fadeOut) {
        if (ServerVersion.is1_21_11orNewer()) {
            return Title.Times.times(fadeIn, stay, fadeOut);
        }
        return Title.Times.of(fadeIn, stay, fadeOut);
    }

    public static Component stable(Component c) {
        return c;
    }
}
