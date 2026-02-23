package dev.by1337.edsl.check;

import dev.by1337.cmd.Command;
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

public interface CheckHandler<T> extends EventHandler<T> {
    static <T> YamlCodec<CheckHandler<T>> codec(boolean oneOf) {
        return new YamlCodec<>() {
            @Override
            public DataResult<CheckHandler<T>> decode(YamlValue yaml) {
                if (yaml.isList()) return yaml.asList(this)
                        .mapValue(list -> new CheckHandler<T>() {
                            @Override
                            public boolean handle(T ctx, PlaceholderApplier placeholders, Command<T> executor) {
                                boolean state = true;
                                for (CheckHandler<T> handler : list) {
                                    if (handler.handle(ctx, placeholders, executor)) {
                                        if (oneOf) return true;
                                    } else if (oneOf) {
                                        state = false;
                                    } else {
                                        return false;
                                    }
                                }
                                return state;
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

    CheckHandler<Void> TRUE = new CheckHandler<Void>() {
        @Override
        public boolean handle(Void ctx, PlaceholderApplier placeholders, Command<Void> executor) {
            return true;
        }

        @Override
        public YamlValue encode() {
            return YamlValue.wrap("true");
        }
    };
    CheckHandler<Void> FALSE = new CheckHandler<Void>() {
        @Override
        public boolean handle(Void ctx, PlaceholderApplier placeholders, Command<Void> executor) {
            return false;
        }

        @Override
        public YamlValue encode() {
            return YamlValue.wrap("false");
        }
    };

    static <T> CheckHandler<T> nopTrue() {
        // noinspection unchecked
        return (CheckHandler<T>) TRUE;
    }

    static <T> CheckHandler<T> nopFalse() {
        // noinspection unchecked
        return (CheckHandler<T>) FALSE;
    }
}
