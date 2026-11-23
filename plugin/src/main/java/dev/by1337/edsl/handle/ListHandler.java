package dev.by1337.edsl.handle;

import dev.by1337.edsl.EventHandler;
import dev.by1337.edsl.context.EventContext;

import java.util.List;

public class ListHandler implements EventHandler {
    private final List<EventHandler> list;

    public ListHandler(List<EventHandler> list) {
        this.list = list;
    }

    @Override
    public boolean handle(EventContext ctx) {
        if (list.isEmpty()) {
            ctx.tracer().log("[] -> true");
            return true;
        }
        boolean state = true;
        try (var enter = ctx.tracer().enter("list [", "] -> %s")) {
            enter.result(true);
            var iterator = list.iterator();
            while (iterator.hasNext()) {
                EventHandler handler = iterator.next();
                if (!handler.handle(ctx)) {
                    enter.result(false);
                    state = false;
                }
                if (iterator.hasNext()) {
                    ctx.tracer().log("");
                }
            }
        }
        return state;
    }

    public List<EventHandler> list() {
        return list;
    }
}
