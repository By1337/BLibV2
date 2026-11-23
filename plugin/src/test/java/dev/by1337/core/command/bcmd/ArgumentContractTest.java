package dev.by1337.core.command.bcmd;

import dev.by1337.cmd.*;
import dev.by1337.core.command.bcmd.argument.*;
import org.junit.jupiter.api.Test;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import dev.by1337.core.util.math.FastExpressionParser;
import java.lang.reflect.Proxy;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ArgumentContractTest {
    private List<Argument<Object, ?>> arguments() {
        return List.of(new ArgumentBool<>("value"), new ArgumentChoice<>("value", List.of("alpha", "beta")),
                new ArgumentInt<>("value"), new ArgumentLong<>("value"), new ArgumentDouble<>("value"),
                new ArgumentPlayers<>("value"), new ArgumentRegistry<>("value", List.of()),
                new GlobalRegistryItem<>("value"));
    }

    @Test
    void missingArgumentsRemainNull() {
        for (var argument : arguments()) {
            var map = new ArgumentMap(1);
            argument.parse(null, new CommandReader(""), map);
            assertNull(map.get("value"), argument.getClass().getSimpleName());
        }
    }

    @Test
    void invalidInputThrowsUserFacingError() {
        for (var argument : arguments().subList(0, 5)) {
            var error = assertThrows(ArgumentParseError.class,
                    () -> argument.parse(null, new CommandReader("invalid"), new ArgumentMap(1)));
            assertInstanceOf(CommandMsgError.class, error);
            assertTrue(error.getMessage().contains("invalid"));
        }
        assertThrows(ArgumentParseError.class, () -> new ArgumentRegistry<>("value", List.of())
                .parse(null, new CommandReader("unknown"), new ArgumentMap(1)));
    }

    @Test
    void commandExecutesWithMissingValueAndStopsOnInvalidValue() {
        var called = new AtomicBoolean();
        var value = new AtomicReference<Boolean>(true);
        var command = new Command<Object>("test").executor(new ArgumentBool<>("flag"), (ctx, flag) -> {
            called.set(true);
            value.set(flag);
        });
        command.execute(null, "");
        assertTrue(called.get());
        assertNull(value.get());
        called.set(false);
        assertThrows(ArgumentParseError.class, () -> command.execute(null, "maybe"));
        assertFalse(called.get());
        command.execute(null, "FALSE");
        assertEquals(false, value.get());
    }

    @Test
    void numericExpressionsStillWork() {
        var map = new ArgumentMap(1);
        new ArgumentInt<>("value").parse(null, new CommandReader("2*8"), map);
        assertEquals(16, map.get("value"));
        new ArgumentLong<>("value").parse(null, new CommandReader("2*8"), map);
        assertEquals(16L, map.get("value"));
        new ArgumentDouble<>("value").parse(null, new CommandReader("5/2"), map);
        assertEquals(2.5, map.get("value"));
    }

    @Test
    void completionFiltersPartialInputWithoutRejectingIt() {
        var bool = new Command<Object>("test").executor(new ArgumentBool<>("flag"), (ctx, flag) -> {
        });
        assertEquals(List.of("true"), bool.suggest(null, "tr").toList());
        assertTrue(bool.suggest(null, "invalid").isEmpty());
        var choice = new Command<Object>("test").executor(
                new ArgumentChoice<>("value", List.of("alpha", "alpine", "beta")), (ctx, value) -> {
                });
        assertEquals(List.of("alpha", "alpine"), choice.suggest(null, "al").toList());
        assertTrue(choice.suggest(null, "unknown").isEmpty());
    }

    @Test
    void numericInputRejectsUnknownWordsAndNonFiniteResults() {
        for (String input : List.of("invalid", "1/0", "0/0", "1e", "2+")) {
            assertThrows(ArgumentParseError.class, () -> new ArgumentDouble<>("value")
                    .parse(null, new CommandReader(input), new ArgumentMap(1)), input);
        }
        // String hashing remains available to existing expression consumers.
        assertEquals("invalid".hashCode(), FastExpressionParser.parse("invalid"));
    }

    @Test
    void playersSuggestMatchingNamesAndRejectUnknownPlayersOnExecution() throws Exception {
        Player player = (Player) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ Player.class },
                (proxy, method, args) -> method.getName().equals("getName") ? "Alex" : null);
        Server server = (Server) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ Server.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "getOnlinePlayers" -> List.of(player);
                    case "getPlayerExact" -> "Alex".equalsIgnoreCase((String) args[0]) ? player : null;
                    default -> null;
                });
        var field = Bukkit.class.getDeclaredField("server");
        field.setAccessible(true);
        Object previous = field.get(null);
        field.set(null, server);
        try {
            var command = new Command<Object>("test").executor(new ArgumentPlayers<>("players"), (ctx, players) -> {
            });
            assertEquals(List.of("Alex"), command.suggest(null, "Al").toList());
            assertTrue(command.suggest(null, "Unknown").isEmpty());
            assertThrows(ArgumentParseError.class, () -> command.execute(null, "Unknown"));
            var map = new ArgumentMap(1);
            new ArgumentPlayers<>("players").parse(null, new CommandReader("*"), map);
            assertEquals(List.of(player), map.get("players"));
        } finally {
            field.set(null, previous);
        }
    }

    @Test
    void formattedCommandErrorUsesItsArguments() {
        assertEquals("Value: 42", new CommandError("Value: {}", 42).getMessage());
    }
}
