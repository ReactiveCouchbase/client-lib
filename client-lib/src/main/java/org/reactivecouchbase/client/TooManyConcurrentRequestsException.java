package org.reactivecouchbase.client;

public class TooManyConcurrentRequestsException extends Throwable {
    public TooManyConcurrentRequestsException(String s) {
        super(s);
    }
}
