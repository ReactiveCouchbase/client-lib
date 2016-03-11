package org.reactivecouchbase.client;

import com.google.common.collect.ImmutableList;
import org.reactivecouchbase.concurrent.Future;
import org.reactivecouchbase.functional.Option;

import java.util.List;
import java.util.function.Function;

public interface Client {

    <T> Future<T> call(Function<ServiceDescriptor, T> f);
    <T> Future<T> callM(Function<ServiceDescriptor, Future<T>> f);

    static Client client(ClientRegistry registry, String name) {
        return client(registry, name, Option.<String>none(), ImmutableList.<String>of());
    }

    static Client client(ClientRegistry registry, String name, String version) {
        return client(registry, name, Option.apply(version), ImmutableList.<String>of());
    }

    static Client client(ClientRegistry registry, String name, List<String> roles) {
        return client(registry, name, Option.<String>none(), ImmutableList.copyOf(roles));
    }

    static Client client(ClientRegistry registry, String name, String version, List<String> roles) {
        return client(registry, name, Option.apply(version), ImmutableList.copyOf(roles));
    }

    static Client client(ClientRegistry registry, String name, Option<String> version, ImmutableList<String> roles) {
        return new LoadbalancedClient(name, version, roles, registry);
    }

    static Client compose(ClientRegistry registry, String name, Option<String> version, ImmutableList<String> roles) {
        return new Client() {
            @Override
            public <T> Future<T> call(Function<ServiceDescriptor, T> f) {
                return null;
            }

            @Override
            public <T> Future<T> callM(Function<ServiceDescriptor, Future<T>> f) {
                return null;
            }
        };
    }
}
