package org.reactivecouchbase.client;

import com.google.common.collect.ImmutableList;
import org.reactivecouchbase.concurrent.Future;
import org.reactivecouchbase.functional.Option;
import org.reactivecouchbase.functional.Unit;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

public interface AsyncClientRegistry {

    Future<List<ServiceDescriptor>> allServices(ExecutorService ec);
    Future<Registration> register(final ServiceDescriptor desc, ExecutorService ec);
    Future<Unit> unregister(final String uuid, ExecutorService ec);

    default Future<Unit> unregister(final ServiceDescriptor desc, ExecutorService ec) {
        return unregister(desc.uid, ec);
    }

    default Future<List<ServiceDescriptor>> services(String name, ExecutorService ec) {
        return services(name, Option.<String>none(), ImmutableList.<String>of(), ec);
    }

    default Future<List<ServiceDescriptor>> services(String name, String version, ExecutorService ec) {
        return services(name, Option.apply(version), ImmutableList.<String>of(), ec);
    }

    default Future<List<ServiceDescriptor>> services(String name, String version, List<String> roles, ExecutorService ec) {
        return services(name, Option.apply(version), ImmutableList.copyOf(roles), ec);
    }

    default Future<List<ServiceDescriptor>> services(final String name, final Option<String> version, final ImmutableList<String> roles, ExecutorService ec) {
        return allServices(ec).map(services ->
            services.stream().filter(input -> {
                if (!input.name.equals(name)) return false;
                if (version.isDefined() && !version.equals(input.version)) return false;
                if (!roles.isEmpty()) {
                    for (String role : input.roles) {
                        if (!roles.contains(role)) return false;
                    }
                }
                return true;
            }).collect(Collectors.toList()), ec);
    }

    default Future<Option<ServiceDescriptor>> service(String name, ExecutorService ec) {
        return service(name, Option.<String>none(), ImmutableList.<String>of(), ec);
    }

    default Future<Option<ServiceDescriptor>> service(String name, String version, ExecutorService ec) {
        return service(name, Option.apply(version), ImmutableList.<String>of(), ec);
    }

    default Future<Option<ServiceDescriptor>> service(String name, String version, List<String> roles, ExecutorService ec) {
        return service(name, Option.apply(version), ImmutableList.copyOf(roles), ec);
    }

    default Future<Option<ServiceDescriptor>> service(String name, Option<String> version, ImmutableList<String> roles, ExecutorService ec) {
        return services(name, version, roles, ec).map(services -> {
            if (services.isEmpty()) {
                return Option.none();
            }
            return Option.apply(services.get(0));
        }, ec);
    }
}