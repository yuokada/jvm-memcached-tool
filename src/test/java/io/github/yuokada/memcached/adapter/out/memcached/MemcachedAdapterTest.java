package io.github.yuokada.memcached.adapter.out.memcached;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.yuokada.memcached.application.port.MemcachedPort;
import io.github.yuokada.memcached.bootstrap.MemcachedClientProvider;
import java.io.IOException;
import java.net.SocketAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
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

        List<MemcachedPort.DumpMetadata> metadata = memcachedAdapter.fetchMetadata(50);
        assertTrue(
            metadata.stream().anyMatch(entry -> entry.key().equals(key)),
            "metadata should include inserted key"
        );
    }

    @Test
    void fetchKeysReturnsRawKeyLines() {
        String key = "memcached:keys:" + UUID.randomUUID();
        memcachedAdapter.set(key, 120, "key-value");

        List<String> keys = memcachedAdapter.fetchKeys(50);
        String encodedKey = URLEncoder.encode(key, StandardCharsets.UTF_8)
            .replace("+", "%20");
        assertTrue(
            keys.stream().anyMatch(line -> line.contains("key=" + encodedKey)),
            "keys output should contain inserted key"
        );
    }
}
