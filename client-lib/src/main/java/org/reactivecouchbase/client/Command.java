package org.reactivecouchbase.client;

import org.reactivecouchbase.common.Duration;
import org.reactivecouchbase.concurrent.Future;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class Command<T> {

    public static final Duration FOREVER = new Duration(Long.MAX_VALUE, TimeUnit.HOURS);

    public Future<T> runAsync(ScheduledExecutorService ec) {
        return Future.async(this::run, ec);
    }

    public T run() {
        return null;
    }

    public Duration timeout() {
        return Duration.parse("60 sec");
    }

    public String name() {
        return this.getClass().getName();
    }

    public T fallback() {
        return null;
    }

    public String cacheKey() {
        return null;
    }

    public String collapseKey() {
        return cacheKey();
    }

    public int retry() {
        return 0;
    }

    public boolean exponentialBackoff() {
        return true;
    }
}