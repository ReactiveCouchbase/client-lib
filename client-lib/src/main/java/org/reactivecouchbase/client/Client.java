package org.reactivecouchbase.client;

import org.reactivecouchbase.concurrent.Future;

import java.util.function.Function;

public interface Client {
    <T> Future<T> call(Function<ServiceDescriptor, T> f);
    <T> Future<T> callM(Function<ServiceDescriptor, Future<T>> f);
}
