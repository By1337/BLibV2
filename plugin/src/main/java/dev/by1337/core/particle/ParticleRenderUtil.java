package dev.by1337.core.particle;

import dev.by1337.core.bridge.registry.LegacyRegistryBridge;
import dev.by1337.particle.BlockType;
import dev.by1337.particle.ItemType;
import dev.by1337.particle.ParticleType;
import dev.by1337.yaml.YamlValue;
import dev.by1337.yaml.codec.DataResult;
import dev.by1337.yaml.codec.YamlCodec;
import dev.by1337.yaml.codec.k2v.Key2ValueCodec;
import dev.by1337.yaml.codec.k2v.WildcardLookupCodec;
import dev.by1337.yaml.codec.schema.SchemaType;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ParticleRenderUtil {
    public static final YamlCodec<BlockType> BLOCK_TYPE_CODEC = new EnumLookupCodec<>(
            BlockType.class,
            (v, c) -> {
                c.accept(v.id());
                c.accept(v.id().split(":")[1]);
            }
    );
    public static final YamlCodec<ItemType> ITEM_TYPE_CODEC = new EnumLookupCodec<>(
            ItemType.class,
            (v, c) -> {
                c.accept(v.id());
                c.accept(v.id().split(":")[1]);
            }
    );

    public static final YamlCodec<ParticleType> PARTICLE_TYPE_CODEC = new EnumLookupCodec<>(
            ParticleType.class,
            (v, c) -> {
                c.accept(v.id());
                c.accept(v.id().split(":")[1]);
            }
    );

    public static ParticleType adaptParticle(Particle particle) {
        return PARTICLE_TYPE_CODEC.decode(
                LegacyRegistryBridge.PARTICLE_TYPE.getKey(particle)
        ).result();
    }

    public static BlockType adaptBlock(Material material) {
        return BLOCK_TYPE_CODEC.decode(material.getKey().asString()).result();
    }

    public static ItemType adaptItem(Material material) {
        return ITEM_TYPE_CODEC.decode(material.getKey().asString()).result();
    }

    private static class EnumLookupCodec<V extends Enum<V>> implements Key2ValueCodec<V> {
        private final Map<String, V> k2v = new HashMap<>();
        private final Map<V, String> v2k;
        private SchemaType schemaType;

        public EnumLookupCodec(Class<V> type, BiConsumer<V, Consumer<String>> applier) {
            v2k = new EnumMap<>(type);
            for (V v : type.getEnumConstants()) {
                applier.accept(v, s -> {
                    v2k.put(v, s);
                    k2v.put(s, v);
                });
            }
        }

        public Map<String, V> asMap() {
            return this.k2v;
        }

        public WildcardLookupCodec<V> wildcard() {
            return new WildcardLookupCodec<>(this.k2v);
        }

        public DataResult<V> decode(YamlValue value) {
            return YamlCodec.STRING.decode(value).flatMap((s) -> {
                V v = this.k2v.get(s.toLowerCase());
                return v == null ? DataResult.error("Unknown key: " + s) : DataResult.success(v);
            });
        }

        public YamlValue encode(V value) {
            String key = this.v2k.get(value);
            return YamlValue.wrap(Objects.requireNonNullElseGet(key, () -> "Unknown value: " + value));
        }

        public @NotNull SchemaType schema() {
            return this.schemaType;
        }
    }

}
