package org.reactivecouchbase.client.test;

import org.junit.Assert;
import org.junit.Test;
import org.reactivecouchbase.client.Command;
import org.reactivecouchbase.client.CommandCollapser;
import org.reactivecouchbase.client.CommandContext;
import org.reactivecouchbase.client.InMemoryCommandCache;
import org.reactivecouchbase.common.Duration;
import org.reactivecouchbase.concurrent.Await;
import org.reactivecouchbase.concurrent.Future;
import org.reactivecouchbase.concurrent.Promise;
import org.reactivecouchbase.functional.Option;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class CommandTest {

    public static final Duration await = Duration.parse("2 min");

    @Test
    public void simpleTest() {
        CommandContext context = CommandContext.of(5);
        String result = Await.result(context.execute(new PassingCommand()), await);
        Assert.assertEquals("Hello", result);
        String result1 = context.get(new PassingCommand());
        Assert.assertEquals("Hello", result1);
    }

    @Test
    public void testCache() {
        CommandContext context = CommandContext.of(5).withCache(InMemoryCommandCache.of(Duration.parse("10 min")));
        String result1 = Await.result(context.execute(new PassingCacheCommand()), await);
        String result2 = Await.result(context.execute(new TimedCommand(Duration.parse("10 sec"))), Duration.parse("2 sec"));
        Assert.assertEquals("Hello Cache", result1);
        Assert.assertEquals("Hello Cache", result2);
    }

    @Test
    public void testCollapser() {
        CommandContext context = CommandContext.of(5).withCollapser(CommandCollapser.of(Duration.parse("10 millis")));
        AtomicInteger counter = new AtomicInteger(0);
        Future<String> result1 = context.execute(new PassingCounterCommand(counter));
        Future<String> result2 = context.execute(new PassingCounterCommand(counter));
        Assert.assertEquals("Hello", Await.result(result1, await));
        Assert.assertEquals("Hello", Await.result(result2, await));
        Assert.assertEquals(1, counter.get());
        context.shutdown();
    }

    @Test
    public void testCollapserWithLotOfCalls() throws Exception {
        CommandContext context = CommandContext.of(12).withCollapser(CommandCollapser.of(Duration.parse("10 millis")));
        AtomicInteger counter = new AtomicInteger(0);
        Future<String> result1 = context.execute(new PassingCounterCommand(counter));
        Future<String> result2 = context.execute(new PassingCounterCommand(counter));
        Future<String> result3 = context.execute(new PassingCounterCommand(counter));
        Future<String> result4 = context.execute(new PassingCounterCommand(counter));
        Future<String> result5 = context.execute(new PassingCounterCommand(counter));
        Future<String> result6 = context.execute(new PassingCounterCommand(counter));
        Future<String> result7 = context.execute(new PassingCounterCommand(counter));
        Thread.sleep(100);
        Future<String> result8 = context.execute(new PassingCounterCommand(counter));
        Future<String> result9 = context.execute(new PassingCounterCommand(counter));
        Future<String> result10 = context.execute(new PassingCounterCommand(counter));
        Assert.assertEquals("Hello", Await.result(result1, await));
        Assert.assertEquals("Hello", Await.result(result2, await));
        Assert.assertEquals("Hello", Await.result(result3, await));
        Assert.assertEquals("Hello", Await.result(result4, await));
        Assert.assertEquals("Hello", Await.result(result5, await));
        Assert.assertEquals("Hello", Await.result(result6, await));
        Assert.assertEquals("Hello", Await.result(result7, await));
        Assert.assertEquals("Hello", Await.result(result8, await));
        Assert.assertEquals("Hello", Await.result(result9, await));
        Assert.assertEquals("Hello", Await.result(result10, await));
        Assert.assertTrue(1 < counter.get());  // should be more than one because of the sleep
        Assert.assertTrue(10 > counter.get()); // should be less than 10 because of collapsing
        Assert.assertTrue(4 > counter.get());  // should be less than 4 because in bad cases, the collapse batch passes during the 3 last commands, but because of collapsing only count one more
        context.shutdown();
    }

    @Test
    public void testCollapserWithLotOfCallsAndCache() throws Exception {
        CommandContext context = CommandContext.of(12).withCollapser(CommandCollapser.of(Duration.parse("10 millis"))).withCache(InMemoryCommandCache.of(Duration.of("10 min")));
        AtomicInteger counter = new AtomicInteger(0);
        Future<String> result1 = context.execute(new PassingCounterCommand(counter));
        Future<String> result2 = context.execute(new PassingCounterCommand(counter));
        Future<String> result3 = context.execute(new PassingCounterCommand(counter));
        Future<String> result4 = context.execute(new PassingCounterCommand(counter));
        Future<String> result5 = context.execute(new PassingCounterCommand(counter));
        Future<String> result6 = context.execute(new PassingCounterCommand(counter));
        Future<String> result7 = context.execute(new PassingCounterCommand(counter));
        Future<String> result8 = context.execute(new PassingCounterCommand(counter));
        Future<String> result9 = context.execute(new PassingCounterCommand(counter));
        Future<String> result10 = context.execute(new PassingCounterCommand(counter));
        Thread.sleep(100);
        Future<String> result11 = context.execute(new PassingCounterCommand(counter));
        Future<String> result12 = context.execute(new PassingCounterCommand(counter));
        Future<String> result13 = context.execute(new PassingCounterCommand(counter));
        Future<String> result14 = context.execute(new PassingCounterCommand(counter));
        Assert.assertEquals("Hello", Await.result(result1, await));
        Assert.assertEquals("Hello", Await.result(result2, await));
        Assert.assertEquals("Hello", Await.result(result3, await));
        Assert.assertEquals("Hello", Await.result(result4, await));
        Assert.assertEquals("Hello", Await.result(result5, await));
        Assert.assertEquals("Hello", Await.result(result6, await));
        Assert.assertEquals("Hello", Await.result(result7, await));
        Assert.assertEquals("Hello", Await.result(result8, await));
        Assert.assertEquals("Hello", Await.result(result9, await));
        Assert.assertEquals("Hello", Await.result(result10, await));
        Assert.assertEquals("Hello", Await.result(result11, await));
        Assert.assertEquals("Hello", Await.result(result12, await));
        Assert.assertEquals("Hello", Await.result(result13, await));
        Assert.assertEquals("Hello", Await.result(result14, await));
        Assert.assertEquals(1, counter.get());
        context.shutdown();
    }

    @Test
    public void testTimeout() {
        CommandContext context = CommandContext.of(5);
        String result = Await.result(context.execute(new TimedCommand(Duration.parse("4 sec"), Duration.parse("3 sec"))), await);
        Assert.assertEquals("Goodbye", result);
    }

    @Test
    public void testTooMuchThread() {
        CommandContext context = CommandContext.of(5);
        Future<String> result1 = context.execute(new TimedCommand(Duration.parse("4 sec")));
        Future<String> result2 = context.execute(new TimedCommand(Duration.parse("4 sec")));
        Future<String> result3 = context.execute(new TimedCommand(Duration.parse("4 sec")));
        Future<String> result4 = context.execute(new TimedCommand(Duration.parse("4 sec")));
        Future<String> result5 = context.execute(new TimedCommand(Duration.parse("4 sec")));
        Future<String> result6 = context.execute(new TimedCommand(Duration.parse("4 sec")));
        Future<String> result7 = context.execute(new TimedCommand(Duration.parse("4 sec")));
        Assert.assertEquals("Goodbye", Await.result(result6, Duration.parse("1 sec"))); // test fast fail
        Assert.assertEquals("Goodbye", Await.result(result7, Duration.parse("1 sec"))); // test fast fail
        Assert.assertEquals("Hello", Await.result(result1, await));
        Assert.assertEquals("Hello", Await.result(result2, await));
        Assert.assertEquals("Hello", Await.result(result3, await));
        Assert.assertEquals("Hello", Await.result(result4, await));
        Assert.assertEquals("Hello", Await.result(result5, await));
    }

    @Test
    public void testTooMuchFailures() {
        CommandContext context = CommandContext.of(5);
        String result1 = context.get(new PassingFailingTimedCommand(true));
        String result2 = context.get(new PassingFailingTimedCommand(false));
        String result3 = context.get(new PassingFailingTimedCommand(false));
        String result4 = context.get(new PassingFailingTimedCommand(false));
        String result5 = context.get(new PassingFailingTimedCommand(false));
        String result6 = context.get(new PassingFailingTimedCommand(true));
        String result7 = context.get(new PassingFailingTimedCommand(true, Duration.parse("2 sec")));
        Assert.assertEquals("Hello", result1);
        Assert.assertEquals("Goodbye", result2);
        Assert.assertEquals("Goodbye", result3);
        Assert.assertEquals("Goodbye", result4);
        Assert.assertEquals("Goodbye", result5);
        Assert.assertEquals("Hello", result6);
        Assert.assertEquals("Hello", result7);
    }


    @Test
    public void testNoFallback() {
        CommandContext context = CommandContext.of(5);
        boolean exception = false;
        try {
            context.get(new FailingCommandWithNoFallback());
        } catch (Exception e) {
            if (e.getCause().getCause() instanceof WeirdException) {
                exception = true;
            }
        }
        Assert.assertTrue(exception);
    }

    @Test
    public void testTimeoutNoFallback() {
        CommandContext context = CommandContext.of(5);
        boolean exception = false;
        try {
            context.get(new FailingTimedCommandWithNoFallback(Duration.of("5 sec"), Duration.of("1 sec")));
        } catch (Exception e) {
            if (e.getCause().getCause() instanceof TimeoutException) {
                exception = true;
            }
        }
        Assert.assertTrue(exception);
    }

    @Test
    public void testFailingFallback() {
        CommandContext context = CommandContext.of(5);
        boolean exception = false;
        try {
            context.get(new FailingCommandWithFailingFallback());
        } catch (Exception e) {
            if (e.getCause().getCause() instanceof FallbackException) {
                exception = true;
            }
        }
        Assert.assertTrue(exception);
    }

    @Test
    public void testRetryCommand() {
        CommandContext context = CommandContext.of(5);
        AtomicInteger counter = new AtomicInteger(0);
        boolean exception = false;
        try {
            context.get(new FailingCommandWithRetry(counter));
        } catch (Exception e) {
            exception = true;
        }
        Assert.assertEquals(10, counter.get());
        Assert.assertTrue(exception);
    }

    @Test
    public void testRetryCommandExpo() {
        CommandContext context = CommandContext.of(5);
        AtomicInteger counter = new AtomicInteger(0);
        boolean exception = false;
        try {
            context.get(new FailingCommandWithRetryNoExpo(counter));
        } catch (Exception e) {
            exception = true;
        }
        Assert.assertEquals(100, counter.get());
        Assert.assertTrue(exception);
    }

    public static class WeirdException extends RuntimeException {
        public WeirdException() {
            super();
        }
    }

    public static class FallbackException extends RuntimeException {
        public FallbackException() {
            super();
        }
    }

    public static class FailingCommandWithNoFallback extends Command<String> {
        @Override
        public Future<String> runAsync(ScheduledExecutorService ec) {
            return Future.failed(new WeirdException());
        }
    }

    public static class FailingCommandWithRetry extends Command<String> {

        private final AtomicInteger counter;

        public FailingCommandWithRetry(AtomicInteger counter) {
            this.counter = counter;
        }

        @Override
        public Future<String> runAsync(ScheduledExecutorService ec) {
            counter.incrementAndGet();
            return Future.failed(new WeirdException());
        }

        @Override
        public int retry() {
            return 10;
        }
    }

    public static class FailingCommandWithRetryNoExpo extends Command<String> {

        private final AtomicInteger counter;

        public FailingCommandWithRetryNoExpo(AtomicInteger counter) {
            this.counter = counter;
        }

        @Override
        public Future<String> runAsync(ScheduledExecutorService ec) {
            counter.incrementAndGet();
            return Future.failed(new WeirdException());
        }

        @Override
        public int retry() {
            return 100;
        }

        @Override
        public boolean exponentialBackoff() {
            return false;
        }
    }

    public static class FailingCommandWithFailingFallback extends Command<String> {
        @Override
        public String run() {
            throw new WeirdException();
        }

        @Override
        public String fallback() {
            throw new FallbackException();
        }
    }

    public static class FailingTimedCommandWithNoFallback extends Command<String> {

        private final Duration duration;
        private final Duration timeout;

        public FailingTimedCommandWithNoFallback(Duration duration, Duration timeout) {
            this.duration = duration;
            this.timeout = timeout;
        }

        public FailingTimedCommandWithNoFallback(Duration duration) {
            this.duration = duration;
            this.timeout = Duration.parse("60 sec");
        }

        @Override
        public Future<String> runAsync(ScheduledExecutorService ec) {
            return Future.timeout("Hello", duration, ec);
        }

        @Override
        public Duration timeout() {
            return timeout;
        }
    }

    public static class PassingFailingTimedCommand extends Command<String> {

        private final Option<Duration> durationOption;
        private final boolean passing;
        private final ScheduledExecutorService sched = Executors.newSingleThreadScheduledExecutor();

        public PassingFailingTimedCommand(boolean passing) {
            this.passing = passing;
            this.durationOption = Option.none();
        }

        public PassingFailingTimedCommand(boolean passing, Duration duration) {
            this.durationOption = Option.apply(duration);
            this.passing = passing;
        }

        @Override
        public Future<String> runAsync(ScheduledExecutorService ec) {
            if (durationOption.isDefined()) {
                final Promise<String> promise = new Promise<String>();
                sched.schedule((Runnable) () -> {
                    if (passing) {
                        promise.trySuccess("Hello");
                    } else {
                        promise.tryFailure(new RuntimeException("I failed"));
                    }
                }, durationOption.get().toMillis(), TimeUnit.MILLISECONDS);
                return promise.future();
            } else {
                if (passing) {
                    return Future.successful("Hello");
                } else {
                    return Future.failed(new RuntimeException("I failed"));
                }
            }
        }

        public String fallback() {
            return "Goodbye";
        }

    }

    public static class PassingCommand extends Command<String> {
        @Override
        public Future<String> runAsync(ScheduledExecutorService ec) {
            return Future.successful("Hello");
        }

        @Override
        public String fallback() {
            return "Goodbye";
        }
    }

    public static class PassingCounterCommand extends Command<String> {

        private final AtomicInteger counter;

        public PassingCounterCommand(AtomicInteger counter) {
            this.counter = counter;
        }

        public Integer count() {
            return counter.get();
        }

        @Override
        public Future<String> runAsync(ScheduledExecutorService ec) {
            counter.incrementAndGet();
            return Future.successful("Hello");
        }

        @Override
        public String fallback() {
            return "Goodbye";
        }

        @Override
        public String cacheKey() {
            return "key";
        }
    }

    public static class PassingCacheCommand extends Command<String> {
        @Override
        public Future<String> runAsync(ScheduledExecutorService ec) {
            return Future.successful("Hello Cache");
        }

        @Override
        public String fallback() {
            return "Goodbye";
        }

        @Override
        public String cacheKey() {
            return "key";
        }
    }

    public static class TimedCommand extends Command<String> {

        private final Duration duration;
        private final Duration timeout;

        public TimedCommand(Duration duration, Duration timeout) {
            this.duration = duration;
            this.timeout = timeout;
        }

        public TimedCommand(Duration duration) {
            this.duration = duration;
            this.timeout = Duration.parse("60 sec");
        }

        @Override
        public Future<String> runAsync(ScheduledExecutorService ec) {
            return Future.timeout("Hello", duration, ec);
        }

        @Override
        public String fallback() {
            return "Goodbye";
        }

        @Override
        public Duration timeout() {
            return timeout;
        }

        @Override
        public String cacheKey() {
            return "key";
        }
    }

    public static class FailingCommand extends Command<String> {
        @Override
        public Future<String> runAsync(ScheduledExecutorService ec) {
            return Future.failed(new RuntimeException("I failed"));
        }

        @Override
        public String fallback() {
            return "Goodbye";
        }
    }
}
