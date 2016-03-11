package org.reactivecouchbase.client;

import com.google.common.collect.ImmutableList;
import org.reactivecouchbase.concurrent.Future;
import org.reactivecouchbase.functional.Option;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

public class LoadbalancedClient implements Client {

    private final String name;
    private final Option<String> version;
    private final ImmutableList<String> roles;
    private final ClientRegistry registry;
    private final AtomicLong counter = new AtomicLong(0);

    LoadbalancedClient(String name, Option<String> version, ImmutableList<String> roles, ClientRegistry registry) {
        this.name = name;
        this.version = version;
        this.roles = roles;
        this.registry = registry;
    }

    public Option<ServiceDescriptor> bestService() {
        List<ServiceDescriptor> services = registry.services(name, version, roles);
        if (services.isEmpty()) return Option.none();
        int size = services.size();
        Long index = (counter.incrementAndGet() % (size > 0 ? size : 1));
        return Option.apply(services.get(index.intValue()));
    }

    @Override
    public <T> Future<T> call(final Function<ServiceDescriptor, T> f) {
        return bestService().map(input -> {
            return Future.successful(f.apply(input));
        }).getOrElse(Future.<T>failed(new ServiceDescNotFoundException("Service not found " + name + ", " + version + ", " + roles)));
    }

    @Override
    public <T> Future<T> callM(final Function<ServiceDescriptor, Future<T>> f) {
        return bestService().map(f::apply).getOrElse(Future.<T>failed(new ServiceDescNotFoundException("Service not found " + name + ", " + version + ", " + roles)));
    }
}
