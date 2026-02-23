package dev.by1337.edsl;

import dev.by1337.cmd.Command;

public abstract class EventContext<T extends EventContext<T>> {



    public abstract Command<T> getCommand();
}
