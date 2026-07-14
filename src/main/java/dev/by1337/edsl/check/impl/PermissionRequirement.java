package dev.by1337.edsl.check.impl;

import dev.by1337.edsl.check.CheckHandler;
import dev.by1337.edsl.context.EventContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record PermissionRequirement(String input) implements CheckHandler {

    private static final Logger log = LoggerFactory.getLogger(PermissionRequirement.class);

    @Override
    public boolean handle(EventContext ctx) {
        String s = ctx.setPlaceholders(input);
        var v = ctx.target();
        if (v == null) return false;
        return v.hasPermission(s);
    }

    public CheckHandler invert() {
        return ctx -> !handle(ctx);
    }

}
