package io.github.yuokada.memcached.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.yuokada.memcached.application.port.MemcachedPort;
import java.net.SocketAddress;
import java.util.Collection;
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
    void executeWithNonStringValueCallsToString() {
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

        assertTrue(useCase.execute(10).isEmpty());
    }

    @Test
    void executeUsesBulkGetInsteadOfIndividualGets() {
        Map<String, Object> store = new HashMap<>();
        store.put("k1", "v1");
        store.put("k2", "v2");
        store.put("k3", "v3");

        FakePort port = new FakePort(
            List.of(
                new MemcachedPort.DumpMetadata("k1", 100),
                new MemcachedPort.DumpMetadata("k2", 200),
                new MemcachedPort.DumpMetadata("k3", 300)
            ),
            store
        );
        DumpUseCase useCase = new DumpUseCase(port);

        List<DumpUseCase.DumpResult> results = useCase.execute(0);

        assertEquals(3, results.size());
        assertEquals(1, port.getBulkCallCount());
        assertEquals(0, port.getCallCount());
        assertEquals("v1", results.stream().filter(r -> r.key().equals("k1")).findFirst().orElseThrow().value());
        assertEquals("v2", results.stream().filter(r -> r.key().equals("k2")).findFirst().orElseThrow().value());
        assertEquals("v3", results.stream().filter(r -> r.key().equals("k3")).findFirst().orElseThrow().value());
    }

    static class FakePort implements MemcachedPort {

        private final List<DumpMetadata> metadata;
        private final Map<String, Object> store;
        private int getCallCount = 0;
        private int getBulkCallCount = 0;

        FakePort(List<DumpMetadata> metadata, Map<String, Object> store) {
            this.metadata = metadata;
            this.store = store;
        }

        int getCallCount() { return getCallCount; }

        int getBulkCallCount() { return getBulkCallCount; }

        @Override
        public Map<SocketAddress, Map<String, String>> stats() { return Map.of(); }

        @Override
        public Map<SocketAddress, Map<String, String>> stats(String subcommand) { return Map.of(); }

        @Override
        public void set(String key, int expirationSeconds, Object value) {}

        @Override
        public Object get(String key) {
            getCallCount++;
            return store.get(key);
        }

        @Override
        public Map<String, Object> getBulk(Collection<String> keys) {
            getBulkCallCount++;
            Map<String, Object> result = new HashMap<>();
            for (String key : keys) {
                Object value = store.get(key);
                if (value != null) {
                    result.put(key, value);
                }
            }
            return result;
        }

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
