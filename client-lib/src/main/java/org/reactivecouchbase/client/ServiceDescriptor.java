package org.reactivecouchbase.client;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.reactivecouchbase.common.Invariant;
import org.reactivecouchbase.functional.Option;

public class ServiceDescriptor {

    public final String uid;
    public final String name;
    public final String url;
    public final ImmutableMap<String, String> metadata;
    public final ImmutableList<String> roles;
    public final Option<String> version;

    public ServiceDescriptor(String uid, String name, String url, ImmutableMap<String, String> metadata, ImmutableList<String> roles, Option<String> version) {
        Invariant.checkNotNull(uid);
        Invariant.checkNotNull(name);
        Invariant.checkNotNull(url);
        this.uid = uid;
        this.name = name;
        this.url = url;
        this.metadata = Objects.firstNonNull(metadata, ImmutableMap.<String, String>of());
        this.roles = Objects.firstNonNull(roles, ImmutableList.<String>of());
        this.version = Objects.firstNonNull(version, Option.<String>none());
    }

    public ServiceDescriptor(String uid, String name, String url) {
        Invariant.checkNotNull(uid);
        Invariant.checkNotNull(name);
        Invariant.checkNotNull(url);
        this.uid = uid;
        this.name = name;
        this.url = url;
        this.metadata = ImmutableMap.of();
        this.roles = ImmutableList.of();
        this.version = Option.none();
    }

    public ServiceDescriptor(String uid, String name, String url, ImmutableList<String> roles) {
        Invariant.checkNotNull(uid);
        Invariant.checkNotNull(name);
        Invariant.checkNotNull(url);
        this.uid = uid;
        this.name = name;
        this.url = url;
        this.metadata = ImmutableMap.of();
        this.roles = MoreObjects.firstNonNull(roles, ImmutableList.<String>of());
        this.version = Option.none();
    }

    public ServiceDescriptor(String uid, String name, String url, ImmutableMap<String, String> metadata) {
        Invariant.checkNotNull(uid);
        Invariant.checkNotNull(name);
        Invariant.checkNotNull(url);
        this.uid = uid;
        this.name = name;
        this.url = url;
        this.metadata = MoreObjects.firstNonNull(metadata, ImmutableMap.<String, String>of());
        this.roles = ImmutableList.of();
        this.version = Option.none();
    }

    public ServiceDescriptor(String uid, String name, String url, ImmutableList<String> roles, Option<String> version) {
        Invariant.checkNotNull(uid);
        Invariant.checkNotNull(name);
        Invariant.checkNotNull(url);
        this.uid = uid;
        this.name = name;
        this.url = url;
        this.metadata = ImmutableMap.of();
        this.roles = MoreObjects.firstNonNull(roles, ImmutableList.<String>of());
        this.version = MoreObjects.firstNonNull(version, Option.<String>none());
    }

    public ServiceDescriptor(String uid, String name, String url, Option<String> version) {
        Invariant.checkNotNull(uid);
        Invariant.checkNotNull(name);
        Invariant.checkNotNull(url);
        this.uid = uid;
        this.name = name;
        this.url = url;
        this.metadata = ImmutableMap.of();
        this.roles = ImmutableList.<String>of();
        this.version = MoreObjects.firstNonNull(version, Option.<String>none());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServiceDescriptor)) return false;

        ServiceDescriptor that = (ServiceDescriptor) o;

        if (!uid.equals(that.uid)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return uid.hashCode();
    }

    @Override
    public String toString() {
        return "ServiceDescriptor{" +
                "uid='" + uid + '\'' +
                ", name='" + name + '\'' +
                ", url='" + url + '\'' +
                ", metadata=" + metadata +
                ", roles=" + roles +
                ", version=" + version +
                '}';
    }
}
