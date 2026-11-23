package dev.by1337.core.bridge.registry;


import dev.by1337.yaml.BukkitCodecs;
import dev.by1337.yaml.YamlValue;
import dev.by1337.yaml.codec.DataResult;
import dev.by1337.yaml.codec.YamlCodec;
import dev.by1337.yaml.codec.schema.SchemaType;
import dev.by1337.yaml.codec.schema.SchemaTypes;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

public class LegacyRegistryBridge {
    public static final RegistryBridge<PotionEffectType> MOB_EFFECT = new RegistryImpl<>();
    public static final RegistryBridge<Particle> PARTICLE_TYPE = new RegistryImpl<>();


    public interface RegistryBridge<T> extends Registry<Holder<T>> {
        YamlCodec<T> yamlCodec();

        NamespacedKey getKey(T value);
    }

    public static class RegistryImpl<T> implements RegistryBridge<T> {
        private final Map<NamespacedKey, Holder<T>> key2value = new HashMap<>();
        private final Map<T, NamespacedKey> value2key = new IdentityHashMap<>();
        private SchemaType schemaType;
        private boolean frozen;

        @Override
        public @NotNull Iterator<Holder<T>> iterator() {
            return key2value.values().iterator();
        }

        @Override
        public @Nullable Holder<T> get(@NotNull NamespacedKey key) {
            return key2value.get(key);
        }

        @Override
        public NamespacedKey getKey(T value) {
            return value2key.get(value);
        }

        @ApiStatus.Internal
        public <R> void importData(Iterator<R> iterator, Function<R, T> map, Function<R, NamespacedKey> toKey) {
            if (frozen) throw new IllegalStateException("Registry already bootstrapped");
            while (iterator.hasNext()) {
                R v = iterator.next();
                T val = map.apply(v);
                NamespacedKey key = toKey.apply(v);
                key2value.put(toKey.apply(v), new Holder<>(val, key));
                value2key.put(val, key);
            }
            frozen = true;
        }

        public YamlCodec<T> yamlCodec() {
            return new YamlCodec<>() {
                @Override
                public DataResult<T> decode(YamlValue yamlValue) {
                    return yamlValue.decode(BukkitCodecs.namespaced_key()).flatMap(key -> {
                        Holder<T> holder = key2value.get(key);
                        if (holder == null) return DataResult.error("Unknown {}", key);
                        return DataResult.success(holder.value);
                    });
                }

                @Override
                public YamlValue encode(T t) {
                    NamespacedKey key = value2key.get(t);
                    if (key == null) {
                        return YamlValue.wrap("Failed to encode " + t);
                    }
                    return YamlValue.wrap(key.asString());
                }

                @Override
                public @NotNull SchemaType schema() {
                    if (schemaType != null) return schemaType;
                    return schemaType = SchemaTypes.enumOf(value2key.values().stream().map(NamespacedKey::getKey).toList());
                }
            };
        }
    }

    public static class Holder<T> implements Keyed {
        private final T value;
        private final NamespacedKey key;

        public Holder(T value, NamespacedKey key) {
            this.value = value;
            this.key = key;
        }

        public Holder(T value) {
            this.value = value;
            if (value instanceof Keyed keyed) {
                key = keyed.getKey();
            } else {
                throw new IllegalArgumentException(value + " must be Keyed");
            }
        }

        public T value() {
            return value;
        }

        @Override
        @NotNull
        public NamespacedKey getKey() {
            return key;
        }
    }
}
