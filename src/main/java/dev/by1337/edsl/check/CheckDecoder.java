package dev.by1337.edsl.check;

import dev.by1337.cmd.Command;
import dev.by1337.edsl.HandlerCodecBuilder;
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

public class CheckDecoder<T> {

    public DataResult<CheckHandler<T>> decode(YamlValue in) {
        return YamlCodec.STRING.decode(in).flatMap(input -> {
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

    private <R extends CheckHandler<T>> CheckHandler<T> tryMutate(R in, Function<R, CheckHandler<T>> mapper) {
        var mapped = mapper.apply(in);
        return mapped != null ? mapped : in;
    }

    public YamlCodec<CheckHandler<T>> createCodec(boolean oneOf) {
        return new YamlCodec<>() {
            @Override
            public DataResult<CheckHandler<T>> decode(YamlValue yaml) {
                if (!yaml.isList()) return CheckDecoder.this.decode(yaml);
                return yaml.asList(this).mapValue(list -> {
                   final List<CheckHandler<T>> handlers = list;
                    return new CheckHandler<>() {

                        @Override
                        public boolean handle(T ctx, PlaceholderApplier placeholders, Command<T> executor) {
                            boolean state = true;
                            for (CheckHandler<T> handler : handlers) {
                                if (handler.handle(ctx, placeholders, executor)) {
                                    if (oneOf) return true;
                                } else {
                                    state = false;
                                }
                            }
                            return state;
                        }

                        @Override
                        public @Nullable YamlValue encode() {
                            List<Object> result = new ArrayList<>();
                            for (CheckHandler<T> handler : handlers) {
                                var v = handler.encode();
                                if (v != null) result.add(v.getRaw());
                            }
                            return !result.isEmpty() ? YamlValue.wrap(result) : null;
                        }
                    };
                });
            }

            @Override
            public YamlValue encode(CheckHandler<T> check) {
                return check.encode();
            }

            @Override
            public @NotNull SchemaType schema() {
                return SchemaTypes.ANY;
            }
        };
    }
}
