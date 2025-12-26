package io.github.yuokada.memcached.adapter.out.memcached;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.yuokada.memcached.bootstrap.MemcachedClientProvider;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.Map;
import java.util.UUID;
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
}
