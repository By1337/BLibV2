package dev.by1337.edsl;

import dev.by1337.cmd.Command;
import dev.by1337.edsl.command.args.ArgumentRawText;
import dev.by1337.edsl.context.EventContext;
import dev.by1337.edsl.messages.Messages;
import dev.by1337.yaml.YamlValue;
import net.kyori.adventure.text.Component;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class MessagesDynamicThemeTest extends EventDslTest {
    @TempDir
    Path directory;

    @Test
    void messagesDecodeOnceWithoutApplyingThemesToFile() throws Exception {
        Files.writeString(directory.resolve("messages.yml"), "$lang: ru_RU\ngreeting: '[message] {label}'\n");
        Plugin plugin = (Plugin) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ Plugin.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "getDataFolder" -> directory.toFile();
                    case "getSLF4JLogger" -> LoggerFactory.getLogger("test");
                    case "getName" -> "TestPlugin";
                    default -> null;
                });
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<String> selected = new AtomicReference<>("first");
        Themes.setSelector(player -> selected.get());
        Themes.setThemeReplacer(id -> text -> {
            calls.incrementAndGet();
            return text.replace("{label}", id);
        });
        try {
            Messages messages = Messages.decoder("messages.yml").decode(YamlValue.wrap(Map.of()), plugin).getOrThrow();
            assertEquals(0, calls.get());
            AtomicReference<Component> output = new AtomicReference<>();
            Command<EventContext> commands = new Command<EventContext>("root")
                    .sub(new Command<EventContext>("[message]").executor(new ArgumentRawText<>("text"),
                            (ctx, text) -> output.set(text.asComponent(ctx))));
            EventContext context = new EventContext(false).setCommand(commands);
            messages.handle("greeting", context);
            assertEquals(Component.text("first"), output.get());
            selected.set("second");
            messages.handle("greeting", context);
            assertEquals(Component.text("second"), output.get());
            assertEquals(2, calls.get());
        } finally {
            Themes.setSelector(null);
            Themes.setThemeReplacer(null);
        }
    }
}
