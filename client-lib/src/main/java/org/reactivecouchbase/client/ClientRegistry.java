package org.reactivecouchbase.client;

import com.google.common.collect.ImmutableList;
import org.reactivecouchbase.concurrent.Future;
import org.reactivecouchbase.functional.Option;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ClientRegistry {

    private final ConcurrentHashMap<String, ServiceDescriptor> serviceCache = new ConcurrentHashMap<String, ServiceDescriptor>();

    public List<ServiceDescriptor> allServices() {
        return ImmutableList.copyOf(serviceCache.values());
    }

    public List<ServiceDescriptor> services(String name) {
        return services(name, Option.<String>none(), ImmutableList.<String>of());
    }

    public List<ServiceDescriptor> services(String name, String version) {
        return services(name, Option.apply(version), ImmutableList.<String>of());
    }

    public List<ServiceDescriptor> services(String name, String version, List<String> roles) {
        return services(name, Option.apply(version), ImmutableList.copyOf(roles));
    }

    public List<ServiceDescriptor> services(final String name, final Option<String> version, final ImmutableList<String> roles) {
        return allServices().stream().filter(input -> {
            if (!input.name.equals(name)) return false;
            if (version.isDefined() && !version.equals(input.version)) return false;
            if (!roles.isEmpty()) {
                for (String role : input.roles) {
                    if (!roles.contains(role)) return false;
                }
            }
            return true;
        }).collect(Collectors.toList());
    }

    public Option<ServiceDescriptor> service(String name) {
        return service(name, Option.<String>none(), ImmutableList.<String>of());
    }

    public Option<ServiceDescriptor> service(String name, String version) {
        return service(name, Option.apply(version), ImmutableList.<String>of());
    }

    public Option<ServiceDescriptor> service(String name, String version, List<String> roles) {
        return service(name, Option.apply(version), ImmutableList.copyOf(roles));
    }

    public Option<ServiceDescriptor> service(String name, Option<String> version, ImmutableList<String> roles) {
        List<ServiceDescriptor> services = services(name, version, roles);
        if (services.isEmpty()) {
            return Option.none();
        }
        return Option.apply(services.get(0));
    }

    public Client client(String name) {
        return client(name, Option.<String>none(), ImmutableList.<String>of());
    }

    public Client client(String name, String version) {
        return client(name, Option.apply(version), ImmutableList.<String>of());
    }

    public Client client(String name, List<String> roles) {
        return client(name, Option.<String>none(), ImmutableList.copyOf(roles));
    }

    public Client client(String name, String version, List<String> roles) {
        return client(name, Option.apply(version), ImmutableList.copyOf(roles));
    }

    public Client client(String name, Option<String> version, ImmutableList<String> roles) {
        return new LoadbalancedClient(name, version, roles, this);
    }

    public Registration register(final ServiceDescriptor desc) {
        if (!serviceCache.containsKey(desc.uid)) {
            serviceCache.putIfAbsent(desc.uid, desc);
        }
        return () -> serviceCache.remove(desc.uid);
    }

    public static class LoadbalancedClient implements Client {

        private final String name;
        private final Option<String> version;
        private final ImmutableList<String> roles;
        private final ClientRegistry registry;
        private final AtomicLong counter = new AtomicLong(0);

        private LoadbalancedClient(String name, Option<String> version, ImmutableList<String> roles, ClientRegistry registry) {
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
}
