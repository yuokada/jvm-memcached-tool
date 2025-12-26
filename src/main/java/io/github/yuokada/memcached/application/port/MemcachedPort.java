package io.github.yuokada.memcached.application.port;

import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public interface MemcachedPort {

    Map<SocketAddress, Map<String, String>> stats();

    Map<SocketAddress, Map<String, String>> stats(String subcommand);

    void set(String key, int expirationSeconds, Object value);

    Object get(String key);

    boolean flush(int timeoutSeconds) throws ExecutionException, InterruptedException, TimeoutException;

    List<DumpMetadata> fetchMetadata(int limit);

    List<String> fetchKeys(int limit);

    record DumpMetadata(String key, int expiration) {

    }
}
