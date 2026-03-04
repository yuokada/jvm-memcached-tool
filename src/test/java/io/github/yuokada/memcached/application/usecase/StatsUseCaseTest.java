package io.github.yuokada.memcached.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.yuokada.memcached.application.port.MemcachedPort;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;

class StatsUseCaseTest {

    private static final SocketAddress SERVER = new InetSocketAddress("localhost", 11211);
    private static final Map<SocketAddress, Map<String, String>> STATS_RESULT =
        Map.of(SERVER, Map.of("pid", "42"));
    private static final Map<SocketAddress, Map<String, String>> ITEMS_RESULT =
        Map.of(SERVER, Map.of("items:1:number", "5"));

    private final RecordingMemcachedPort port = new RecordingMemcachedPort();
    private final StatsUseCase useCase = new StatsUseCase(port);

    @Test
    void executeWithEmptyOperationCallsStatsWithNoSubcommand() {
        port.statsResult = STATS_RESULT;

        Map<SocketAddress, Map<String, String>> result = useCase.execute("");

        assertSame(STATS_RESULT, result);
        assertEquals(null, port.lastSubcommand);
    }

    @Test
    void executeWithNullOperationCallsStatsWithNoSubcommand() {
        port.statsResult = STATS_RESULT;

        Map<SocketAddress, Map<String, String>> result = useCase.execute(null);

        assertSame(STATS_RESULT, result);
        assertEquals(null, port.lastSubcommand);
    }

    @Test
    void executeWithItemsSubcommandForwardsToPort() {
        port.subcommandResult = ITEMS_RESULT;

        Map<SocketAddress, Map<String, String>> result = useCase.execute("items");

        assertSame(ITEMS_RESULT, result);
        assertEquals("items", port.lastSubcommand);
    }

    @Test
    void executeWithSettingsSubcommandForwardsToPort() {
        port.subcommandResult = STATS_RESULT;

        useCase.execute("settings");

        assertEquals("settings", port.lastSubcommand);
    }

    @Test
    void executeWithSizesSubcommandForwardsToPort() {
        port.subcommandResult = STATS_RESULT;

        useCase.execute("sizes");

        assertEquals("sizes", port.lastSubcommand);
    }

    @Test
    void executeWithUnknownOperationThrowsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> useCase.execute("unknown_cmd")
        );

        assertTrue(ex.getMessage().contains("unknown_cmd"));
        assertTrue(ex.getMessage().contains("Available commands"));
    }

    /** Minimal stub that records calls. */
    static class RecordingMemcachedPort implements MemcachedPort {

        Map<SocketAddress, Map<String, String>> statsResult = Map.of();
        Map<SocketAddress, Map<String, String>> subcommandResult = Map.of();
        String lastSubcommand = null;

        @Override
        public Map<SocketAddress, Map<String, String>> stats() {
            return statsResult;
        }

        @Override
        public Map<SocketAddress, Map<String, String>> stats(String subcommand) {
            lastSubcommand = subcommand;
            return subcommandResult;
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
