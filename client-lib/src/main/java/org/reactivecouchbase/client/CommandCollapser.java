package org.reactivecouchbase.client;

import org.reactivecouchbase.common.Duration;
import org.reactivecouchbase.concurrent.Future;
import org.reactivecouchbase.concurrent.Promise;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CommandCollapser {

    private static class ExecutionContext<T> {
        private final Command<T> command;
        private final Promise<T> promise;
        private final Future<T> future;
        private final CommandContext ctx;
        private final long start;

        private ExecutionContext(Command<T> command, Promise<T> promise, Future<T> future, CommandContext ctx, long start) {
            this.command = command;
            this.promise = promise;
            this.future = future;
            this.ctx = ctx;
            this.start = start;
        }

        public String collapseKey() {
            return command.collapseKey();
        }

        public void execute() {
            ctx.executeRequest(command, promise, start);
        }
    }

    private final Object lock = new Object();
    private final Duration every;
    private final ScheduledExecutorService ec = Executors.newSingleThreadScheduledExecutor();

    private ConcurrentHashMap<String, ExecutionContext<?>> queue = new ConcurrentHashMap<String, ExecutionContext<?>>();

    private CommandCollapser(Duration every) {
        this.every = every;
    }

    <T> Future<T> add(Command<T> command, Promise<T> promise, Future<T> future, CommandContext ctx, long start) {
        synchronized (lock) {
            String key = command.collapseKey();
            if (key == null) return null;
            if (!queue.containsKey(key)) {
                ExecutionContext<T> e = (ExecutionContext<T>) queue.putIfAbsent(key, new ExecutionContext<T>(command, promise, future, ctx, start));
                if (e != null) {
                    return e.future;
                }
                return future;
            } else {
                return ((ExecutionContext<T>) queue.get(key)).future;
            }
        }
    }

    private void executeWaitingRequests() {
        synchronized (lock) {
            if (queue.isEmpty()) return;
            List<String> remove = new ArrayList<String>();
            for (Map.Entry<String, ExecutionContext<?>> entry : queue.entrySet()) {
                remove.add(entry.getKey());
                entry.getValue().execute();
            }
            for (String s : remove) {
                queue.remove(s);
            }
        }
    }

    private void schedule(long v, TimeUnit u) {
        if (!ec.isShutdown()) {
            ec.schedule(new Runnable() {
                @Override
                public void run() {
                    try {
                        executeWaitingRequests();
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                    try {
                        if (!ec.isShutdown()) {
                            schedule(every.toMillis(), TimeUnit.MILLISECONDS);
                        }
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            }, v, u);
        }
    }

    private void start() {
        schedule(0, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        ec.shutdown();
    }

    public static CommandCollapser of(Duration d) {
        CommandCollapser collapser = new CommandCollapser(d);
        collapser.start();
        return collapser;
    }
}
