package dev.by1337.edsl.check;

import dev.by1337.edsl.EventHandler;
import dev.by1337.edsl.check.impl.MathRequirement;
import dev.by1337.edsl.check.impl.PermissionRequirement;
import dev.by1337.edsl.check.impl.StringsRequirement;
import dev.by1337.edsl.context.EventContext;
import dev.by1337.edsl.trace.EventTracer;
import dev.by1337.yaml.YamlValue;
import dev.by1337.yaml.codec.DataResult;
import dev.by1337.yaml.decoder.DecodeContext;
import dev.by1337.yaml.decoder.YamlDecoder;

import java.util.List;
import java.util.function.Function;

@FunctionalInterface
public interface CheckHandler<T extends EventContext<T>> extends EventHandler<T> {
    static <T extends EventContext<T>> YamlDecoder<CheckHandler<T>> decoder() {
        return new YamlDecoder<CheckHandler<T>>() {
            private final YamlDecoder<List<CheckHandler<T>>> list = this.listOf();

            @Override
            public DataResult<CheckHandler<T>> decode(DecodeContext ctx, YamlValue yaml) {
                var fromKey = ctx.getFromKey();
                boolean oneOf = fromKey != null && fromKey.equals("if-one");
                if (yaml.isCollection()) return list.decode(yaml).map(list -> c -> {
                    try (EventTracer.TracerDepth enter = c.enter(oneOf ? "if-one" : "if", "if-list -> %s")) {
                        boolean state = true;
                        for (CheckHandler<T> handler : list) {
                            if (handler.handle(c)) {
                                if (oneOf) {
                                    enter.result(true);
                                    return true;
                                }
                            } else if (!oneOf) {
                                enter.result(false);
                                return false;
                            }
                        }
                        return state;
                    }
                });

                return STRING.decode(ctx, yaml).flatMap(input -> {
                    if (input.equalsIgnoreCase("true")) return DataResult.success(CheckHandler.nopTrue());
                    if (input.equalsIgnoreCase("false")) return DataResult.success(CheckHandler.nopFalse());
                    String[] args = input.split(" ");
                    if (args.length == 2) {
                        if (args[0].equals("has")) {
                            return DataResult.success(new PermissionRequirement<>(args[1]));
                        } else if (args[0].equals("!has")) {
                            return DataResult.success(new PermissionRequirement<T>(args[1]).invert());
                        }
                    } else if (args.length == 3) {
                        var op = StringsRequirement.Operation.byOperator(args[1]);
                        if (op == null)
                            return DataResult.success(tryMutate(new MathRequirement<>(input), MathRequirement::compile));
                        return DataResult.success(tryMutate(new StringsRequirement<>(op, args[0], args[2]), StringsRequirement::compile));
                    }
                    return DataResult.success(tryMutate(new MathRequirement<>(input), MathRequirement::compile));
                });
            }

            private <E extends CheckHandler<T>> CheckHandler<T> tryMutate(E in, Function<E, CheckHandler<T>> mapper) {
                CheckHandler<T> out = mapper.apply(in);
                return out != null ? out : in;
            }
        };
    }

    static <T extends EventContext<T>> CheckHandler<T> nopTrue() {
        return ctx -> {
            ctx.log("if 'true' -> true");
            return true;
        };
    }

    static <T extends EventContext<T>> CheckHandler<T> nopFalse() {
        return ctx -> {
            ctx.log("if 'false' -> false");
            return true;
        };
    }

    static <T extends EventContext<T>> CheckHandler<T> nopTrue(String exp, Object... args) {
        return nopTrue(String.format(exp, args));
    }

    static <T extends EventContext<T>> CheckHandler<T> nopTrue(String exp) {
        return ctx -> {
            ctx.log("if '%s' -> true", exp);
            return true;
        };
    }

    static <T extends EventContext<T>> CheckHandler<T> nopFalse(String exp, Object... args) {
        return nopFalse(String.format(exp, args));
    }

    static <T extends EventContext<T>> CheckHandler<T> nopFalse(String exp) {
        return ctx -> {
            ctx.log("if '%s' -> false", exp);
            return false;
        };
    }
}
