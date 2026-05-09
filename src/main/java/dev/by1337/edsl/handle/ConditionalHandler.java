package dev.by1337.edsl.handle;

import dev.by1337.edsl.context.EventContext;
import dev.by1337.edsl.EventHandler;
import dev.by1337.edsl.check.CheckHandler;
import dev.by1337.yaml.YamlValue;
import dev.by1337.yaml.codec.DataResult;
import dev.by1337.yaml.codec.RecordYamlCodecBuilder;
import dev.by1337.yaml.codec.YamlCodec;
import dev.by1337.yaml.decoder.RecordYamlDecoder;
import dev.by1337.yaml.decoder.YamlDecoder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class ConditionalHandler<C extends EventContext<C>> implements EventHandler<C> {
    private final CheckHandler<C> tester;
    private final EventHandler<C> success;
    private final EventHandler<C> failure;

    private ConditionalHandler(CheckHandler<C> tester, EventHandler<C> success, EventHandler<C> failure) {
        this.tester = Objects.requireNonNullElse(tester, CheckHandler.nopTrue());
        this.success = success;
        this.failure = failure;
    }

    @Override
    public boolean handle(C ctx) {
        try (var enter = ctx.enter("if-do {", "} -> %s")){
            if (tester.handle(ctx)) {
                if (success != null)
                    success.handle(ctx);
                enter.result(true);
                return true;
            }
            if (failure != null)
                failure.handle(ctx);
            enter.result(false);
            return false;
        }
    }


    public static <T extends EventContext<T>> YamlDecoder<ConditionalHandler<T>> decoder(YamlDecoder<EventHandler<T>> codec) {
        YamlDecoder<CheckHandler<T>> ifDecoder = CheckHandler.decoder();
        return RecordYamlDecoder.mapOf(
                ConditionalHandler::new,
                ifDecoder.fieldOf("if")
                        .or(ifDecoder.fieldOf("if-all"))
                        .or(ifDecoder.fieldOf("if-one")),
                codec.fieldOf("do"),
                codec.fieldOf("else")
        );
    }
}
