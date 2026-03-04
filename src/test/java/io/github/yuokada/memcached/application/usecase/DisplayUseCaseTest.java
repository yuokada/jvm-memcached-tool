package io.github.yuokada.memcached.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.yuokada.memcached.application.port.MemcachedPort;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;

class DisplayUseCaseTest {

    private static final SocketAddress SERVER = new InetSocketAddress("localhost", 11211);

    @Test
    void executeReturnsSummaryPerServer() {
        FakePort port = new FakePort(
            Map.of(SERVER, Map.of("items:1:number", "10", "items:1:age", "300",
                "items:1:evicted", "0", "items:1:evicted_time", "0", "items:1:outofmemory", "0")),
            Map.of(SERVER, Map.of("1:chunk_size", "96", "1:total_pages", "1",
                "1:total_chunks", "100", "1:used_chunks", "10"))
        );
        DisplayUseCase useCase = new DisplayUseCase(port);

        List<DisplayUseCase.DisplayResult> results = useCase.execute();

        assertEquals(1, results.size());
        List<DisplayUseCase.SlabSummary> slabs = results.get(0).slabs();
        assertEquals(1, slabs.size());
        DisplayUseCase.SlabSummary slab = slabs.get(0);
        assertEquals(1, slab.slabId());
        assertEquals(10, slab.count());
        assertEquals(300, slab.maxAgeSeconds());
        assertEquals("96B", slab.itemSize());
        assertFalse(slab.full());
    }

    @Test
    void executeFormatsChunkSizeAbove1024AsKilo() {
        FakePort port = new FakePort(
            Map.of(SERVER, Map.of()),
            Map.of(SERVER, Map.of("2:chunk_size", "2048", "2:total_pages", "1",
                "2:total_chunks", "5", "2:used_chunks", "3"))
        );
        DisplayUseCase useCase = new DisplayUseCase(port);

        List<DisplayUseCase.SlabSummary> slabs = useCase.execute().get(0).slabs();

        assertEquals(1, slabs.size());
        assertEquals("2.0K", slabs.get(0).itemSize());
    }

    @Test
    void executeMarksSlabAsFullWhenAllChunksUsed() {
        FakePort port = new FakePort(
            Map.of(SERVER, Map.of()),
            Map.of(SERVER, Map.of("3:chunk_size", "128", "3:total_pages", "2",
                "3:total_chunks", "50", "3:used_chunks", "50"))
        );
        DisplayUseCase useCase = new DisplayUseCase(port);

        assertTrue(useCase.execute().get(0).slabs().get(0).full());
    }

    @Test
    void executeExcludesSlabsWithZeroTotalPages() {
        // A slab with total_pages=0 has no actual allocation and should be filtered out
        FakePort port = new FakePort(
            Map.of(SERVER, Map.of()),
            Map.of(SERVER, Map.of("4:chunk_size", "64", "4:total_pages", "0",
                "4:total_chunks", "0", "4:used_chunks", "0"))
        );
        DisplayUseCase useCase = new DisplayUseCase(port);

        assertTrue(useCase.execute().get(0).slabs().isEmpty());
    }

    @Test
    void executeWithNoServersReturnsEmptyList() {
        FakePort port = new FakePort(Map.of(), Map.of());
        DisplayUseCase useCase = new DisplayUseCase(port);

        assertTrue(useCase.execute().isEmpty());
    }

    static class FakePort implements MemcachedPort {

        private final Map<SocketAddress, Map<String, String>> itemsStats;
        private final Map<SocketAddress, Map<String, String>> slabsStats;

        FakePort(
            Map<SocketAddress, Map<String, String>> itemsStats,
            Map<SocketAddress, Map<String, String>> slabsStats
        ) {
            this.itemsStats = itemsStats;
            this.slabsStats = slabsStats;
        }

        @Override
        public Map<SocketAddress, Map<String, String>> stats() { return Map.of(); }

        @Override
        public Map<SocketAddress, Map<String, String>> stats(String subcommand) {
            return switch (subcommand) {
                case "items" -> itemsStats;
                case "slabs" -> slabsStats;
                default -> Map.of();
            };
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
