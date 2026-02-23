package dev.by1337.edsl;

import dev.by1337.edsl.check.CheckDecoder;
import dev.by1337.edsl.check.CheckHandler;
import dev.by1337.edsl.handle.HandlerDecoder;
import dev.by1337.yaml.codec.YamlCodec;

public class HandlerCodecBuilder<T> {
    private CheckDecoder<T> checkDecoder = new CheckDecoder<>();
    private HandlerDecoder<T> handlerDecoder = new HandlerDecoder<>(this);

    private final YamlCodec<CheckHandler<T>> checkCodec = YamlCodec.lazyLoad(() -> checkDecoder.createCodec(false));
    private final YamlCodec<CheckHandler<T>> checkCodecOneOf = YamlCodec.lazyLoad(() -> checkDecoder.createCodec(true));

    private final YamlCodec<EventHandler<T>> handlerCodec = YamlCodec.lazyLoad(() -> handlerDecoder.createCodec());

    public YamlCodec<CheckHandler<T>> checkCodec() {
        return checkCodec;
    }

    public HandlerDecoder<T> handlerDecoder() {
        return handlerDecoder;
    }

    public HandlerCodecBuilder<T> setHandlerDecoder(HandlerDecoder<T> handlerDecoder) {
        this.handlerDecoder = handlerDecoder;
        return this;
    }

    public YamlCodec<EventHandler<T>> handlerCodec() {
        return handlerCodec;
    }

    public CheckDecoder<T> checkDecoder() {
        return checkDecoder;
    }

    public HandlerCodecBuilder<T> setCheckDecoder(CheckDecoder<T> checkDecoder) {
        this.checkDecoder = checkDecoder;
        return this;
    }

    public YamlCodec<CheckHandler<T>> checkCodecOneOf() {
        return checkCodecOneOf;
    }
}
