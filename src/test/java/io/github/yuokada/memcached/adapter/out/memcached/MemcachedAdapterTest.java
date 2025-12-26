package io.github.yuokada.memcached.adapter.out.memcached;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.yuokada.memcached.bootstrap.MemcachedClientProvider;
import java.io.IOException;
import java.net.SocketAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;
import net.spy.memcached.MemcachedClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class MemcachedAdapterTest {

    private static final String MEMCACHED_IMAGE = "memcached:1.6.32-alpine";
    private static final Integer MEMCACHED_PORT = 11211;

    @Container
    static final GenericContainer<?> MEMCACHED = new GenericContainer<>(
        DockerImageName.parse(MEMCACHED_IMAGE)
    )
        .withExposedPorts(MEMCACHED_PORT);

    private static MemcachedClient memcachedClient;
    private static MemcachedAdapter memcachedAdapter;

    @BeforeAll
    static void setUpMemcachedClient() throws IOException {
        int mappedPort = MEMCACHED.getMappedPort(MEMCACHED_PORT);
        String host = MEMCACHED.getHost();
        memcachedClient = MemcachedClientProvider.getMemcachedClient(host, mappedPort);
        memcachedAdapter = new MemcachedAdapter(memcachedClient);
    }

    @AfterAll
    static void tearDownMemcachedClient() {
        if (memcachedClient != null) {
            memcachedClient.shutdown();
        }
    }

    @Test
    void setAndGetValueRoundTripsThroughMemcached() {
        String key = "memcached:test:" + UUID.randomUUID();
        String expectedValue = "value-" + UUID.randomUUID();

        memcachedAdapter.set(key, 60, expectedValue);

        assertEquals(expectedValue, memcachedAdapter.get(key));
    }

    @Test
    void getBulkRetrievesMultipleValuesInSingleCall() {
        String key1 = "memcached:bulk1:" + UUID.randomUUID();
        String key2 = "memcached:bulk2:" + UUID.randomUUID();
        String key3 = "memcached:bulk3:" + UUID.randomUUID();
        String value1 = "value1-" + UUID.randomUUID();
        String value2 = "value2-" + UUID.randomUUID();
        String value3 = "value3-" + UUID.randomUUID();

        memcachedAdapter.set(key1, 60, value1);
        memcachedAdapter.set(key2, 60, value2);
        memcachedAdapter.set(key3, 60, value3);

        Map<String, Object> results = memcachedAdapter.getBulk(List.of(key1, key2, key3));

        assertEquals(3, results.size(), "getBulk should return all three keys");
        assertEquals(value1, results.get(key1));
        assertEquals(value2, results.get(key2));
        assertEquals(value3, results.get(key3));
    }

    @Test
    void statsReturnsServerMetrics() {
        Map<SocketAddress, Map<String, String>> stats = memcachedAdapter.stats();

        assertFalse(stats.isEmpty(), "stats should contain at least one server entry");
        Map<String, String> firstServerStats = stats.values().iterator().next();
        assertTrue(
            firstServerStats.containsKey("pid"),
            "stats should include a representative metric like pid"
        );
    }

    @Test
    void statsSettingsSubcommandReturnsValues() {
        Map<SocketAddress, Map<String, String>> stats = memcachedAdapter.stats("settings");

        assertFalse(stats.isEmpty(), "stats -- settings should return server information");
        Map<String, String> firstServerStats = stats.values().iterator().next();
        assertTrue(
            firstServerStats.containsKey("maxbytes"),
            "settings stats should include configuration values"
        );
    }

    @Test
    void flushClearsStoredData() throws ExecutionException, InterruptedException, TimeoutException {
        String key = "memcached:flush:" + UUID.randomUUID();
        memcachedAdapter.set(key, 300, "value");
        assertEquals("value", memcachedAdapter.get(key));

        assertTrue(memcachedAdapter.flush(5));
        assertNull(memcachedAdapter.get(key));
    }

    @Test
    void fetchMetadataReturnsKeysWithExpiration() {
        String key = "memcached:metadata:" + UUID.randomUUID();
        memcachedAdapter.set(key, 120, "meta-value");
        assertTrue(
            awaitCondition(() -> memcachedAdapter.fetchMetadata(50)
                .stream()
                .anyMatch(entry -> entry.key().equals(key))),
            "metadata should include inserted key"
        );
    }

    @Test
    void fetchKeysReturnsRawKeyLines() {
        String key = "memcached:keys:" + UUID.randomUUID();
        memcachedAdapter.set(key, 120, "key-value");

        String encodedKey = URLEncoder.encode(key, StandardCharsets.UTF_8)
            .replace("+", "%20");
        assertTrue(
            awaitCondition(() -> memcachedAdapter.fetchKeys(50)
                .stream()
                .anyMatch(line -> line.contains("key=" + encodedKey))),
            "keys output should contain inserted key"
        );
    }

    private boolean awaitCondition(BooleanSupplier condition) {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return true;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting", e);
            }
        }
        return false;
    }
}
