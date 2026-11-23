package dev.by1337.core.command.bcmd;

import dev.by1337.cmd.Command;
import dev.by1337.core.command.bcmd.argument.ArgumentBool;
import dev.by1337.core.command.bcmd.argument.ArgumentInt;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class CommandMessagesTest {
    @TempDir
    Path directory;

    private Plugin plugin() {
        return (Plugin) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ Plugin.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "getDataFolder" -> directory.toFile();
                    case "getResource" -> getClass().getResourceAsStream("/" + args[0]);
                    case "getSLF4JLogger" -> LoggerFactory.getLogger(getClass());
                    case "getName" -> "BLib";
                    default -> null;
                });
    }

    @Test
    void copiesMessagesAndPreservesCustomization() throws Exception {
        CommandMessages.load(plugin());
        Path file = directory.resolve("messages.yml");
        assertTrue(Files.readString(file).contains("invalid-boolean:"));
        String customized = "invalid-boolean: 'Custom {input}'\n";
        Files.writeString(file, customized);
        CommandMessages.load(plugin());
        assertEquals(customized, Files.readString(file));
    }

    @Test
    void wrapperReportsExecutionErrorsAndKeepsCompletionSilent() {
        var output = new ArrayList<String>();
        CommandSender sender = (CommandSender) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{ CommandSender.class }, (proxy, method, args) -> {
                    if (method.getName().equals("sendMessage") && args[0] instanceof String text) output.add(text);
                    return null;
                });
        var wrapper = new CommandWrapper(new Command<CommandSender>("test")
                .executor(new ArgumentBool<>("flag"), (ctx, flag) -> fail("Must not execute")), plugin());
        assertFalse(wrapper.execute(sender, "test", new String[]{ "invalid" }));
        assertEquals(1, output.size());
        assertTrue(output.get(0).contains("invalid"));
        output.clear();
        assertTrue(wrapper.tabComplete(sender, "test", new String[]{ "invalid" }).isEmpty());
        var numeric = new CommandWrapper(new Command<CommandSender>("number")
                .executor(new ArgumentInt<>("value"), (ctx, value) -> {
                }), plugin());
        assertTrue(numeric.tabComplete(sender, "number", new String[]{ "invalid" }).isEmpty());
        assertTrue(output.isEmpty());
    }
}
