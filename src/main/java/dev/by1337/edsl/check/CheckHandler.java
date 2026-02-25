package dev.by1337.edsl.check;

import dev.by1337.cmd.Command;
import dev.by1337.edsl.EventContext;
import dev.by1337.edsl.EventHandler;
import dev.by1337.edsl.check.impl.MathRequirement;
import dev.by1337.edsl.check.impl.StringsRequirement;
import dev.by1337.plc.PlaceholderApplier;
import dev.by1337.yaml.YamlValue;
import dev.by1337.yaml.codec.DataResult;
import dev.by1337.yaml.codec.YamlCodec;
import dev.by1337.yaml.codec.schema.SchemaType;
import dev.by1337.yaml.codec.schema.SchemaTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public interface CheckHandler<T extends EventContext> extends EventHandler<T> {
    static <T extends EventContext> YamlCodec<CheckHandler<T>> codec(boolean oneOf) {
        return new YamlCodec<>() {
            @Override
            public DataResult<CheckHandler<T>> decode(YamlValue yaml) {
                if (yaml.isList()) return yaml.asList(this)
                        .mapValue(list -> new CheckHandler<T>() {
                            @Override
                            public boolean handle(T ctx, PlaceholderApplier placeholders, Command<T> executor) {
                                try (var enter = ctx.enter("if-list oneOf: " + oneOf, "if-list -> %s")) {
                                    boolean state = true;
                                    for (CheckHandler<T> handler : list) {
                                        if (handler.handle(ctx, placeholders, executor)) {
                                            if (oneOf) {
                                                enter.result(true);
                                                return true;
                                            }
                                        } else if (oneOf) {
                                            enter.result(false);
                                            state = false;
                                        } else {
                                            enter.result(false);
                                            return false;
                                        }
                                    }
                                    return state;
                                }
                            }

                            @Override
                            public @Nullable YamlValue encode() {
                                List<Object> result = new ArrayList<>();
                                for (CheckHandler<T> handler : list) {
                                    var v = handler.encode();
                                    if (v != null) result.add(v.getRaw());
                                }
                                return !result.isEmpty() ? YamlValue.wrap(result) : null;

                            }
                        })
                        ;
                return YamlCodec.STRING.decode(yaml).flatMap(input -> {
                    if (input.equalsIgnoreCase("true")) return DataResult.success(CheckHandler.nopTrue());
                    if (input.equalsIgnoreCase("false")) return DataResult.success(CheckHandler.nopFalse());
                    String[] args = input.split(" ");
                    if (args.length == 3) {
                        var op = StringsRequirement.Operation.byOperator(args[1]);
                        var v = new MathRequirement<>(input);
                        if (op == null)
                            return DataResult.success(tryMutate(new MathRequirement<>(input), MathRequirement::compile));
                        return DataResult.success(tryMutate(new StringsRequirement<>(op, args[0], args[2]), StringsRequirement::compile));
                    } else {
                        return DataResult.success(tryMutate(new MathRequirement<>(input), MathRequirement::compile));
                    }
                });
            }

            private <E extends CheckHandler<T>> CheckHandler<T> tryMutate(E in, Function<E, CheckHandler<T>> mapper) {
                CheckHandler<T> out = mapper.apply(in);
                return out != null ? out : in;
            }

            @Override
            public YamlValue encode(CheckHandler<T> c) {
                return c.encode();
            }

            @Override
            public @NotNull SchemaType schema() {
                return SchemaTypes.ANY;
            }
        };
    }

    CheckHandler<EventContext> TRUE = new CheckHandler<EventContext>() {
        @Override
        public boolean handle(EventContext ctx, PlaceholderApplier placeholders, Command<EventContext> executor) {
            ctx.log("if 'true' -> true");
            return true;
        }

        @Override
        public YamlValue encode() {
            return YamlValue.wrap("true");
        }
    };
    CheckHandler<EventContext> FALSE = new CheckHandler<EventContext>() {
        @Override
        public boolean handle(EventContext ctx, PlaceholderApplier placeholders, Command<EventContext> executor) {
            ctx.log("if 'false' -> false");
            return false;
        }

        @Override
        public YamlValue encode() {
            return YamlValue.wrap("false");
        }
    };

    static <T extends EventContext> CheckHandler<T> nopTrue() {
        // noinspection unchecked
        return (CheckHandler<T>) TRUE;
    }

    static <T extends EventContext> CheckHandler<T> nopFalse() {
        // noinspection unchecked
        return (CheckHandler<T>) FALSE;
    }

    static <T extends EventContext> CheckHandler<T> nopTrue(String exp, Object... args) {
        return nopTrue(String.format(exp, args));
    }
    static <T extends EventContext> CheckHandler<T> nopTrue(String exp) {
        return new CheckHandler<T>() {
            @Override
            public boolean handle(T ctx, PlaceholderApplier placeholders, Command<T> executor) {
                ctx.log("if '%s' -> true", exp);
                return true;
            }

            @Override
            public YamlValue encode() {
                return YamlValue.wrap("true");
            }
        };
    }

    static <T extends EventContext> CheckHandler<T> nopFalse(String exp, Object... args) {
        return nopFalse(String.format(exp, args));
    }
    static <T extends EventContext> CheckHandler<T> nopFalse(String exp) {
        return new CheckHandler<T>() {
            @Override
            public boolean handle(T ctx, PlaceholderApplier placeholders, Command<T> executor) {
                ctx.log("if '%s' -> false", exp);
                return false;
            }

            @Override
            public YamlValue encode() {
                return YamlValue.wrap("false");
            }
        };
    }
}
