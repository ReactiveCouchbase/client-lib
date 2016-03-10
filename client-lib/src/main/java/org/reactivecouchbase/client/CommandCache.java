package org.reactivecouchbase.client;

public interface CommandCache {

    Object get(String key);
    void put(String key, Object value);
    void cleanUp();
}
