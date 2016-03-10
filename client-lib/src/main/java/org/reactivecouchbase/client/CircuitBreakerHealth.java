package org.reactivecouchbase.client;

import org.reactivecouchbase.common.Duration;
import org.reactivecouchbase.common.MeasuredRate;

import java.util.concurrent.atomic.AtomicLong;

public class CircuitBreakerHealth {

    private final AtomicLong totalTime = new AtomicLong(0);

    private final MeasuredRate totalRequests;

    private final MeasuredRate totalErrorRequests;

    private final MeasuredRate totalSuccessRequests;

    public CircuitBreakerHealth(Duration windowDuration) {
        this.totalRequests = new MeasuredRate(windowDuration.toMillis());
        this.totalErrorRequests = new MeasuredRate(windowDuration.toMillis());
        this.totalSuccessRequests = new MeasuredRate(windowDuration.toMillis());
    }

    public void incrementTotalRequests() {
        totalRequests.increment();
    }

    public void incrementErrors() {
        totalErrorRequests.increment();
    }

    public void incrementTotalTime(Duration duration) {
        totalTime.addAndGet(duration.toMillis());
    }

    public void incrementSuccesses() {
        totalSuccessRequests.increment();
    }

    public long getTotalRequests() {
        return totalRequests.getCount();
    }

    public double getErrorPercentage() {
        return (totalErrorRequests.getCount() * 100.0) / totalRequests.getCount();
    }

    public void reset() {
        totalTime.set(0);
        totalRequests.reset();
        totalErrorRequests.reset();
        totalSuccessRequests.reset();
    }
}
