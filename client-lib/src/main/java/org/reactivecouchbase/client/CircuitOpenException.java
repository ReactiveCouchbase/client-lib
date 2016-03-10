package org.reactivecouchbase.client;

public class CircuitOpenException extends Throwable {
    public CircuitOpenException(String s) {
        super(s);
    }
}
