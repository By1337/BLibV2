package dev.by1337.edsl;

import dev.by1337.cmd.Command;

public class EventContextTest extends EventContext<EventContextTest> {
    @Override
    public Command<EventContextTest> getCommand() {
        return null;
    }
}
