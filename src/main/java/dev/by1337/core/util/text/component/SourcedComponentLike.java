package dev.by1337.core.util.text.component;

import dev.by1337.yaml.codec.YamlCodec;
import net.kyori.adventure.text.ComponentLike;

public interface SourcedComponentLike extends ComponentLike {
    YamlCodec<SourcedComponentLike> CODEC = YamlCodec.STRING
            .map(
                    SourcedComponentFactory::create,
                    SourcedComponentLike::source
            );
    YamlCodec<ComponentLike> COMPONENT_LIKE_CODEC = CODEC
            .map(s -> s, SourcedComponentFactory::of);
    String source();
}
