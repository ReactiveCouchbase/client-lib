package org.reactivecouchbase.client;

import java.util.concurrent.atomic.AtomicBoolean;
import org.reactivecouchbase.common.Duration;

public class CircuitBreaker {

    public static enum Strategy {
        UNIQUE_PER_CONTEXT, UNIQUE_PER_COMMAND
    }

    private static final int circuitBreakerRequestVolumeThreshold = 1;
    private static final double circuitBreakerErrorThresholdPercentage = 50.0;
    private static final String circuitBreakerMetricsWindow = "10 sec";

    final CircuitBreakerHealth metrics = new CircuitBreakerHealth(Duration.parse(circuitBreakerMetricsWindow));
    private final AtomicBoolean circuitOpen = new AtomicBoolean(false);

    public void markSuccess(Duration duration) {
        if (circuitOpen.get()) {
            metrics.reset();
            circuitOpen.set(false);
        } else {
            metrics.incrementTotalRequests();
            metrics.incrementSuccesses();
            metrics.incrementTotalTime(duration);
        }
    }

    public void markFailure(Duration duration) {
        metrics.incrementTotalRequests();
        metrics.incrementErrors();
        metrics.incrementTotalTime(duration);
    }

    public boolean isOpen() {
        if (circuitOpen.get()) {
            return true;
        }
        if (metrics.getTotalRequests() < circuitBreakerRequestVolumeThreshold) {
            return false;
        }
        if (metrics.getErrorPercentage() < circuitBreakerErrorThresholdPercentage) {
            return false;
        } else {
            return circuitOpen.compareAndSet(false, true);
        }
    }

    public boolean allowRequest() {
        return !isOpen();
    }
}
