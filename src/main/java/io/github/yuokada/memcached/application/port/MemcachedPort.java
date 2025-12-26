package io.github.yuokada.memcached.application.port;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public interface MemcachedPort {

    Map<SocketAddress, Map<String, String>> stats();

    Map<SocketAddress, Map<String, String>> stats(String subcommand);

    void set(String key, int expirationSeconds, Object value);

    Object get(String key);

    boolean flush(int timeoutSeconds) throws ExecutionException, InterruptedException, TimeoutException;

    Collection<InetSocketAddress> getAvailableServers();
}
