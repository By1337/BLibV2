package dev.by1337.core.event;

import java.io.Closeable;

/** A subscription whose {@link #close()} is thread-safe, idempotent and never throws a checked exception. */
@FunctionalInterface
public interface Subscription extends Closeable {
    @Override
    void close();
}
