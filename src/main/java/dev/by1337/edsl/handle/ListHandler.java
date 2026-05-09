package dev.by1337.edsl.handle;

import dev.by1337.edsl.EventHandler;
import dev.by1337.edsl.context.EventContext;

import java.util.List;

public class ListHandler<C extends EventContext<C>> implements EventHandler<C> {
    private final List<EventHandler<C>> list;

    public ListHandler(List<EventHandler<C>> list) {
        this.list = list;
    }

    @Override
    public boolean handle(C ctx) {
        if (list.isEmpty()) {
            ctx.log("[] -> true");
            return true;
        }
        boolean state = true;
        try (var enter = ctx.enter("list [", "] -> %s")) {
            enter.result(true);
            var iterator = list.iterator();
            while (iterator.hasNext()) {
                EventHandler<C> handler = iterator.next();
                if (!handler.handle(ctx)) {
                    enter.result(false);
                    state = false;
                }
                if (iterator.hasNext()) {
                    ctx.log("");
                }
            }
        }
        return state;
    }

    public List<EventHandler<C>> list() {
        return list;
    }
}
