package io.github.yuokada.memcached.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.yuokada.memcached.application.port.MemcachedPort;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;

class DumpUseCaseTest {

    @Test
    void executeMapsDumpMetadataToResultWithValue() {
        Map<String, Object> store = new HashMap<>();
        store.put("key1", "hello");
        store.put("key2", "world");

        FakePort port = new FakePort(
            List.of(new MemcachedPort.DumpMetadata("key1", 300),
                new MemcachedPort.DumpMetadata("key2", 600)),
            store
        );
        DumpUseCase useCase = new DumpUseCase(port);

        List<DumpUseCase.DumpResult> results = useCase.execute(0);

        assertEquals(2, results.size());
        DumpUseCase.DumpResult first = results.stream().filter(r -> r.key().equals("key1")).findFirst().orElseThrow();
        assertEquals("key1", first.key());
        assertEquals(300, first.expiration());
        assertEquals("hello", first.value());
    }

    @Test
    void executeUsesEmptyStringWhenValueIsNull() {
        FakePort port = new FakePort(
            List.of(new MemcachedPort.DumpMetadata("missing_key", 0)),
            Map.of()
        );
        DumpUseCase useCase = new DumpUseCase(port);

        List<DumpUseCase.DumpResult> results = useCase.execute(0);

        assertEquals(1, results.size());
        assertEquals("", results.get(0).value());
    }

    @Test
    void executeWithNonObjectValueCallsToString() {
        Map<String, Object> store = new HashMap<>();
        store.put("intKey", 12345);

        FakePort port = new FakePort(
            List.of(new MemcachedPort.DumpMetadata("intKey", 60)),
            store
        );
        DumpUseCase useCase = new DumpUseCase(port);

        List<DumpUseCase.DumpResult> results = useCase.execute(0);

        assertEquals("12345", results.get(0).value());
    }

    @Test
    void executeWithEmptyMetadataReturnsEmptyList() {
        FakePort port = new FakePort(List.of(), Map.of());
        DumpUseCase useCase = new DumpUseCase(port);

        assertEquals(List.of(), useCase.execute(10));
    }

    static class FakePort implements MemcachedPort {

        private final List<DumpMetadata> metadata;
        private final Map<String, Object> store;

        FakePort(List<DumpMetadata> metadata, Map<String, Object> store) {
            this.metadata = metadata;
            this.store = store;
        }

        @Override
        public Map<SocketAddress, Map<String, String>> stats() { return Map.of(); }

        @Override
        public Map<SocketAddress, Map<String, String>> stats(String subcommand) { return Map.of(); }

        @Override
        public void set(String key, int expirationSeconds, Object value) {}

        @Override
        public Object get(String key) { return store.get(key); }

        @Override
        public boolean flush(int timeoutSeconds)
            throws ExecutionException, InterruptedException, TimeoutException {
            return true;
        }

        @Override
        public List<DumpMetadata> fetchMetadata(int limit) { return metadata; }

        @Override
        public List<String> fetchKeys(int limit) { return List.of(); }
    }
}
