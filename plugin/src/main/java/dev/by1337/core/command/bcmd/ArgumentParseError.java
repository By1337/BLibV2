package dev.by1337.core.command.bcmd;

import dev.by1337.cmd.CommandMsgError;
import dev.by1337.edsl.context.EventContext;
import dev.by1337.plc.PlaceholderResolver;

/** A supplied argument is invalid. Missing arguments are left absent from ArgumentMap. */
public final class ArgumentParseError extends CommandMsgError {
    private final String messageKey;
    private final String argument;
    private final String input;
    private final String expected;

    public ArgumentParseError(String messageKey, String argument, String input, String expected) {
        super("Invalid " + argument + ": " + input + ". Expected: " + expected);
        this.messageKey = messageKey;
        this.argument = argument;
        this.input = input;
        this.expected = expected;
    }

    public String messageKey() {
        return messageKey;
    }

    public PlaceholderResolver<EventContext> placeholders() {
        return PlaceholderResolver.<EventContext>of("argument", argument)
                .append("input", input)
                .append("expected", expected);
    }
}
