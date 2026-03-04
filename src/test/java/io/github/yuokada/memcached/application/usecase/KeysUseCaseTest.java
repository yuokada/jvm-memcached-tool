package io.github.yuokada.memcached.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.github.yuokada.memcached.application.port.MemcachedPort;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;

class KeysUseCaseTest {

    @Test
    void executeDelegatesToPortWithLimit() {
        List<String> expected = List.of("key=foo exp=300 la=1", "key=bar exp=-1 la=2");
        FakePort port = new FakePort(expected);
        KeysUseCase useCase = new KeysUseCase(port);

        List<String> result = useCase.execute(10);

        assertSame(expected, result);
        assertEquals(10, port.capturedLimit);
    }

    @Test
    void executeWithZeroLimitForwardsZero() {
        FakePort port = new FakePort(List.of());
        new KeysUseCase(port).execute(0);

        assertEquals(0, port.capturedLimit);
    }

    static class FakePort implements MemcachedPort {

        private final List<String> keys;
        int capturedLimit = -1;

        FakePort(List<String> keys) {
            this.keys = keys;
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
        public boolean flush(int timeoutSeconds)
            throws ExecutionException, InterruptedException, TimeoutException {
            return true;
        }

        @Override
        public List<DumpMetadata> fetchMetadata(int limit) { return List.of(); }

        @Override
        public List<String> fetchKeys(int limit) {
            capturedLimit = limit;
            return keys;
        }
    }
}
