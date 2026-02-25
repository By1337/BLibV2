package dev.by1337.edsl.check.impl;

import dev.by1337.cmd.Command;
import dev.by1337.core.util.math.FastExpressionParser;
import dev.by1337.edsl.EventContext;
import dev.by1337.edsl.check.CheckHandler;
import dev.by1337.plc.PlaceholderApplier;
import dev.by1337.yaml.YamlValue;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record MathRequirement<C extends EventContext>(String input) implements CheckHandler<C> {

    private static final Logger log = LoggerFactory.getLogger(MathRequirement.class);

    @Override
    public boolean handle(C ctx, PlaceholderApplier placeholders, Command<C> executor) {
        String s = placeholders.setPlaceholders(input);
        try {
            var v = FastExpressionParser.parse(s) == 1D;
            ctx.tracer().log("if '%s' -> %s", s, v);
            return v;
        } catch (FastExpressionParser.MathFormatException e) {
            log.error(
                    "Failed to parse math requirement. expression: '{}' replaced expression: '{}'\n{}",
                    input, s,
                    e.getMessage()
            );
            return false;
        }
    }

    public CheckHandler<C> invert() {
        return new MathRequirement<>("(" + input + ") == 0");
    }

    public @Nullable CheckHandler<C> compile() {
        //rnd, irnd
        if (input.contains("{") || input.contains("%") || input.contains("rnd")) return null;
        try {
            return FastExpressionParser.parse(input) == 1D ?
                    CheckHandler.nopTrue(input) :
                    CheckHandler.nopFalse(input);
        } catch (FastExpressionParser.MathFormatException e) {
            return null;
        }
    }

    @Override
    public YamlValue encode() {
        return YamlValue.wrap(input);
    }
}
