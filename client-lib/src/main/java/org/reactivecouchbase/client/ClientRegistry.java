package org.reactivecouchbase.client;

import com.google.common.collect.ImmutableList;
import org.reactivecouchbase.functional.Option;

import java.util.List;
import java.util.stream.Collectors;

public interface ClientRegistry {

    List<ServiceDescriptor> allServices();
    Registration register(final ServiceDescriptor desc);

    default List<ServiceDescriptor> services(String name) {
        return services(name, Option.<String>none(), ImmutableList.<String>of());
    }

    default List<ServiceDescriptor> services(String name, String version) {
        return services(name, Option.apply(version), ImmutableList.<String>of());
    }

    default List<ServiceDescriptor> services(String name, String version, List<String> roles) {
        return services(name, Option.apply(version), ImmutableList.copyOf(roles));
    }

    default List<ServiceDescriptor> services(final String name, final Option<String> version, final ImmutableList<String> roles) {
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

    default Option<ServiceDescriptor> service(String name) {
        return service(name, Option.<String>none(), ImmutableList.<String>of());
    }

    default Option<ServiceDescriptor> service(String name, String version) {
        return service(name, Option.apply(version), ImmutableList.<String>of());
    }

    default Option<ServiceDescriptor> service(String name, String version, List<String> roles) {
        return service(name, Option.apply(version), ImmutableList.copyOf(roles));
    }

    default Option<ServiceDescriptor> service(String name, Option<String> version, ImmutableList<String> roles) {
        List<ServiceDescriptor> services = services(name, version, roles);
        if (services.isEmpty()) {
            return Option.none();
        }
        return Option.apply(services.get(0));
    }
}
