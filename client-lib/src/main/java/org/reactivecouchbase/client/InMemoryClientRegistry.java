package org.reactivecouchbase.client;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryClientRegistry implements ClientRegistry {

    private final ConcurrentHashMap<String, ServiceDescriptor> serviceCache = new ConcurrentHashMap<>();

    public List<ServiceDescriptor> allServices() {
        return ImmutableList.copyOf(serviceCache.values());
    }

    public Registration register(final ServiceDescriptor desc) {
        if (!serviceCache.containsKey(desc.uid)) {
            serviceCache.putIfAbsent(desc.uid, desc);
        }
        return () -> serviceCache.remove(desc.uid);
    }

}
