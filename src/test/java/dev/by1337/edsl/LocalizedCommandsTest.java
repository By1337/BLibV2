package dev.by1337.edsl;

import dev.by1337.cmd.Command;
import dev.by1337.edsl.command.LocalizedTextCommands;
import dev.by1337.edsl.context.EventContext;
import dev.by1337.plc.PlaceholderResolver;
import dev.by1337.yaml.YamlValue;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class LocalizedCommandsTest {
    private interface VersionedTestServer {}

    @AfterEach
    void resetThemes() {
        Themes.setSelector(null);
        Themes.setThemeReplacer(null);
    }

    private Player player(AtomicReference<Locale> locale, List<Component> messages) {
        return (Player) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("locale")) return locale.get();
                    if (method.getName().equals("sendMessage") && args.length == 1 && args[0] instanceof Component text)
                        messages.add(text);
                    return null;
                });
    }

    private Command<EventContext> commands(List<Player> players) {
        return LocalizedTextCommands.create(EventContext::target, ctx -> ctx, (ctx, output) -> {
            for (Player player : players) {
                try (var ignored = ctx.target(player)) { output.accept(player); }
            }
        });
    }

    @Test
    void yamlSuffixesAndExplicitCommandsRespectCurrentExactLocaleAndRuntimePlaceholders() {
        var output = new ArrayList<Component>();
        var locale = new AtomicReference<>(new Locale("ru", "RU"));
        Player viewer = player(locale, output);
        var ctx = new EventContext(false, viewer, PlaceholderResolver.of("value", "42"))
                .setCommand(commands(List.of(viewer)));
        Themes.setThemeReplacer(id -> s -> s.replace("{theme}", "color"));
        EventHandler handler = EventHandler.decoder().decode(YamlValue.wrap(List.of(
                Map.of("messages-ru_RU", "{theme} {value}"),
                Map.of("message-en_US", "English {value}")))).getOrThrow();
        handler.handle(ctx);
        locale.set(Locale.US);
        handler.handle(ctx);
        locale.set(new Locale("ru", "BY"));
        handler.handle(ctx);
        assertEquals(List.of(Component.text("color 42"), Component.text("English 42")), output);
        locale.set(Locale.US);
        EventHandler.decoder().decode(YamlValue.wrap(Map.of("messages_if_lang", "en_US explicit")))
                .getOrThrow().handle(ctx);
        assertEquals(Component.text("explicit"), output.get(2));
    }

    @Test
    void localizedBroadcastFiltersRecipientsAndRestoresContextTarget() {
        var ruMessages = new ArrayList<Component>();
        var enMessages = new ArrayList<Component>();
        var ru = player(new AtomicReference<>(new Locale("ru", "RU")), ruMessages);
        var en = player(new AtomicReference<>(Locale.US), enMessages);
        Themes.setSelector(p -> p == en ? "English theme" : "Russian theme");
        Themes.setThemeReplacer(id -> text -> text.replace("{theme}", id));
        var ctx = new EventContext(false, ru).setCommand(commands(List.of(ru, en)));
        EventHandler.decoder().decode(YamlValue.wrap(Map.of("broadcast-en_US", "{theme}"))).getOrThrow().handle(ctx);
        assertTrue(ruMessages.isEmpty());
        assertEquals(List.of(Component.text("English theme")), enMessages);
        assertSame(ru, ctx.target());
    }

    @Test
    void preprocessingSupportsTextChannelsAndRejectsMalformedLocale() {
        assertEquals("[title_if_lang] en-US \"Title\" 1 2 3",
                LocalizedTextCommands.preprocess("[title-en_US] \"Title\" 1 2 3"));
        assertEquals("[action_bar_all_if_lang] ru-RU text",
                LocalizedTextCommands.preprocess("[action_bar_all-ru_RU] text"));
        assertTrue(EventHandler.decoder().decode(YamlValue.wrap(Map.of("messages-ru_???", "bad"))).hasError());
        assertEquals("[console] say hello", LocalizedTextCommands.preprocess("[console] say hello"));
    }

    @Test
    void localizedActionBarAndTitleApplyTheGateBeforeRendering() throws Exception {
        var locale = new AtomicReference<>(Locale.US);
        var bars = new ArrayList<Component>();
        var titles = new ArrayList<Title>();
        Player viewer = (Player) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("locale")) return locale.get();
                    if (method.getName().equals("sendActionBar")) bars.add((Component) args[0]);
                    if (method.getName().equals("showTitle")) titles.add((Title) args[0]);
                    return null;
                });
        var ctx = new EventContext(false, viewer, PlaceholderResolver.of("value", "7"))
                .setCommand(commands(List.of(viewer)));
        var handler = EventHandler.decoder().decode(YamlValue.wrap(List.of(
                Map.of("action_bar-en_US", "bar {value}"),
                Map.of("title-en_US", "\"Title {value}\" {value} 20 5")))).getOrThrow();
        var serverField = Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        Object previousServer = serverField.get(null);
        Server server = (Server) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Server.class, VersionedTestServer.class},
                (proxy, method, args) -> null);
        serverField.set(null, server);
        try {
            handler.handle(ctx);
            locale.set(new Locale("ru", "RU"));
            handler.handle(ctx);
        } finally {
            serverField.set(null, previousServer);
        }
        assertEquals(List.of(Component.text("bar 7")), bars);
        assertEquals(1, titles.size());
        assertEquals(Component.text("Title 7"), titles.get(0).title());
        assertEquals(java.time.Duration.ofMillis(350), titles.get(0).times().fadeIn());
    }
}
