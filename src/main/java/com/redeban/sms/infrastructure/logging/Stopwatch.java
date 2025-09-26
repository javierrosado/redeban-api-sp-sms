package com.redeban.sms.infrastructure.logging;

public final class Stopwatch {
    private final long startNanos;

    private Stopwatch() {
        this.startNanos = System.nanoTime();
    }

    public static Stopwatch start() {
        return new Stopwatch();
    }

    public long elapsedMs() {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
