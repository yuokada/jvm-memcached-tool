package io.github.yuokada.memcached.application.usecase;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.yuokada.memcached.application.port.MemcachedPort;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;

class FlushUseCaseTest {

    @Test
    void executeReturnsTrueOnSuccess() {
        FlushUseCase useCase = new FlushUseCase(new FakePort(true, null));

        assertTrue(useCase.execute());
    }

    @Test
    void executeReturnsFalseWhenPortReturnsFalse() {
        FlushUseCase useCase = new FlushUseCase(new FakePort(false, null));

        assertFalse(useCase.execute());
    }

    @Test
    void executeWrapsInterruptedExceptionAndSetsInterruptFlag() {
        FlushUseCase useCase = new FlushUseCase(new FakePort(false, new InterruptedException("test")));

        assertThrows(IllegalStateException.class, useCase::execute);
        assertTrue(Thread.interrupted(), "interrupt flag should be set after InterruptedException");
    }

    @Test
    void executeWrapsTimeoutException() {
        FlushUseCase useCase = new FlushUseCase(
            new FakePort(false, new TimeoutException("timed out"))
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class, useCase::execute);
        assertTrue(ex.getMessage().contains("flush"));
    }

    @Test
    void executeWrapsExecutionException() {
        FlushUseCase useCase = new FlushUseCase(
            new FakePort(false, new ExecutionException("failed", new RuntimeException()))
        );

        assertThrows(IllegalStateException.class, useCase::execute);
    }

    static class FakePort implements MemcachedPort {

        private final boolean result;
        private final Exception toThrow;

        FakePort(boolean result, Exception toThrow) {
            this.result = result;
            this.toThrow = toThrow;
        }

        @Override
        public Map<SocketAddress, Map<String, String>> stats() { return Map.of(); }

        @Override
        public Map<SocketAddress, Map<String, String>> stats(String subcommand) { return Map.of(); }

        @Override
        public void set(String key, int expirationSeconds, Object value) {}

        @Override
        public Object get(String key) { return null; }

        @Override
        public Map<String, Object> getBulk(Collection<String> keys) { return Map.of(); }

        @Override
        public boolean flush(int timeoutSeconds)
            throws ExecutionException, InterruptedException, TimeoutException {
            if (toThrow instanceof InterruptedException e) throw e;
            if (toThrow instanceof TimeoutException e) throw e;
            if (toThrow instanceof ExecutionException e) throw e;
            return result;
        }

        @Override
        public List<DumpMetadata> fetchMetadata(int limit) { return List.of(); }

        @Override
        public List<String> fetchKeys(int limit) { return List.of(); }
    }
}
