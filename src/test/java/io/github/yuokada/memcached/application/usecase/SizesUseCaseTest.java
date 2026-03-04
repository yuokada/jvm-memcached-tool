package io.github.yuokada.memcached.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.yuokada.memcached.application.port.MemcachedPort;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;

class SizesUseCaseTest {

    private static final SocketAddress SERVER = new InetSocketAddress("localhost", 11211);

    @Test
    void executeReturnsOneResultPerServer() {
        FakePort port = new FakePort(Map.of(SERVER, Map.of("96", "10", "128", "5")));
        SizesUseCase useCase = new SizesUseCase(port);

        List<SizesUseCase.SizesResult> results = useCase.execute();

        assertEquals(1, results.size());
        assertEquals(2, results.get(0).entries().size());
    }

    @Test
    void executeEntriesAreSortedBySize() {
        FakePort port = new FakePort(Map.of(SERVER, Map.of("512", "3", "64", "7", "256", "1")));
        SizesUseCase useCase = new SizesUseCase(port);

        List<SizesUseCase.SizeCount> entries = useCase.execute().get(0).entries();

        assertEquals(64, entries.get(0).sizeBytes());
        assertEquals(256, entries.get(1).sizeBytes());
        assertEquals(512, entries.get(2).sizeBytes());
    }

    @Test
    void executeCountValueIsPreservedAsString() {
        FakePort port = new FakePort(Map.of(SERVER, Map.of("96", "42")));
        SizesUseCase useCase = new SizesUseCase(port);

        SizesUseCase.SizeCount entry = useCase.execute().get(0).entries().get(0);

        assertEquals("42", entry.count());
    }

    @Test
    void executeWithNonNumericSizeLabelDefaultsToZero() {
        FakePort port = new FakePort(Map.of(SERVER, Map.of("bad_label", "1")));
        SizesUseCase useCase = new SizesUseCase(port);

        SizesUseCase.SizeCount entry = useCase.execute().get(0).entries().get(0);

        assertEquals(0L, entry.sizeBytes());
    }

    @Test
    void executeWithEmptyStatsReturnsEmptyList() {
        FakePort port = new FakePort(Map.of());
        SizesUseCase useCase = new SizesUseCase(port);

        assertTrue(useCase.execute().isEmpty());
    }

    static class FakePort implements MemcachedPort {

        private final Map<SocketAddress, Map<String, String>> sizesStats;

        FakePort(Map<SocketAddress, Map<String, String>> sizesStats) {
            this.sizesStats = sizesStats;
        }

        @Override
        public Map<SocketAddress, Map<String, String>> stats() { return Map.of(); }

        @Override
        public Map<SocketAddress, Map<String, String>> stats(String subcommand) {
            return sizesStats;
        }

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
        public List<String> fetchKeys(int limit) { return List.of(); }
    }
}
