package org.reactivecouchbase.client;

import org.reactivecouchbase.common.Duration;
import org.reactivecouchbase.concurrent.Await;
import org.reactivecouchbase.concurrent.Future;
import org.reactivecouchbase.concurrent.NamedExecutors;
import org.reactivecouchbase.concurrent.Promise;
import org.reactivecouchbase.functional.Option;
import org.reactivecouchbase.functional.Try;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class CommandContext {

    private static final String DEFAULT_BREAKER = "__DEFAULT_BREAKER__";
    private final ConcurrentHashMap<String, CircuitBreaker> breakers = new ConcurrentHashMap<String, CircuitBreaker>();
    private final Option<CommandCache> cache;
    private final Option<CommandCollapser> collapser;
    private final ScheduledExecutorService scheduledExecutorService;
    private final int allowedThreads;
    private final CircuitBreaker.Strategy strategy;

    private final AtomicInteger counter = new AtomicInteger(0);

    CommandContext(ScheduledExecutorService scheduledExecutorService, int allowedThreads, CircuitBreaker.Strategy strategy, Option<CommandCache> cache, Option<CommandCollapser> collapser) {
        this.scheduledExecutorService = scheduledExecutorService;
        this.allowedThreads = allowedThreads;
        this.cache = cache;
        this.collapser = collapser;
        this.strategy = strategy;
    }

    public static CommandContext of(int n) {
        return new CommandContext(NamedExecutors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1, "COMMAND-CONTEXT"), n, CircuitBreaker.Strategy.UNIQUE_PER_COMMAND, Option.<CommandCache>none(), Option.<CommandCollapser>none());
    }

    public CommandContext withAllowedThreads(int n) {
        return new CommandContext(this.scheduledExecutorService, n, this.strategy, this.cache, this.collapser);
    }

    public CommandContext withExecutor(ScheduledExecutorService ec) {
        return new CommandContext(ec, this.allowedThreads, this.strategy, this.cache, this.collapser);
    }

    public CommandContext withCache(CommandCache c) {
        return new CommandContext(this.scheduledExecutorService, this.allowedThreads, this.strategy, Option.apply(c), this.collapser);
    }

    public CommandContext withCollapser(CommandCollapser c) {
        return new CommandContext(this.scheduledExecutorService, this.allowedThreads, this.strategy, this.cache, Option.apply(c));
    }

    public CommandContext withCircuitBreakerStrategy(CircuitBreaker.Strategy c) {
        return new CommandContext(this.scheduledExecutorService, this.allowedThreads, c, this.cache, this.collapser);
    }

    CircuitBreaker breaker(String key) {
        if (strategy == CircuitBreaker.Strategy.UNIQUE_PER_CONTEXT) {
            key = DEFAULT_BREAKER;
        }
        if (!breakers.containsKey(key)) {
            breakers.putIfAbsent(key, new CircuitBreaker());
        }
        return breakers.get(key);
    }

    public <T> Future<T> execute(final Command<T> command) {
        final long start = System.currentTimeMillis();
        final Promise<T> promise = new Promise<T>();
        final Future<T> finalFuture = promise.future().andThen(ttry -> counter.decrementAndGet(), scheduledExecutorService);
        String cacheKey = command.cacheKey();
        if (cacheKey != null && cache.isDefined()) {
            Object o = cache.get().get(cacheKey);
            if (o != null) {
                return (Future<T>) o;
            }
        }
        final CircuitBreaker breaker = breaker(command.name());
        if (!breaker.allowRequest()) {
            try {
                T fValue = command.fallback();
                if (fValue == null) {
                    return Future.failed(new CircuitOpenException("The circuit is open"));
                } else {
                    return Future.successful(fValue);
                }
            } catch (Throwable t) {
                return Future.failed(t);
            }
        }
        if (allowedThreads <= counter.get()) {
            try {
                T fValue = command.fallback();
                if (fValue == null) {
                    return Future.failed(new TooManyConcurrentRequestsException("Max allowed request is " + allowedThreads));
                } else {
                    return Future.successful(fValue);
                }
            } catch (Throwable t) {
                return Future.failed(t);
            }
        }
        if (cacheKey != null && cache.isDefined()) {
            cache.get().put(cacheKey, finalFuture);
        }
        if (collapser.isDefined()) {
            Future<T> collapsed = collapser.get().add(command, promise, finalFuture, this, start);
            if (collapsed == null) {
                executeRequest(command, promise, start);
            } else {
                return collapsed;
            }
        } else {
            executeRequest(command, promise, start);
        }
        return finalFuture;
    }

    <T> void executeRequest(final Command<T> command, final Promise<T> promise, final long start) {
        final CircuitBreaker breaker = breaker(command.name());
        counter.incrementAndGet();
        int retry = command.retry();
        if (retry == 0) retry = 1;
        Future<T> fu = Future.retry(retry, command.exponentialBackoff(), () -> command.runAsync(scheduledExecutorService), scheduledExecutorService);
        fu.andThen(tTry -> {
            Duration duration = new Duration((System.currentTimeMillis() - start), TimeUnit.MILLISECONDS);
            for(Throwable t : tTry.asFailure()) {
                breaker.markFailure(duration);
                try {
                    T fValue = command.fallback();
                    if (fValue == null) {
                        promise.tryFailure(t);
                    } else {
                        promise.trySuccess(fValue);
                    }
                } catch (Throwable tt) {
                    promise.tryFailure(tt);
                }
            }
            for (T value : tTry.asSuccess()) {
                breaker.markSuccess(duration);
                promise.trySuccess(value);
            }
        });
        Future.timeout(null, command.timeout(), scheduledExecutorService).andThen(ttry -> {
            //breaker.markFailure(command.timeout());
            try {
                T fValue = command.fallback();
                if (fValue == null) {
                    promise.tryFailure(new TimeoutException("Request timeout (" + command.timeout().toHumanReadable() + ")"));
                } else {
                    promise.trySuccess(fValue);
                }
            } catch (Throwable t) {
                promise.tryFailure(t);
            }
        });
    }

    public <T> T get(Command<T> command) {
        return Await.result(execute(command), Command.FOREVER);
    }

    public <T> Try<T> getResult(Command<T> command) {
        try {
            return Try.success(Await.result(execute(command), Command.FOREVER));
        } catch (Exception e) {
            return Try.failure(e);
        }
    }

    public void shutdown() {
        scheduledExecutorService.shutdown();
        if (collapser.isDefined()) {
            collapser.get().stop();
        }
        if (cache.isDefined()) {
            cache.get().cleanUp();
        }
    }
}