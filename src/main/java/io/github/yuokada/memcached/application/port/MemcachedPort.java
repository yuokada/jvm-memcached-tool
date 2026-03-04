package io.github.yuokada.memcached.application.port;

import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Port interface for all Memcached operations.
 *
 * <p>Implementations must be thread-safe only to the extent that Memcached clients
 * themselves are; callers should not share a single command execution concurrently.
 *
 * <p>Keys passed to {@link #set} and {@link #get} must be valid Memcached keys
 * (no whitespace, no control characters). Keys returned by {@link #fetchKeys} and
 * {@link #fetchMetadata} may be URL-encoded; callers are responsible for decoding.
 */
public interface MemcachedPort {

    /**
     * Returns general server statistics keyed by server address.
     */
    Map<SocketAddress, Map<String, String>> stats();

    /**
     * Returns server statistics for the given subcommand (e.g. {@code items},
     * {@code settings}, {@code sizes}) keyed by server address.
     */
    Map<SocketAddress, Map<String, String>> stats(String subcommand);

    /**
     * Stores {@code value} under {@code key} with the given expiration.
     *
     * @param expirationSeconds TTL in seconds; {@code 0} means no expiration
     */
    void set(String key, int expirationSeconds, Object value);

    /**
     * Returns the value stored under {@code key}, or {@code null} if absent or expired.
     */
    Object get(String key);

    /**
     * Flushes all items from the cache and waits up to {@code timeoutSeconds} for confirmation.
     *
     * @return {@code true} if the flush succeeded within the timeout
     */
    boolean flush(int timeoutSeconds) throws ExecutionException, InterruptedException, TimeoutException;

    /**
     * Fetches key metadata from the LRU crawler.
     *
     * @param limit maximum number of entries to return; {@code 0} means no limit
     */
    List<DumpMetadata> fetchMetadata(int limit);

    /**
     * Fetches raw key lines from the LRU crawler metadump output.
     *
     * @param limit maximum number of lines to return; {@code 0} means no limit
     */
    List<String> fetchKeys(int limit);

    /**
     * Key metadata returned by the LRU crawler metadump command.
     *
     * @param key        the decoded key name
     * @param expiration Unix timestamp of expiry; {@code -1} means no expiration
     */
    record DumpMetadata(String key, int expiration) {}
}
