package dev.by1337.core.event;

import com.sun.management.ThreadMXBean;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Logger;

/** Standalone diagnostic microbenchmark, deliberately outside production and the test suite. */
public final class EventBusBenchmark {
    private static final String EVENT = "economy:balance";
    private static final UUID PLAYER = new UUID(1, 2);
    private static final int WARMUP_ROUNDS = 5;
    private static final int MEASUREMENT_ROUNDS = 7;
    private static final int ITERATIONS = 5_000_000;
    private static volatile long sink;

    public static void main(String[] args) {
        ThreadMXBean memory = ManagementFactory.getPlatformMXBean(ThreadMXBean.class);
        if (memory != null && memory.isThreadAllocatedMemorySupported()) {
            memory.setThreadAllocatedMemoryEnabled(true);
        } else {
            memory = null;
        }
        System.out.println(System.getProperty("java.vm.name") + " " + System.getProperty("java.version"));
        System.out.printf("warmup=%d, samples=%d, emits/sample=%d%n", WARMUP_ROUNDS, MEASUREMENT_ROUNDS, ITERATIONS);
        for (int subscribers : new int[]{ 0, 1, 5, 20 }) run(subscribers, memory);
    }

    private static void run(int count, ThreadMXBean memory) {
        try (var bus = new EventBus(Logger.getAnonymousLogger())) {
            long[] counters = new long[count];
            for (int i = 0; i < count; i++) {
                final int index = i;
                bus.on(EVENT, id -> counters[index] += id.getLeastSignificantBits());
            }
            for (int i = 0; i < WARMUP_ROUNDS; i++) emit(bus);
            double[] times = new double[MEASUREMENT_ROUNDS];
            long allocated = 0;
            long thread = Thread.currentThread().getId();
            for (int i = 0; i < MEASUREMENT_ROUNDS; i++) {
                long beforeBytes = memory == null ? 0 : memory.getThreadAllocatedBytes(thread);
                long start = System.nanoTime();
                emit(bus);
                long elapsed = System.nanoTime() - start;
                if (memory != null) allocated += memory.getThreadAllocatedBytes(thread) - beforeBytes;
                times[i] = (double) elapsed / ITERATIONS;
            }
            long checksum = 0;
            long expected = 2L * (WARMUP_ROUNDS + MEASUREMENT_ROUNDS) * ITERATIONS;
            for (long counter : counters) {
                if (counter != expected) throw new AssertionError("Wrong delivery count: " + counter);
                checksum += counter;
            }
            sink = checksum;
            Arrays.sort(times);
            System.out.printf(Locale.ROOT, "%2d subscribers: median=%.2f ns/emit, min=%.2f, max=%.2f, allocated=%s B/emit, checksum=%d%n",
                    count, times[times.length / 2], times[0], times[times.length - 1],
                    memory == null ? "unavailable" : String.format(Locale.ROOT, "%.6f", (double) allocated / (MEASUREMENT_ROUNDS * (long) ITERATIONS)), sink);
        }
    }

    private static void emit(EventBus bus) {
        for (int i = 0; i < ITERATIONS; i++) bus.emit(EVENT, PLAYER);
    }
}
