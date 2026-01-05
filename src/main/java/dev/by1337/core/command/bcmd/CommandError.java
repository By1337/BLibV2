package dev.by1337.core.command.bcmd;

import dev.by1337.cmd.CommandMsgError;
import dev.by1337.core.util.text.MessageFormatter;

public class CommandError extends CommandMsgError {

    public CommandError(String message) {
        super(message);
    }

    public CommandError(String message, Object ... args) {
        super(MessageFormatter.apply(message, message));
    }
}
