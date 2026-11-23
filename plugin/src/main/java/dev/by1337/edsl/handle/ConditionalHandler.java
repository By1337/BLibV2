package dev.by1337.edsl.handle;

import dev.by1337.edsl.context.EventContext;
import dev.by1337.edsl.EventHandler;
import dev.by1337.edsl.check.CheckHandler;
import dev.by1337.yaml.decoder.RecordYamlDecoder;
import dev.by1337.yaml.decoder.YamlDecoder;

import java.util.Objects;

public class ConditionalHandler implements EventHandler {
    private final CheckHandler tester;
    private final EventHandler success;
    private final EventHandler failure;

    private ConditionalHandler(CheckHandler tester, EventHandler success, EventHandler failure) {
        this.tester = Objects.requireNonNullElse(tester, CheckHandler.nopTrue());
        this.success = success;
        this.failure = failure;
    }

    @Override
    public boolean handle(EventContext ctx) {
        try (var enter = ctx.tracer().enter("if-do {", "} -> %s")) {
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


    public static YamlDecoder<ConditionalHandler> decoder(YamlDecoder<EventHandler> codec) {
        return decoder(codec, CheckHandler.decoder());
    }

    public static YamlDecoder<ConditionalHandler> decoder(YamlDecoder<EventHandler> codec, YamlDecoder<CheckHandler> ifDecoder) {
        return RecordYamlDecoder.mapOf(
                (condition, all, one, success, failure) -> new ConditionalHandler(
                        condition != null ? condition : all != null ? all : one,
                        success,
                        failure
                ),
                ifDecoder.fieldOf("if"),
                ifDecoder.fieldOf("if-all"),
                ifDecoder.fieldOf("if-one"),
                codec.fieldOf("do"),
                codec.fieldOf("else")
        );
    }
}
