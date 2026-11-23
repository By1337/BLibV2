package dev.by1337.edsl;

import dev.by1337.edsl.context.EventContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import dev.by1337.yaml.YamlValue;
import dev.by1337.yaml.YamlMap;
import static org.junit.jupiter.api.Assertions.*;

class LocaleMapCommandsTest extends EventDslTest {
    private final Locale previousDefault = Themes.defaultLocale;

    private interface VersionedServer {
    }

    record Output(List<Component> messages, List<Component> bars, List<Title> titles) {
        Output() {
            this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }
    }

    @AfterEach
    void reset() {
        Themes.defaultLocale = previousDefault;
        Themes.setSelector(null);
        Themes.setThemeReplacer(null);
    }

    private Player player(AtomicReference<Locale> locale, Output output) {
        return (Player) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ Player.class }, (p, method, args) -> {
            switch (method.getName()) {
                case "locale":
                    return locale.get();
                case "getName":
                    return "test";
                case "sendMessage":
                    if (args.length == 1 && args[0] instanceof Component text) output.messages.add(text);
                    break;
                case "sendActionBar":
                    output.bars.add((Component) args[0]);
                    break;
                case "showTitle":
                    output.titles.add((Title) args[0]);
                    break;
            }
            return null;
        });
    }

    @Test
    void selectsLiveLocaleThenConfiguredFallbackWithoutFreezingThemeOrPlaceholders() {
        Themes.defaultLocale = Locale.US;
        var locale = new AtomicReference<>(Locale.FRANCE);
        var output = new Output();
        var player = player(locale, output);
        var value = new AtomicReference<>("one");
        Themes.setThemeReplacer(id -> text -> text.replace("{color}", "theme"));
        var context = new EventContext(false, player) {
            @Override
            public String setPlaceholders(String text) {
                return text.replace("{value}", value.get());
            }
        };
        var handler = EventHandler.decoder().decode(YamlValue.wrap(Map.of("message", Map.of(
                "ru_RU", "{color} русский {value}", "en_US", "{color} English {value}")))).getOrThrow();
        assertTrue(handler.handle(context));
        locale.set(new Locale("ru", "BY"));
        value.set("two");
        handler.handle(context);
        assertEquals(List.of(Component.text("theme English one"), Component.text("theme русский two")), output.messages);
        assertTrue(EventHandler.decoder().decode(YamlValue.wrap(Map.of("message", Map.of("ru_???", "bad")))).hasError());
    }

    @Test
    void missingLanguageAndMissingConfiguredFallbackDoNotPickAnArbitraryTranslation() {
        Themes.defaultLocale = Locale.US;
        var output = new Output();
        var player = player(new AtomicReference<>(Locale.FRANCE), output);
        EventHandler.decoder().decode(YamlValue.wrap(Map.of("message", Map.of("ru_RU", "русский"))))
                .getOrThrow().handle(new EventContext(false, player));
        assertTrue(output.messages.isEmpty());
        assertTrue(EventHandler.decoder().decode(YamlValue.wrap(Map.of("message", Map.of()))).hasError());
    }

    @Test
    void broadcastsSelectLocaleAndThemeForEveryRecipientAndRestoreTarget() throws Exception {
        var ruOut = new Output();
        var enOut = new Output();
        var ru = player(new AtomicReference<>(new Locale("ru", "RU")), ruOut);
        var en = player(new AtomicReference<>(Locale.US), enOut);
        Themes.setSelector(p -> p == ru ? "red" : "blue");
        Themes.setThemeReplacer(id -> text -> text.replace("{theme}", id));
        var context = new EventContext(false, ru);
        withServer(List.of(ru, en), () -> {
            for (String command : List.of("bc", "broadcast", "message_all")) {
                EventHandler.decoder().decode(YamlValue.wrap(Map.of(command, Map.of(
                        "ru_RU", "{theme} RU", "en_US", "{theme} EN")))).getOrThrow().handle(context);
            }
        });
        assertEquals(List.of(Component.text("red RU"), Component.text("red RU"), Component.text("red RU")), ruOut.messages);
        assertEquals(List.of(Component.text("blue EN"), Component.text("blue EN"), Component.text("blue EN")), enOut.messages);
        assertSame(ru, context.target());
    }

    @Test
    void actionBarAndTitleMapsUseTheSameLocalizedPayloadRules() throws Exception {
        var output = new Output();
        var player = player(new AtomicReference<>(Locale.US), output);
        var context = new EventContext(false, player) {
            @Override
            public String setPlaceholders(String text) {
                return text.replace("{ticks}", "7");
            }
        };
        withServer(List.of(player), () -> {
            for (String name : List.of("action_bar", "action_bar_all")) {
                EventHandler.decoder().decode(YamlValue.wrap(Map.of(name, Map.of("en_US", "bar")))).getOrThrow().handle(context);
            }
            for (String name : List.of("title", "title_all")) {
                EventHandler.decoder().decode(YamlValue.wrap(Map.of(name, Map.of("en_US", "\"Title\\nSubtitle\" {ticks} 20 5"))))
                        .getOrThrow().handle(context);
            }
            EventHandler.decoder().decode(YamlValue.wrap(Map.of("title", Map.of("en_US", "Plain title\nPlain subtitle"))))
                    .getOrThrow().handle(context);
        });
        assertEquals(List.of(Component.text("bar"), Component.text("bar")), output.bars);
        assertEquals(3, output.titles.size());
        assertEquals(Component.text("Plain title"), output.titles.get(2).title());
        assertEquals(Component.text("Plain subtitle"), output.titles.get(2).subtitle());
        assertEquals(Component.text("Title"), output.titles.get(0).title());
        assertEquals(Component.text("Subtitle"), output.titles.get(0).subtitle());
        assertEquals(java.time.Duration.ofMillis(350), output.titles.get(0).times().fadeIn());
    }

    @Test
    void multilineTitlesAcceptTimingsOnOneOrThreeLines() throws Exception {
        var output = new Output();
        var locale = new AtomicReference<>(new Locale("ru", "RU"));
        var player = player(locale, output);
        var ticks = new AtomicReference<>("7");
        var context = new EventContext(false, player) {
            @Override
            public String setPlaceholders(String text) {
                return text.replace("{ticks}", ticks.get()).replace("%player_name%", "Alex");
            }
        };
        var yaml = YamlMap.loadFromString("""
                title:
                  ru_RU: |-
                    Привет!
                    Игрок: %player_name%
                    {ticks}
                    30
                    9
                  en_US: |
                    Hello!
                    Player: %player_name%
                    {ticks} 30 9
                """);
        var parsed = dev.by1337.edsl.command.TextCommands.Payload.parse("title", yaml.get("title").asYamlMap().getOrThrow().get("ru_RU").asString().getOrThrow());
        assertEquals("{ticks}", parsed.fadeIn());
        assertEquals("30", parsed.stay());
        assertEquals("9", parsed.fadeOut());
        withServer(List.of(player), () -> {
            for (String channel : List.of("title", "title_all")) {
                var handler = EventHandler.decoder().decode(YamlValue.wrap(Map.of(channel, yaml.get("title").getRaw())))
                        .getOrThrow();
                locale.set(new Locale("ru", "RU"));
                ticks.set("7");
                handler.handle(context);
                locale.set(Locale.US);
                ticks.set("8");
                handler.handle(context);
            }
        });
        assertEquals(4, output.titles.size());
        for (int i = 0; i < output.titles.size(); i++) {
            var title = output.titles.get(i);
            boolean ru = i % 2 == 0;
            assertEquals(Component.text(ru ? "Привет!" : "Hello!"), title.title());
            assertEquals(Component.text(ru ? "Игрок: Alex" : "Player: Alex"), title.subtitle());
            assertEquals(java.time.Duration.ofMillis(ru ? 350 : 400), title.times().fadeIn());
            assertEquals(java.time.Duration.ofMillis(1500), title.times().stay());
            assertEquals(java.time.Duration.ofMillis(450), title.times().fadeOut());
        }
    }

    @Test
    void multilineTitleRequiresExactlyThreeTimingArguments() {
        for (String tail : List.of("10", "10 20", "10 20 30 40")) {
            assertTrue(EventHandler.decoder().decode(YamlValue.wrap(Map.of("title", Map.of(
                    "en_US", "Title\nSubtitle\n" + tail)))).hasError(), tail);
        }
    }

    @Test
    void invalidRuntimeTitleDurationDoesNotAbortFollowingCommands() throws Exception {
        var output = new Output();
        var player = player(new AtomicReference<>(Locale.US), output);
        var context = new EventContext(false, player) {
            @Override
            public String setPlaceholders(String text) {
                return text.replace("{ticks}", "invalid");
            }

            @Override
            public org.slf4j.Logger getLogger() {
                return org.slf4j.helpers.NOPLogger.NOP_LOGGER;
            }
        };
        var handler = EventHandler.decoder().decode(YamlValue.wrap(List.of(
                Map.of("title", Map.of("en_US", "\"Title\" {ticks}")),
                Map.of("message", Map.of("en_US", "after"))))).getOrThrow();
        withServer(List.of(player), () -> handler.handle(context));
        assertTrue(output.titles.isEmpty());
        assertEquals(List.of(Component.text("after")), output.messages);
    }

    private void withServer(List<Player> players, Runnable action) throws Exception {
        var field = Bukkit.class.getDeclaredField("server");
        field.setAccessible(true);
        Object previous = field.get(null);
        var server = Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ Server.class, VersionedServer.class },
                (proxy, method, args) -> method.getName().equals("getOnlinePlayers") ? players : null);
        field.set(null, server);
        try {
            action.run();
        } finally {
            field.set(null, previous);
        }
    }
}
