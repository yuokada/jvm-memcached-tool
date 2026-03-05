package io.github.yuokada.memcached.bootstrap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MemcachedClientProviderTest {

    @Test
    void isConfigEndpointReturnsTrueForElastiCacheConfigEndpoint() {
        assertTrue(MemcachedClientProvider.isConfigEndpoint("mycluster.cfg.use1.cache.amazonaws.com"));
    }

    @Test
    void isConfigEndpointReturnsFalseForRegularHost() {
        assertFalse(MemcachedClientProvider.isConfigEndpoint("localhost"));
    }

    @Test
    void isConfigEndpointReturnsFalseForIpAddress() {
        assertFalse(MemcachedClientProvider.isConfigEndpoint("127.0.0.1"));
    }

    @Test
    void isConfigEndpointReturnsFalseForHostWithoutCfgSegment() {
        assertFalse(MemcachedClientProvider.isConfigEndpoint("memcached.example.com"));
    }
}
