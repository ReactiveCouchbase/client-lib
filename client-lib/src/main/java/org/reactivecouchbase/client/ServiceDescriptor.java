package org.reactivecouchbase.client;

import com.google.common.base.MoreObjects;
import org.reactivecouchbase.common.Invariant;
import org.reactivecouchbase.functional.Option;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceDescriptor implements Serializable {

    public final String uid;
    public final String name;
    public final String url;
    public final Map<String, String> metadata;
    public final List<String> roles;
    public final Option<String> version;

    public ServiceDescriptor(String uid, String name, String url, Map<String, String> metadata, List<String> roles, Option<String> version) {
        Invariant.checkNotNull(uid);
        Invariant.checkNotNull(name);
        Invariant.checkNotNull(url);
        this.uid = uid;
        this.name = name;
        this.url = url;
        this.metadata = MoreObjects.firstNonNull(metadata, new HashMap<>());
        this.roles = MoreObjects.firstNonNull(roles, new ArrayList<>());
        this.version = MoreObjects.firstNonNull(version, Option.<String>none());
    }

    public ServiceDescriptor(String uid, String name, String url) {
        Invariant.checkNotNull(uid);
        Invariant.checkNotNull(name);
        Invariant.checkNotNull(url);
        this.uid = uid;
        this.name = name;
        this.url = url;
        this.metadata = new HashMap<>();
        this.roles = new ArrayList<>();
        this.version = Option.none();
    }

    public ServiceDescriptor(String uid, String name, String url, List<String> roles) {
        Invariant.checkNotNull(uid);
        Invariant.checkNotNull(name);
        Invariant.checkNotNull(url);
        this.uid = uid;
        this.name = name;
        this.url = url;
        this.metadata = new HashMap<>();;
        this.roles = MoreObjects.firstNonNull(roles, new ArrayList<>());
        this.version = Option.none();
    }

    public ServiceDescriptor(String uid, String name, String url, Map<String, String> metadata) {
        Invariant.checkNotNull(uid);
        Invariant.checkNotNull(name);
        Invariant.checkNotNull(url);
        this.uid = uid;
        this.name = name;
        this.url = url;
        this.metadata = MoreObjects.firstNonNull(metadata, new HashMap<>());
        this.roles = new ArrayList<>();
        this.version = Option.none();
    }

    public ServiceDescriptor(String uid, String name, String url, List<String> roles, Option<String> version) {
        Invariant.checkNotNull(uid);
        Invariant.checkNotNull(name);
        Invariant.checkNotNull(url);
        this.uid = uid;
        this.name = name;
        this.url = url;
        this.metadata = new HashMap<>();
        this.roles = MoreObjects.firstNonNull(roles, new ArrayList<>());
        this.version = MoreObjects.firstNonNull(version, Option.<String>none());
    }

    public ServiceDescriptor(String uid, String name, String url, Option<String> version) {
        Invariant.checkNotNull(uid);
        Invariant.checkNotNull(name);
        Invariant.checkNotNull(url);
        this.uid = uid;
        this.name = name;
        this.url = url;
        this.metadata = new HashMap<>();
        this.roles = new ArrayList<>();
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
