package org.reactivecouchbase.client;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.reactivecouchbase.common.Duration;

import java.util.concurrent.TimeUnit;

public class InMemoryCommandCache implements CommandCache {

    private final Cache<String, Object> cache;

    private InMemoryCommandCache(Duration retained) {
        this.cache = CacheBuilder.newBuilder().expireAfterAccess(retained.toMillis(), TimeUnit.MILLISECONDS).build();
    }

    public static InMemoryCommandCache of(Duration d) {
        return new InMemoryCommandCache(d);
    }

    @Override
    public Object get(String key) {
        return cache.getIfPresent(key);
    }

    @Override
    public void put(String key, Object value) {
        cache.put(key, value);
    }

    @Override
    public void cleanUp() {
        cache.cleanUp();
    }
}
